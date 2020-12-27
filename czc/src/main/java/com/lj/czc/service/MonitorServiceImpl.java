package com.lj.czc.service;

import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.lj.czc.pojo.bean.Sku;
import com.lj.czc.pojo.vo.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

/**
 * @author: jiangbo
 * @create: 2020-11-14
 **/
@Slf4j
@Service
public class MonitorServiceImpl {

    public static final String LIST_URL = "https://jingli-server-c.jd.com/fuli/search/searchList?page=%s&cat=653-655";
    public static final String SKU_URL = "https://jingli-server-c.jd.com/fuli/product/detail?productCode=%s&areaIds=22_1930_49324_49398";
    public static final String CART_LIST = "https://jingli-server-c.jd.com/fuli/cart/queryCartList";
    public static final String ADD_TO_CART = "https://jingli-server-c.jd.com/fuli/cart/addToCart";

    /**
     * 屏蔽的品牌
     */
    @Value("${block-brand}")
    private List<String> blockBrand;

    /**
     * 屏蔽的商品
     */
    private Set<String> blockSku = new HashSet<>();

    @Value("${cookie}")
    private String cookie;

    private static final String CODE = "-XWJ7oIOzkBMCQobPP_G4QLsUK2-f6_nOFsEDSm6rRk=";

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private RobotServiceImpl robotService;

    @Autowired
    private SkuServiceImpl skuService;

    private final AtomicBoolean monitorNew = new AtomicBoolean(false);

    private final AtomicBoolean monitorPrice = new AtomicBoolean(false);

    public List<Sku> list(String cookie) {
        if (!Strings.isBlank(cookie) && !this.cookie.equals(cookie)) {
            this.cookie = cookie;
            log.info(cookie);
        }
        return skuService.findAll();
    }

    public void initSpuId(){
        List<Sku> skus = skuService.findAll();
        List<Sku> newSkus = new ArrayList<>();
        for (Sku sku : skus) {
            String skuId = sku.getSkuId();
            Optional<Sku> dbSkuOpt = skuService.findById(skuId);
            dbSkuOpt.ifPresent((dbSku) -> {
                HashSet<String> similarSkuIds = new HashSet<>();
                similarSkuIds.add(skuId);
                List<Sku> similars = travelSimilar(new ArrayList<>(), similarSkuIds);
                if (!CollectionUtils.isEmpty(similars)) {
                    for (Sku similar : similars) {
                        similar.setSpuId(skuId);
                        log.info("新上架商品[{}]的关联商品[{}]状态[{}]", skuId, similar.getSkuId(), similar.getDesc());
                        // robotService.send(buildMsg(similar, "诚至诚商品上架提示"));
                    }
                    newSkus.addAll(similars);
                }
            });
        }
        skuService.saveAll(newSkus);
        // 触发加入购物车
        addToCart();
    }

    /**
     * 添加购物车
     */
    public void addToCart() {
        List<Sku> all = skuService.findAll();
        List<SkuVo> cartList = cartList();
        List<String> cartSkuIds = CollectionUtils.isEmpty(cartList) ? new ArrayList<>() : cartList.stream().map(SkuVo::getSkuId).collect(toList());
        for (Sku sku : all) {
            String skuId = sku.getSkuId();
            if (!cartSkuIds.contains(skuId)) {
                String requestBody = String.format("{\"num\":1,\"productCode\":\"\",\"skuId\":%s,\"skuType\":1}", skuId);
                sendPostRequest(ADD_TO_CART, requestBody, String.class, (data) -> data == null ? Strings.EMPTY : data).ifPresent((s) -> log.info("[{}]添加购物车成功", skuId));
            }
        }
    }

    /**
     * 查询购物车列表
     *
     * @return 购物车列表数据
     */
    public List<SkuVo> cartList() {
        return sendPostRequest(CART_LIST, null, CartDataVo.class, CartDataVo::getSkuVoList).orElse(Collections.emptyList());
    }

    /**
     * @param page         爬取第几页
     * @return 总共有多少页
     */
    private int getPage(int page) {
        String listUrl = String.format(LIST_URL, page);
        return sendGetRequest(listUrl, ListDataVo.class, (data) -> {
            Integer pageCount = data.getPageCount();
            List<SkuInfoDto> skuInfoDtos = data.getSkuInfoDtos();
            if (!CollectionUtils.isEmpty(skuInfoDtos)) {
                for (SkuInfoDto skuInfoDto : skuInfoDtos) {
                    StockInfo stockInfo = skuInfoDto.getStockInfo();
                    if (stockInfo != null) {
                        Sku newSku = Sku.generate(skuInfoDto);
                        handleSku(newSku);
                    }
                }
            }
            return pageCount;
        }).orElse(0);
    }

    /**
     * 处理sku逻辑，如果新增，则向列表增加，如果状态更新，则更新列表，并且把更新的列表的序列号和时间更新
     *
     * @param newSku
     */
    private void handleSku(Sku newSku) {
        String skuId = newSku.getSkuId();
        String desc = newSku.getDesc();
        if (!blockSku.contains(skuId)) {
            Optional<Sku> skuOptional = skuService.findById(skuId);
            if (!skuOptional.isPresent()) {
                // 新商品
                for (String brand : blockBrand) {
                    if (newSku.getName().contains(brand)) {
                        blockSku.add(skuId);
                        return;
                    }
                }
                List<Sku> skus = travelSimilar(new ArrayList<>(), Collections.singleton(newSku.getSkuId()));
                if (!CollectionUtils.isEmpty(skus)) {
                    for (Sku sku : skus) {
                        sku.setSpuId(skuId);
                        log.info("新上架商品[{}]状态[{}]", skuId, desc);
                        robotService.send(buildMsg(newSku, "诚至诚商品上架提示"));
                    }
                    skuService.saveAll(skus);
                    // 触发加入购物车
                    addToCart();
                }
            }
        }
    }

    /**
     * 监控新商品，通过商品列表去监控新上架商品
     */
    @PostConstruct
    public void monitorNewItem() {
        loopExecAndRetry("新商品监控", monitorNew, () -> {
            int page = 1;
            int totalPage = getPage(page);
            for (int i = page + 1; i <= totalPage; i++) {
                getPage(i);
            }
            printListInfo();
            return true;
        });
    }

    /**
     * 通过获取购物车里面的商品的状态来更新已有商品的状态
     */
    public void updateSkuByFromCart() {
        List<SkuVo> cartList = cartList();
        if (!CollectionUtils.isEmpty(cartList)) {
            List<String> skuIds = cartList.stream().map(SkuVo::getSkuId).collect(toList());
            Map<String, SkuVo> newSkuIdMapInfo = cartList.stream().collect(toMap(SkuVo::getSkuId, i -> i));
            Map<String, Sku> skuIdMapSku = skuService.findAllById(skuIds).stream().collect(toMap(Sku::getSkuId, i -> i));
            List<Sku> changedSkus = new ArrayList<>();
            for (String skuId : skuIds) {
                Sku sku = skuIdMapSku.get(skuId);
                SkuVo skuVo = newSkuIdMapInfo.get(skuId);
                if ((!Objects.equals(sku.getGoodsState(), skuVo.getGoodsState()) ||
                        !Objects.equals(sku.getWPrice(), skuVo.getModelPrice()))) {
                    sku.setWPrice(skuVo.getModelPrice());
                    sku.setGoodsState(skuVo.getGoodsState());
                    if (skuVo.getGoodsState() == 0 && Double.parseDouble(skuVo.getModelPrice()) <= Double.parseDouble(sku.getNotifyPrice())) {
                        robotService.send(buildMsg(sku, "诚至诚商品变更提示"));
                    }
                    changedSkus.add(sku);
                }
            }
            if (!CollectionUtils.isEmpty(changedSkus)) {
                skuService.saveAll(changedSkus);
            }
        }
    }

    /**
     * 通过购物车列表监控商品状态（价格，是否可买）
     */
    @PostConstruct
    public void monitorPrice() {
        loopExecAndRetry("商品价格监控", monitorPrice, () -> {
            updateSkuByFromCart();
            return true;
        });
    }

    /**
     * 循环执行任务，并且失败重试
     * @param name 执行任务的名称
     * @param lock 执行任务的锁，避免单个任务被重复执行
     * @param supplier 执行任务的内容
     */
    private void loopExecAndRetry(String name, AtomicBoolean lock, Supplier<Boolean> supplier){
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(() -> {
            if (lock.compareAndSet(false, true)) {
                int tryTimes = 0;
                robotService.send(String.format("[%s]线程正在初始化...", name));
                try {
                    while (true) {
                        try {
                            Boolean success = supplier.get();
                            if (success) {
                                tryTimes = 0;
                            } else {
                                tryTimes++;
                            }
                        } catch (Exception e) {
                            if (++tryTimes > 3) {
                                log.error("[{}]线程发生异常[{}],超出重试次数,正在停止", name, e.getMessage());
                                break;
                            }
                            log.error("[{}]线程发生异常[{}],正在进行第[{}]次尝试", name, e.getMessage(), tryTimes);
                            e.printStackTrace();
                            Thread.sleep(10000);
                        }
                        Thread.sleep(1000);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                robotService.sendRestartCard(String.format("[%s]线程异常终止，请尝试重新启动", name));
                lock.set(false);
            } else {
                String msg = String.format("[%s]线程正在监控中，请勿重复监控", name);
                log.info(msg);
                robotService.sendRestartCard(msg);
            }
        });
    }

    /**
     * 遍历给定商品的所有同型号商品
     *
     * @param visitedSkuIds 记录遍历过的节点
     * @param similarSkuIds 记录所有的关联节点
     * @return 所有关联商品的信息
     */
    private List<Sku> travelSimilar(Collection<String> visitedSkuIds, Set<String> similarSkuIds) {
        ArrayList<String> notVisitedIds = new ArrayList<>(similarSkuIds);
        notVisitedIds.removeAll(visitedSkuIds);
        if (CollectionUtil.isEmpty(notVisitedIds)) {
            return Collections.emptyList();
        }
        List<Sku> similarSkus = new ArrayList<>();
        for (String notVisitedId : notVisitedIds) {
            Optional<Sku> skuOptional = getSkuInfo(notVisitedId);
            skuOptional.ifPresent(sku -> {
                similarSkus.add(sku);
                List<String> itemSimilarSkuIds = sku.getSimilarSkus();
                if (!CollectionUtils.isEmpty(itemSimilarSkuIds)) {
                    similarSkuIds.addAll(itemSimilarSkuIds);
                }
            });
            visitedSkuIds.add(notVisitedId);
        }
        List<Sku> nextSimilarSkus = travelSimilar(visitedSkuIds, similarSkuIds);
        similarSkus.addAll(nextSimilarSkus);
        return similarSkus;
    }


    private Optional<Sku> getSkuInfo(String skuId) {
        String skuUrl = String.format(SKU_URL, skuId);
        return sendGetRequest(skuUrl, DetailDataVo.class, (data) -> {
            Sku sku = Sku.generate(data);
            List<String> similarSkuIds = data.getSimilarProducts().stream()
                    .flatMap(similarProduct -> similarProduct.getSaleAttrList().stream().
                            flatMap(saleAttr -> saleAttr.getSkuIds().stream()))
                    .map(String::valueOf)
                    .collect(toList());
            sku.setSimilarSkus(similarSkuIds);
            return sku;
        });
    }

    private <T, E> Optional<E> sendGetRequest(String url, Class<T> clazz, Function<T, E> function) {
        HttpEntity<String> entity = getEntity(null);
        return sendRequest(url, HttpMethod.GET, entity, clazz, function);
    }

    private <T, E> Optional<E> sendPostRequest(String url, String requestBody, Class<T> clazz, Function<T, E> function) {
        HttpEntity<String> entity = getEntity(requestBody);
        return sendRequest(url, HttpMethod.POST, entity, clazz, function);
    }

    @NotNull
    private HttpEntity<String> getEntity(String requestBody) {
        HttpHeaders headers = new HttpHeaders();
        String cookie = String.format("sam_cookie_activity=%s; activityCode=\"%s\"", this.cookie, CODE);
        headers.add("cookie", cookie);
        HttpEntity<String> entity = Strings.isBlank(requestBody) ? new HttpEntity<>(headers) : new HttpEntity<>(requestBody, headers);
        return entity;
    }

    private <T, E> Optional<E> sendRequest(String url, HttpMethod httpMethod, HttpEntity<?> httpEntity, Class<T> clazz, Function<T, E> function) {
        ResponseEntity<String> responseEntity = restTemplate.exchange(url, httpMethod, httpEntity, String.class);
        HttpStatus statusCode = responseEntity.getStatusCode();
        String body = responseEntity.getBody();
        if (HttpStatus.OK == statusCode) {
            try {
                JSONObject responseData = JSONObject.parseObject(body);
                T data = responseData.getObject("data", clazz);
                Integer code = responseData.getObject("code", Integer.class);
                if (code == 1000) {
                    return Optional.ofNullable(function.apply(data));
                } else {
                    log.error("服务器状态码错误[{}]", body);
                }
            } catch (JSONException e) {
                log.error("解析json异常[{}]", body);
            }
        } else {
            log.error("请求异常http status[{}]，response body[{}]", statusCode.value(), body);
        }
        return Optional.empty();
    }

    private String buildMsg(Sku sku, String title) {
        return title + "\n" +
                "商品id：" + sku.getSkuId() + "\n" +
                "商品名称：" + sku.getName() + "\n" +
                "商品状态：" + sku.getDesc() + "\n" +
                "提醒价格：" + sku.getNotifyPrice() + "\n" +
                "批发价格：" + sku.getWPrice();
    }

    private void printListInfo() {
        Map<String, List<String>> reversed = new HashMap<>();
        List<Sku> all = skuService.findAll();
        for (Sku sku : all) {
            String desc = sku.getDesc();
            if (reversed.containsKey(desc)) {
                reversed.get(desc).add(sku.getSkuId());
            } else {
                reversed.put(desc, new ArrayList<>(Collections.singletonList(sku.getSkuId())));
            }
        }
        log.info("总监控商品数[{}],商品状态[{}]", all.size(), JSON.toJSONString(reversed));
    }

}

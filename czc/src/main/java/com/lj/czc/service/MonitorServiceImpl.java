package com.lj.czc.service;

import com.alibaba.fastjson.JSON;
import com.lj.czc.pojo.bean.Sku;
import com.lj.czc.pojo.vo.ListDataVo;
import com.lj.czc.pojo.vo.SkuInfoDto;
import com.lj.czc.pojo.vo.SkuVo;
import com.lj.czc.pojo.vo.StockInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
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

    /**
     * 屏蔽的品牌
     */
    @Value("${block-brand}")
    private List<String> blockBrand;

    /**
     * 屏蔽的商品
     */
    private final Set<String> blockSku = new HashSet<>();

    @Autowired
    private RobotServiceImpl robotService;

    @Autowired
    private SkuServiceImpl skuService;

    @Autowired
    private RequestHelper requestHelper;

    private final AtomicBoolean monitorNew = new AtomicBoolean(false);

    private final AtomicBoolean monitorPrice = new AtomicBoolean(false);

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
     * 通过购物车列表监控商品状态（价格，是否可买）
     */
    @PostConstruct
    public void monitorCart() {
        loopExecAndRetry("商品价格监控", monitorPrice, () -> {
            checkSkuFromCart();
            return true;
        });
    }

    /**
     * @param page 爬取第几页
     * @return 总共有多少页
     */
    private int getPage(int page) {
        String listUrl = String.format(LIST_URL, page);
        return requestHelper.sendGetRequest(listUrl, ListDataVo.class, (data) -> {
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
     * 通过获取购物车里面的商品的状态来更新已有商品的状态
     */
    public void checkSkuFromCart() {
        List<SkuVo> cartList = skuService.cartList();
        if (!CollectionUtils.isEmpty(cartList)) {
            List<String> skuIds = cartList.stream().map(SkuVo::getSkuId).collect(toList());
            Map<String, Sku> skuIdMapSku = skuService.findAllById(skuIds).stream().collect(toMap(Sku::getSkuId, i -> i));
            List<Sku> changedSkus = new ArrayList<>();

            // 更新购物车商品价格
            for (SkuVo skuVo : cartList) {
                Sku sku = skuIdMapSku.remove(skuVo.getSkuId());
                if (sku!=null){
                    if ((!Objects.equals(sku.getGoodsState(), skuVo.getGoodsState())
                            || !Objects.equals(sku.getWPrice(), skuVo.getModelPrice())
                            || !Objects.equals(sku.getDesc(), skuVo.getStockInfo().getDesc()))) {
                        sku.setWPrice(skuVo.getModelPrice());
                        sku.setGoodsState(skuVo.getGoodsState());
                        sku.setDesc(skuVo.getStockInfo().getDesc());
                        if (null != sku.getNotifyPrice() && sku.getNotifyPrice() > 0) {
                            Integer notifyPrice = null;
                            if (null != sku.getSoldPrice() && sku.getSoldPrice() > 0) {
                                notifyPrice = skuService.calNotifyPrice(sku.getSoldPrice());
                            } else if (null != skuVo.getModelPrice()) {
                                // 没有设置售出价格就使用默认的提醒价格
                                notifyPrice = sku.getNotifyPrice();
                            }

                            if (null != notifyPrice
                                    && skuVo.getGoodsState() == 0
                                    && skuVo.getModelPrice() < notifyPrice) {
                                sku.setNotifyPrice(notifyPrice);
                                robotService.send(buildMsg(sku, "诚至诚商品变更提示"));
                            }
                        }
                        changedSkus.add(sku);
                    }
                }
            }

            // 把没有在购物车的商品添加至购物车
            Set<String> skuNotInCart = skuIdMapSku.keySet();
            if (!CollectionUtils.isEmpty(skuNotInCart)){
                skuService.addToCart(skuNotInCart);
            }

            if (!CollectionUtils.isEmpty(changedSkus)) {
                skuService.saveAll(changedSkus);
            }
        }
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
                Set<String> root = new HashSet<>();
                root.add(newSku.getSkuId());
                List<Sku> skus = skuService.travelSimilar(new ArrayList<>(), root);
                if (!CollectionUtils.isEmpty(skus)) {
                    for (Sku sku : skus) {
                        sku.setSpuId(skuId);
                        log.info("新上架商品[{}]状态[{}]", skuId, desc);
                        robotService.send(buildMsg(newSku, "诚至诚商品上架提示"));
                    }
                    skuService.saveAll(skus);
                    // 触发加入购物车
                    skuService.addAllToCart();
                }
            }
        }
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

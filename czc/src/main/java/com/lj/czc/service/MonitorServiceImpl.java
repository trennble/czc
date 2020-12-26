package com.lj.czc.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.lj.czc.pojo.vo.ListDataVo;
import com.lj.czc.pojo.vo.SkuInfoDto;
import com.lj.czc.pojo.vo.StockInfo;
import com.lj.czc.pojo.bean.Sku;
import com.lj.czc.util.EmailUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author: jiangbo
 * @create: 2020-11-14
 **/
@Slf4j
@Service
public class MonitorServiceImpl {

    public static final String LIST_URL = "https://jingli-server-c.jd.com/fuli/search/searchList?page=%s&cat=653-655";
    public static final String SKU_URL = "https://jingli-server-c.jd.com/fuli/product/detail?productCode=%s&areaIds=22_1930_49324_49398";

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

    private final AtomicBoolean onMonitor = new AtomicBoolean(false);

    public List<Sku> list(String cookie){
        if (!Strings.isBlank(cookie) && !this.cookie.equals(cookie)){
            this.cookie = cookie;
            log.info(cookie);
        }
        return skuService.findAll();
    }

    /**
     *
     * @param page 爬取第几页
     * @param serialNumber 序列号，标志当前是第几次爬去，如果商品的序列号小于当前爬去的序列号，那么说明该商品已被下架
     * @return 总共有多少页
     */
    private int getPage(int page, int serialNumber){
        HttpHeaders headers = new HttpHeaders();
        String cookie = String.format("sam_cookie_activity=%s; activityCode=\"%s\"", this.cookie, CODE);
        headers.add("cookie", cookie);
        HttpEntity<String> entity = new HttpEntity<>(headers);
        String listUrl = String.format(LIST_URL, page);
        ResponseEntity<String> responseEntity = restTemplate.exchange(listUrl, HttpMethod.GET, entity, String.class);
        HttpStatus statusCode = responseEntity.getStatusCode();
        String body = responseEntity.getBody();
        Integer pageCount = 0;
        if (HttpStatus.OK == statusCode) {
            try {
                ListDataVo data = JSONObject.parseObject(body).getObject("data", ListDataVo.class);
                pageCount = data.getPageCount();
                List<SkuInfoDto> skuInfoDtos = data.getSkuInfoDtos();
                if (!CollectionUtils.isEmpty(skuInfoDtos)){
                    for (SkuInfoDto skuInfoDto : skuInfoDtos) {
                        StockInfo stockInfo = skuInfoDto.getStockInfo();
                        if (stockInfo!=null){
                            Sku newSku = Sku.generate(skuInfoDto, serialNumber);
                            handleSku(newSku);
                        }
                    }
                }
            } catch (JSONException e) {
                log.error("url: {}, http status: {}, response body: {}", listUrl, statusCode.value(), body);
                EmailUtil.sendHtml(body);
            }
        } else {
            log.error("url: {}, http status: {}, response body: {}", listUrl, statusCode.value(), body);
            EmailUtil.sendHtml(body);
        }
        return pageCount;
    }

    /**
     * 处理sku逻辑，如果新增，则向列表增加，如果状态更新，则更新列表，并且把更新的列表的序列号和时间更新
     * @param newSku
     */
    private void handleSku(Sku newSku) {
        String wPrice = newSku.getWPrice();
        String skuId = newSku.getSkuId();
        String desc = newSku.getDesc();
        Integer serialNumber = newSku.getSerialNumber();
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
                skuService.save(newSku);
                log.info("新上架商品[{}]状态[{}]", skuId, desc);
                robotService.send(buildMsg(newSku, "诚至诚商品上架提示"));
            } else {
                // 已有的商品
                Sku existSku = skuOptional.get();
                Integer oldSerialNumber = existSku.getSerialNumber();
                String oldDesc = existSku.getDesc();
                String oldWPrice = existSku.getWPrice();
                if (!Objects.equals(oldDesc, desc)
                        || !Objects.equals(oldWPrice, wPrice)
                        || (serialNumber - oldSerialNumber) > 1) {
                    log.info("商品[{}]状态变更[{}]", skuId, desc);
                    skuService.save(newSku);
                    if ("有货".equals(desc)) {
                        robotService.send(buildMsg(newSku, "诚至诚商品变更提示"));
                    }
                }
            }
        }
    }

    @PostConstruct
    public void monitorList() {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(() -> {
            if (onMonitor.compareAndSet(false, true)){
                int tryTimes = 0;
                int serialNumber = 0;
                robotService.send("监控程序正在初始化...");
                try {
                    while (true) {
                        try {
                            int page = 1;
                            int totalPage = getPage(page, serialNumber);
                            for (int i = page + 1; i <= totalPage; i++) {
                                getPage(i, serialNumber);
                            }
                            printListInfo();
                            tryTimes = 0;
                        } catch (Exception e) {
                            if (++tryTimes > 3) {
                                log.error("程序异常[{}],超出重试次数,正在停止", e.getMessage());
                                break;
                            }
                            log.error("程序发生异常[{}],正在进行第[{}]次尝试", e.getMessage(), tryTimes);
                            e.printStackTrace();
                            Thread.sleep(10000);
                        }
                        Thread.sleep(1000);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                robotService.sendRestartCard("监控程序异常终止，请尝试重新启动");
                onMonitor.set(false);
            }else{
                String msg = "程序正在监控中，请勿重复监控";
                log.info(msg);
                robotService.sendRestartCard(msg);
            }
        });
    }

    private String buildMsg(Sku sku, String title) {
        String url = String.format(SKU_URL, sku.getSkuId());
        return title + "\n" +
                "商品id：" + sku.getSkuId() + "\n" +
                "商品名称：" + sku.getName() + "\n" +
                "商品状态：" + sku.getDesc() + "\n" +
                "京东价格：" + sku.getHPrice() + "\n" +
                "批发价格：" + sku.getWPrice() + "\n" +
                "商品链接：" + url;
    }

    private void printListInfo(){
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

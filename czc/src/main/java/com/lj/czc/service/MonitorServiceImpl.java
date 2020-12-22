package com.lj.czc.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.lj.czc.vo.ListDataVo;
import com.lj.czc.vo.SkuInfoDto;
import com.lj.czc.vo.StockInfo;
import com.lj.czc.pojo.SkuInfo;
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


    @Value("${skus}")
    private List<String> skus;

    @Value("${cookie}")
    private String cookie;

    private static final String CODE = "-XWJ7oIOzkBMCQobPP_G4QLsUK2-f6_nOFsEDSm6rRk=";

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private RobotServiceImpl robotService;

    private Map<String, String> skuStatus = new ConcurrentHashMap<>();

    private Map<Long, SkuInfo> listSkuStatus = new ConcurrentHashMap<>();

    private Boolean inited = false;

    private AtomicBoolean onMonitor = new AtomicBoolean(false);

    public List<SkuInfo> list(String cookie){
        if (!Strings.isBlank(cookie) && !this.cookie.equals(cookie)){
            this.cookie = cookie;
            log.info(cookie);
        }
        return new ArrayList<>(listSkuStatus.values());
    }

    public boolean tryout() {
        HttpHeaders headers = new HttpHeaders();
        String cookie = String.format("sam_cookie_activity=%s; activityCode=\"%s\"", this.cookie, CODE);
        headers.add("cookie", cookie);
        HttpEntity<String> entity = new HttpEntity<>(headers);
        for (String sku : skus) {
            String skuUrl = String.format(SKU_URL, sku);
            ResponseEntity<String> responseEntity = restTemplate.exchange(skuUrl, HttpMethod.GET, entity, String.class);
            HttpStatus statusCode = responseEntity.getStatusCode();
            String body = responseEntity.getBody();
            if (HttpStatus.OK == statusCode) {
                try {
                    JSONObject data = JSONObject.parseObject(body).getJSONObject("data");
                    String desc = data.getJSONObject("productStockVo").getString("desc");
                    String name = data.getJSONObject("baseInfo").getString("skuName");
                    if (!skuStatus.containsKey(sku)){
                        skuStatus.put(sku, desc);
                        log.info("初始化商品[{}]状态[{}]", sku, desc);
                    }else{
                        String oldDesc = skuStatus.get(sku);
                        if (!Objects.equals(oldDesc, desc)){
                            log.info("商品[{}]状态变更[{}]", sku, desc);
                            skuStatus.put(sku, desc);
                            EmailUtil.sendStatusChange(new SkuInfo(Long.valueOf(sku), name, desc));
                        }else{
                            log.info("商品[{}]监控中，状态[{}]", sku, desc);
                        }
                    }
                }catch (JSONException e){
                    log.error("url: {}, http status: {}, response body: {}", skuUrl, statusCode.value(), body);
                    EmailUtil.sendHtml(body);
                    return false;
                }
            } else {
                log.error("url: {}, http status: {}, response body: {}", skuUrl, statusCode.value(), body);
                EmailUtil.sendHtml(body);
                return false;
            }
        }
        return true;
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
                        String hPrice = skuInfoDto.getHPrice();
                        String wPrice = skuInfoDto.getWPrice();
                        Long skuId = skuInfoDto.getSkuId();
                        String wareName = skuInfoDto.getWareName();
                        StockInfo stockInfo = skuInfoDto.getStockInfo();
                        if (stockInfo!=null){
                            String desc = stockInfo.getDesc();
                            SkuInfo newSkuInfo = new SkuInfo(skuId, wareName, desc, hPrice, wPrice, serialNumber);
                            handleSku(newSkuInfo);
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
     * @param newSkuInfo
     */
    private void handleSku(SkuInfo newSkuInfo) {
        String wPrice = newSkuInfo.getWPrice();
        Long skuId = newSkuInfo.getSkuId();
        String desc = newSkuInfo.getDesc();
        Integer serialNumber = newSkuInfo.getSerialNumber();
        if (!listSkuStatus.containsKey(skuId)){
            listSkuStatus.put(skuId, newSkuInfo);
            if (!inited){
                log.info("初始化商品[{}]状态[{}]", skuId, desc);
            }else{
                log.info("新上架商品[{}]状态[{}]", skuId, desc);
                robotService.send(buildMsg(newSkuInfo, "诚至诚商品上架提示"));
            }
        }else{
            SkuInfo existSku = listSkuStatus.get(skuId);
            Integer oldSerialNumber = existSku.getSerialNumber();
            String oldDesc = existSku.getDesc();
            String oldWPrice = existSku.getWPrice();
            if (!Objects.equals(oldDesc, desc)
                    || !Objects.equals(oldWPrice, wPrice)
                    || (serialNumber - oldSerialNumber) > 1) {
                log.info("商品[{}]状态变更[{}]", skuId, desc);
                existSku.setDesc(desc);
                existSku.setWPrice(wPrice);
                existSku.setSerialNumber(serialNumber);
                if ("有货".equals(desc)) {
                    robotService.send(buildMsg(newSkuInfo, "诚至诚商品变更提示"));
                }
            }
        }
    }

    // @PostConstruct
    public void monitorSku() throws InterruptedException {
        int tryTimes = 0;
        while (true) {
            boolean success = false;
            try {
                success = tryout();
            }catch (Exception e){
                e.printStackTrace();
            }
            if (!success && ++tryTimes > 3) {
                break;
            }
            Thread.sleep(1000);
        }
        log.error("重试次数过多，停止检测");
    }

    @PostConstruct
    public void monitorList() {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(() -> {
            if (onMonitor.compareAndSet(false, true)){
                int tryTimes = 0;
                int serialNumber = 0;
                try {
                    while (true) {
                        try {
                            int page = 1;
                            int totalPage = getPage(page, serialNumber);
                            for (int i = page + 1; i <= totalPage; i++) {
                                getPage(i, serialNumber);
                            }
                            printListInfo();
                            if (!inited) {
                                inited = true;
                                robotService.send("监控初始化成功");
                            }
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

    private String buildMsg(SkuInfo skuInfo, String title) {
        String url = String.format(SKU_URL, skuInfo.getSkuId());
        return title + "\n" +
                "商品id：" + skuInfo.getSkuId() + "\n" +
                "商品名称：" + skuInfo.getName() + "\n" +
                "商品状态：" + skuInfo.getDesc() + "\n" +
                "京东价格：" + skuInfo.getHPrice() + "\n" +
                "批发价格：" + skuInfo.getWPrice() + "\n" +
                "商品链接：" + url;
    }

    /**
     * 回滚序列号，如果中途发生了异常，那么回滚这次爬去的数据的序列号，防止序列号不一致
     */
    private void rollBackSerialNumber(){

    }

    private void printListInfo(){
        Map<String, List<Long>> reversed = new HashMap<>();
        for (Map.Entry<Long, SkuInfo> entry : listSkuStatus.entrySet()) {
            String desc = entry.getValue().getDesc();
            if (reversed.containsKey(desc)) {
                reversed.get(desc).add(entry.getKey());
            } else {
                reversed.put(desc, new ArrayList<>(Collections.singletonList(entry.getKey())));
            }
        }
        log.info("总监控商品数[{}],商品状态[{}]", listSkuStatus.size(), JSON.toJSONString(reversed));
    }

}

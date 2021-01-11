package com.lj.czc.service;

import cn.hutool.core.collection.CollectionUtil;
import com.lj.czc.pojo.bean.Sku;
import com.lj.czc.pojo.vo.CartDataVo;
import com.lj.czc.pojo.vo.DetailDataVo;
import com.lj.czc.pojo.vo.SkuVo;
import com.lj.czc.repo.SkuRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;

import static com.lj.czc.common.Constant.*;
import static java.util.stream.Collectors.toList;

/**
 * @author: jiangbo
 * @create: 2020-12-26
 **/
@Slf4j
@Service
public class SkuServiceImpl {

    @Autowired
    private SkuRepository skuRepository;

    @Autowired
    private CacheServiceImpl cacheService;

    @Autowired
    private RequestHelper requestHelper;

    public void reCalculateNotifyPrice(){
        List<Sku> all = skuRepository.findAll();
        for (Sku sku : all) {
            Integer soldPrice = sku.getSoldPrice();
            if (soldPrice != null && soldPrice > 0) {
                sku.setNotifyPrice(calNotifyPrice(soldPrice));
            }
        }
        skuRepository.saveAll(all);
    }

    public int calNotifyPrice(Number soldPrice){
        Integer moutai = cacheService.getMoutai();
        Integer profit = cacheService.getProfit();
        return (soldPrice.intValue() - profit) * 6000 / (7499 - moutai);
    }

    public Sku setSoldPrice(String skuId, Integer soldPrice) {
        Sku sku = findById(skuId).orElseThrow(() -> new RuntimeException("没有找到对应的商品ID"));
        sku.setSoldPrice(soldPrice);
        Integer notifyPrice = calNotifyPrice(soldPrice);
        sku.setNotifyPrice(notifyPrice);
        return save(sku);
    }

    public void initSpuId(){
        List<Sku> skus = findAll();
        List<Sku> newSkus = new ArrayList<>();
        for (Sku sku : skus) {
            String skuId = sku.getSkuId();
            Optional<Sku> dbSkuOpt = findById(skuId);
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
        saveAll(newSkus);
        // 触发加入购物车
        addAllToCart();
    }

    /**
     * 添加购物车
     */
    public void addAllToCart() {
        List<Sku> all = findAll();
        if (!CollectionUtils.isEmpty(all)){
            List<String> skuIds = all.stream().map(Sku::getSkuId).distinct().collect(toList());
            addToCart(skuIds);
        }
    }

    public void addToCart(Collection<String> skuIds){
        List<SkuVo> cartList = cartList();
        List<String> cartSkuIds = CollectionUtils.isEmpty(cartList) ? new ArrayList<>() : cartList.stream().map(SkuVo::getSkuId).collect(toList());
        for (String skuId : skuIds) {
            if (!cartSkuIds.contains(skuId)) {
                String requestBody = String.format("{\"num\":1,\"productCode\":\"\",\"skuId\":%s,\"skuType\":1}", skuId);
                requestHelper.sendPostRequest(ADD_TO_CART, requestBody, String.class, (data) -> data == null ? Strings.EMPTY : data).ifPresent((s) -> log.info("[{}]添加购物车成功", skuId));
            }
        }
    }

    /**
     * 查询购物车列表
     *
     * @return 购物车列表数据
     */
    public List<SkuVo> cartList() {
        return requestHelper.sendPostRequest(CART_LIST, null, CartDataVo.class, CartDataVo::getSkuVoList).orElse(Collections.emptyList());
    }

    /**
     * 遍历给定商品的所有同型号商品
     *
     * @param visitedSkuIds 记录遍历过的节点
     * @param similarSkuIds 记录所有的关联节点
     * @return 所有关联商品的信息
     */
    public List<Sku> travelSimilar(Collection<String> visitedSkuIds, Set<String> similarSkuIds) {
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
        return requestHelper.sendGetRequest(skuUrl, DetailDataVo.class, (data) -> {
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

    public Optional<Sku> findById(String id){
        return skuRepository.findById(id);
    }

    public List<Sku> findAll(){
        return skuRepository.findAll();
    }

    public Sku save(Sku sku){
        return skuRepository.save(sku);
    }

    public List<Sku> saveAll(List<Sku> sku){
        return skuRepository.saveAll(sku);
    }

    public List<Sku> findAllById(List<String> skuIds){
        ArrayList<Sku> skus = new ArrayList<>();
        for (Sku sku : skuRepository.findAllById(skuIds)) {
            skus.add(sku);
        }
        return skus;
    }
}

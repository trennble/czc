package com.lj.czc.service;

import com.lj.czc.pojo.bean.Sku;
import com.lj.czc.repo.SkuRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * @author: jiangbo
 * @create: 2020-12-26
 **/
@Service
public class SkuServiceImpl {

    @Autowired
    private SkuRepository skuRepository;

    public Optional<Sku> findById(String id){
        return skuRepository.findById(id);
    }

    public List<Sku> findAll(){
        return skuRepository.findAll();
    }

    public Sku save(Sku sku){
        return skuRepository.save(sku);
    }
}

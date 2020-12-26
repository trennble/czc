package com.lj.czc.repo;

import com.lj.czc.pojo.bean.Sku;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SkuRepository extends MongoRepository<Sku, String> {
}
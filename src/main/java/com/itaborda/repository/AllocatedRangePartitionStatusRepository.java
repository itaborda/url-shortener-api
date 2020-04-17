package com.itaborda.repository;

import com.itaborda.model.AllocatedRangePartitionStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AllocatedRangePartitionStatusRepository extends MongoRepository<AllocatedRangePartitionStatus,String> {

}

package com.bluewave.civicconnect.complains;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ComplainSearchRepository extends ElasticsearchRepository<ComplainRepo.ComplainSearchDocument, String> {
}

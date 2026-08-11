package com.bluewave.civicconnect.complains;

import com.bluewave.civicconnect.complains.dto.ComplainSearchDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ComplainSearchRepository extends ElasticsearchRepository<ComplainSearchDocument, String> {
}

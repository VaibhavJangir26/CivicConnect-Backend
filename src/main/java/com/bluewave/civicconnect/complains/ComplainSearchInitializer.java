package com.bluewave.civicconnect.complains;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class ComplainSearchInitializer {

    private final ComplainRepo complainRepo;
    private final ComplainSearchRepository complainSearchRepository;
    private final ComplainSearchService complainSearchService;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional(readOnly = true)
    public void syncExistingComplaintsToElasticsearch() {
        try {
            long count = complainSearchRepository.count();
            if (count == 0) {
                log.info("Elasticsearch complaints index is empty. Populating from Database...");
                List<Complains> allComplains = complainRepo.findAll();
                if (!allComplains.isEmpty()) {
                    List<ComplainSearchDocument> docs = allComplains.stream()
                            .map(complainSearchService::mapToSearchDocument)
                            .collect(Collectors.toList());
                    complainSearchRepository.saveAll(docs);
                    log.info("Successfully indexed {} complaints into Elasticsearch.", docs.size());
                } else {
                    log.info("No existing complaints found in Database to index.");
                }
            } else {
                log.info("Elasticsearch complaints index already populated with {} documents.", count);
            }
        } catch (Exception e) {
            log.warn("Could not sync complaints to Elasticsearch on startup: {}", e.getMessage());
        }
    }
}

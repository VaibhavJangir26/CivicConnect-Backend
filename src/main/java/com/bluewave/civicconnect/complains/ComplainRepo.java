package com.bluewave.civicconnect.complains;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import com.bluewave.civicconnect.complains.constatns.ComplainStatus;
import com.bluewave.civicconnect.complains.dto.ComplainSearchRequestDTO;
import com.bluewave.civicconnect.profile.Address;
import com.bluewave.civicconnect.profile.Profile;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.annotation.Id;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
public interface ComplainRepo extends JpaRepository<Complains, String> {

    // For Managers/Admins filtering
    List<Complains> findByComplainStatus(ComplainStatus status);

    // For Citizens to see only their complaints
    List<Complains> findByProfile(Profile profile);
    List<Complains> findByProfileAndComplainStatus(Profile profile, ComplainStatus status);

    // For Officers to see only complaints assigned to them
    List<Complains> findByAssignedOfficer(Profile officer);
    List<Complains> findByAssignedOfficerAndComplainStatus(Profile officer, ComplainStatus status);

    @Service
    @RequiredArgsConstructor
    @Slf4j
    class ComplainSearchService {

        private final ElasticsearchOperations elasticsearchOperations;
        private final ComplainSearchRepository complainSearchRepository;

        private static final String SEARCH_CACHE = "complains_search";
        private static final String SUGGEST_CACHE = "complains_suggest";

        /**
         * Global Fuzzy Search with Filters & Pagination (Redis Cached)
         */
        @Cacheable(value = SEARCH_CACHE, key = "(#dto.keyword != null ? #dto.keyword.trim().toLowerCase() : '') + '-' + (#dto.status != null ? #dto.status : 'ALL') + '-' + #dto.page + '-' + #dto.size")
        public List<ComplainSearchDocument> globalSearch(ComplainSearchRequestDTO dto) {
            log.info("Executing Elasticsearch global search for keyword: {}, status: {}", dto.getKeyword(), dto.getStatus());

            BoolQuery.Builder boolQuery = new BoolQuery.Builder();

            // 1. Multi-field Fuzzy Match on Keyword
            if (dto.getKeyword() != null && !dto.getKeyword().isBlank()) {
                String term = dto.getKeyword().trim();
                boolQuery.must(Query.of(q -> q.multiMatch(m -> m
                        .fields("message^3", "categoryName^2", "address", "citizenName", "citizenUsername", "assignedOfficerName")
                        .query(term)
                        .fuzziness("AUTO")
                        .prefixLength(1)
                )));
            }

            // 2. Filter by Complain Status
            if (dto.getStatus() != null) {
                boolQuery.filter(Query.of(q -> q.term(t -> t.field("complainStatus").value(dto.getStatus().name()))));
            }

            // 3. Date Range Filter
            if (dto.getStartDate() != null && dto.getEndDate() != null) {
                boolQuery.filter(Query.of(q -> q.range(r -> r.date(d -> d.field("createdAt").gte(dto.getStartDate().toString()).lte(dto.getEndDate().toString())))));
            } else if (dto.getStartDate() != null) {
                boolQuery.filter(Query.of(q -> q.range(r -> r.date(d -> d.field("createdAt").gte(dto.getStartDate().toString())))));
            } else if (dto.getEndDate() != null) {
                boolQuery.filter(Query.of(q -> q.range(r -> r.date(d -> d.field("createdAt").lte(dto.getEndDate().toString())))));
            }

            // 4. Build NativeQuery with Pagination
            Query finalQuery;
            boolean hasKeyword = dto.getKeyword() != null && !dto.getKeyword().isBlank();
            boolean hasStatus = dto.getStatus() != null;
            boolean hasDates = dto.getStartDate() != null || dto.getEndDate() != null;

            if (!hasKeyword && !hasStatus && !hasDates) {
                finalQuery = Query.of(q -> q.matchAll(m -> m));
            } else {
                finalQuery = Query.of(q -> q.bool(boolQuery.build()));
            }

            NativeQuery nativeQuery = NativeQuery.builder()
                    .withQuery(finalQuery)
                    .withPageable(PageRequest.of(Math.max(0, dto.getPage()), Math.max(1, dto.getSize())))
                    .build();

            SearchHits<ComplainSearchDocument> searchHits = elasticsearchOperations.search(nativeQuery, ComplainSearchDocument.class);

            return searchHits.stream()
                    .map(SearchHit::getContent)
                    .collect(Collectors.toList());
        }

        /**
         * Autosuggestion endpoint with prefix and fuzzy matching (Redis Cached)
         */
        @Cacheable(value = SUGGEST_CACHE, key = "#prefix != null ? #prefix.trim().toLowerCase() : ''")
        public List<String> autosuggest(String prefix) {
            if (prefix == null || prefix.isBlank()) {
                return List.of();
            }

            String term = prefix.trim();
            log.info("Executing Elasticsearch autosuggestion query for prefix: {}", term);

            NativeQuery suggestQuery = NativeQuery.builder()
                    .withQuery(Query.of(q -> q.multiMatch(m -> m
                            .fields("message^3", "categoryName^2", "address")
                            .query(term)
                            .type(co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType.BoolPrefix)
                            .fuzziness("AUTO")
                    )))
                    .withPageable(PageRequest.of(0, 10))
                    .build();

            SearchHits<ComplainSearchDocument> hits = elasticsearchOperations.search(suggestQuery, ComplainSearchDocument.class);

            Set<String> suggestions = new LinkedHashSet<>();
            for (SearchHit<ComplainSearchDocument> hit : hits) {
                ComplainSearchDocument doc = hit.getContent();
                if (doc.getCategoryName() != null && doc.getCategoryName().toLowerCase().contains(term.toLowerCase())) {
                    suggestions.add(doc.getCategoryName());
                }
                if (doc.getMessage() != null) {
                    suggestions.add(truncateMessage(doc.getMessage(), 60));
                }
                if (doc.getAddress() != null && doc.getAddress().toLowerCase().contains(term.toLowerCase())) {
                    suggestions.add(doc.getAddress());
                }
            }

            return new ArrayList<>(suggestions);
        }

        /**
         * Sync JPA Complains entity to Elasticsearch
         */
        @CacheEvict(value = {SEARCH_CACHE, SUGGEST_CACHE}, allEntries = true)
        public void indexComplain(Complains complain) {
            if (complain == null) return;
            try {
                ComplainSearchDocument doc = mapToSearchDocument(complain);
                complainSearchRepository.save(doc);
                log.info("Indexed complaint into Elasticsearch -> ID: {}", complain.getId());
            } catch (Exception e) {
                log.error("Failed to index complaint ID {} into Elasticsearch: {}", complain.getId(), e.getMessage());
            }
        }

        /**
         * Delete complaint from Elasticsearch index
         */
        @CacheEvict(value = {SEARCH_CACHE, SUGGEST_CACHE}, allEntries = true)
        public void deleteComplainFromIndex(String complainId) {
            if (complainId == null) return;
            try {
                complainSearchRepository.deleteById(complainId);
                log.info("Deleted complaint from Elasticsearch index -> ID: {}", complainId);
            } catch (Exception e) {
                log.error("Failed to delete complaint ID {} from Elasticsearch: {}", complainId, e.getMessage());
            }
        }

        /**
         * Map JPA Entity to Elasticsearch Search Document
         */
        public ComplainSearchDocument mapToSearchDocument(Complains complain) {
            String fullAddress = formatAddress(complain.getAddress());
            String citizenFullName = complain.getProfile() != null ? complain.getProfile().getFullName() : null;
            String citizenUser = (complain.getProfile() != null && complain.getProfile().getUsers() != null)
                    ? complain.getProfile().getUsers().getUsername() : null;
            String officerFullName = complain.getAssignedOfficer() != null ? complain.getAssignedOfficer().getFullName() : null;

            return ComplainSearchDocument.builder()
                    .id(complain.getId())
                    .message(complain.getMessage())
                    .address(fullAddress)
                    .categoryName(complain.getCategory() != null ? complain.getCategory().getCategoryName() : null)
                    .complainStatus(complain.getComplainStatus() != null ? complain.getComplainStatus().name() : null)
                    .complainPriority(complain.getComplainPriority() != null ? complain.getComplainPriority().name() : null)
                    .citizenName(citizenFullName)
                    .citizenUsername(citizenUser)
                    .assignedOfficerName(officerFullName)
                    .createdAt(complain.getCreatedAt())
                    .updatedAt(complain.getUpdatedAt())
                    .build();
        }

        private String formatAddress(Address addr) {
            if (addr == null) return null;
            List<String> parts = new ArrayList<>();
            if (addr.getAddressLine() != null && !addr.getAddressLine().isBlank()) parts.add(addr.getAddressLine());
            if (addr.getCity() != null && !addr.getCity().isBlank()) parts.add(addr.getCity());
            if (addr.getState() != null && !addr.getState().isBlank()) parts.add(addr.getState());
            if (addr.getPincode() != null && !addr.getPincode().isBlank()) parts.add(addr.getPincode());
            if (addr.getCountry() != null && !addr.getCountry().isBlank()) parts.add(addr.getCountry());
            return parts.isEmpty() ? null : String.join(", ", parts);
        }

        private String truncateMessage(String str, int maxLength) {
            if (str == null) return "";
            return str.length() <= maxLength ? str : str.substring(0, maxLength) + "...";
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Document(indexName = "complaints")
    class ComplainSearchDocument implements Serializable {

        private static final long serialVersionUID = 1L;

        @Id
        private String id;

        @Field(type = FieldType.Text, analyzer = "standard")
        private String message;

        @Field(type = FieldType.Text, analyzer = "standard")
        private String address;

        @Field(type = FieldType.Keyword)
        private String categoryName;

        @Field(type = FieldType.Keyword)
        private String complainStatus;

        @Field(type = FieldType.Keyword)
        private String complainPriority;

        @Field(type = FieldType.Text, analyzer = "standard")
        private String citizenName;

        @Field(type = FieldType.Keyword)
        private String citizenUsername;

        @Field(type = FieldType.Text, analyzer = "standard")
        private String assignedOfficerName;

        @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second_millis)
        private LocalDateTime createdAt;

        @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second_millis)
        private LocalDateTime updatedAt;
    }
}
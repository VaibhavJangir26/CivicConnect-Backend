package com.bluewave.civicconnect.complains;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(indexName = "complaints")
public class ComplainSearchDocument implements Serializable {

    @Serial
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
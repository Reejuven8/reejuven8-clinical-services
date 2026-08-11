package com.reejuven8.healthdata.model.document;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Document(collection = "fhir_resources")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FhirResource {
    @Id private String id;

    @Field("patient_id") private String patientId;
    @Field("resource_type") private String resourceType;
    @Field("resource_id") private String resourceId;
    @Field("status") private String status;
    @Field("effective_datetime") private Instant effectiveDatetime;
    @Field("category") private String category;
    @Field("code") private Map<String, Object> code;
    @Field("value_quantity") private Map<String, Object> valueQuantity;
    @Field("interpretation") private Map<String, Object> interpretation;
    @Field("reference_range") private Map<String, Object> referenceRange;
    @Field("source") private String source;
    @Field("source_file_s3_url") private String sourceFileS3Url;
    @Field("parsing_metadata") private Map<String, Object> parsingMetadata;
    @Field("tags") private List<String> tags;
    @Field("notes") private String notes;
    @Field("raw_fhir_json") private Map<String, Object> rawFhirJson;
    @Field("created_at") @Builder.Default private Instant createdAt = Instant.now();
    @Field("updated_at") @Builder.Default private Instant updatedAt = Instant.now();
}

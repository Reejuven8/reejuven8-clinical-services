package com.reejuven8.healthdata.service;

import com.reejuven8.healthdata.model.document.FhirResource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecordSyncService {

    private final FhirResourceService fhirResourceService;

    @SuppressWarnings("unchecked")
    public void processAbdmBundle(String patientId, Object fhirBundle) {
        if (fhirBundle == null) {
            log.warn("Null FHIR bundle for patientId={}", patientId);
            return;
        }
        // ABDM sends FHIR Bundle — extract entry array if structured, else store as raw
        if (fhirBundle instanceof Map<?, ?> bundleMap) {
            List<?> entries = (List<?>) bundleMap.get("entry");
            if (entries != null) {
                entries.forEach(entry -> processEntry(patientId, (Map<String, Object>) entry));
                return;
            }
        }
        // Store raw bundle as a single resource
        FhirResource resource = FhirResource.builder()
            .patientId(patientId)
            .resourceType("Bundle")
            .source("ABDM")
            .status("final")
            .rawFhirJson(fhirBundle instanceof Map<?,?> m ? (Map<String, Object>) m : Map.of("raw", fhirBundle.toString()))
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();
        fhirResourceService.save(resource);
        log.info("Stored ABDM bundle for patientId={}", patientId);
    }

    @SuppressWarnings("unchecked")
    public void storeParsedObservation(String patientId, String sourceDocumentId, Object observation) {
        if (!(observation instanceof Map<?, ?> obsMap)) return;
        Map<String, Object> obs = (Map<String, Object>) obsMap;

        FhirResource resource = FhirResource.builder()
            .patientId(patientId)
            .resourceType("Observation")
            .source("AI_PARSED")
            .status("final")
            .category((String) obs.get("category"))
            .rawFhirJson(obs)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();
        fhirResourceService.save(resource);
    }

    @SuppressWarnings("unchecked")
    private void processEntry(String patientId, Map<String, Object> entry) {
        Map<String, Object> resourceMap = (Map<String, Object>) entry.get("resource");
        if (resourceMap == null) return;
        String resourceType = (String) resourceMap.get("resourceType");

        FhirResource resource = FhirResource.builder()
            .patientId(patientId)
            .resourceType(resourceType)
            .resourceId((String) resourceMap.get("id"))
            .source("ABDM")
            .status("final")
            .rawFhirJson(resourceMap)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();
        fhirResourceService.save(resource);
    }
}

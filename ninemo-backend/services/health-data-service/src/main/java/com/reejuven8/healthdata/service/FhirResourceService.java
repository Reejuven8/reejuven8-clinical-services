package com.reejuven8.healthdata.service;

import com.reejuven8.healthdata.exception.ResourceNotFoundException;
import com.reejuven8.healthdata.model.document.FhirResource;
import com.reejuven8.healthdata.model.dto.FhirResourceResponse;
import com.reejuven8.healthdata.repository.FhirResourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FhirResourceService {

    private final FhirResourceRepository repository;

    public FhirResource save(FhirResource resource) {
        return repository.save(resource);
    }

    public Page<FhirResourceResponse> getByPatient(String patientId, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return repository.findByPatientId(patientId, pageable).map(this::toResponse);
    }

    public List<FhirResourceResponse> getByPatientAndType(String patientId, String resourceType) {
        return repository.findByPatientIdAndResourceType(patientId, resourceType)
            .stream().map(this::toResponse).toList();
    }

    public FhirResourceResponse getById(String id) {
        FhirResource resource = repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("FhirResource not found: " + id));
        return toResponse(resource);
    }

    private FhirResourceResponse toResponse(FhirResource r) {
        return new FhirResourceResponse(
            r.getId(), r.getPatientId(), r.getResourceType(), r.getResourceId(),
            r.getStatus(), r.getEffectiveDatetime(), r.getCategory(),
            r.getCode(), r.getValueQuantity(), r.getSource(),
            r.getSourceFileS3Url(), r.getTags(), r.getNotes(), r.getCreatedAt()
        );
    }
}

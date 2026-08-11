package com.reejuven8.healthdata.repository;
import com.reejuven8.healthdata.model.document.FhirResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
public interface FhirResourceRepository extends MongoRepository<FhirResource, String> {
    Page<FhirResource> findByPatientId(String patientId, Pageable pageable);
    List<FhirResource> findByPatientIdAndResourceType(String patientId, String resourceType);
}

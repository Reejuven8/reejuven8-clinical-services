package com.reejuven8.ninemo.clinical.service;

import com.reejuven8.ninemo.clinical.model.dto.DietLookupResponse;
import com.reejuven8.ninemo.clinical.model.entity.DietFoodSafety;
import com.reejuven8.ninemo.clinical.repository.DietFoodSafetyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DietLookupService {

    private final DietFoodSafetyRepository repository;

    public List<DietLookupResponse> search(String query) {
        return repository.searchByName(query).stream()
            .map(this::toResponse)
            .toList();
    }

    private DietLookupResponse toResponse(DietFoodSafety entity) {
        return DietLookupResponse.builder()
            .ingredientName(entity.getIngredientName())
            .ingredientNameHindi(entity.getIngredientNameHindi())
            .safetyRating(entity.getSafetyRating().name())
            .medicalReasoning(entity.getMedicalReasoning())
            .safeQuantity(entity.getSafeQuantity())
            .build();
    }
}

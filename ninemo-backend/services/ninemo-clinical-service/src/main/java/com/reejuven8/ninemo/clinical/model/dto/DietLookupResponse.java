package com.reejuven8.ninemo.clinical.model.dto;

import lombok.Builder;
import lombok.Data;

@Data @Builder
public class DietLookupResponse {
    private String ingredientName;
    private String ingredientNameHindi;
    private String safetyRating;
    private String medicalReasoning;
    private String safeQuantity;
}

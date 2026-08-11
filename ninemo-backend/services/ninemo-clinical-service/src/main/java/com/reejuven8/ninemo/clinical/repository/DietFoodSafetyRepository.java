package com.reejuven8.ninemo.clinical.repository;

import com.reejuven8.ninemo.clinical.model.entity.DietFoodSafety;
import com.reejuven8.ninemo.clinical.model.enums.FoodSafetyRating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DietFoodSafetyRepository extends JpaRepository<DietFoodSafety, UUID> {
    List<DietFoodSafety> findBySafetyRating(FoodSafetyRating rating);

    @Query(value = "SELECT * FROM diet_food_safety WHERE ingredient_name ILIKE %:query% OR ingredient_name_hindi ILIKE %:query% LIMIT 20", nativeQuery = true)
    List<DietFoodSafety> searchByName(String query);
}

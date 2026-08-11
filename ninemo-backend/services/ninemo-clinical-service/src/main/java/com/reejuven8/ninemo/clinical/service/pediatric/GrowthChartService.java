package com.reejuven8.ninemo.clinical.service.pediatric;

import com.reejuven8.ninemo.clinical.model.document.GrowthMeasurement;
import com.reejuven8.ninemo.clinical.model.dto.GrowthInputRequest;
import com.reejuven8.ninemo.clinical.model.entity.ChildProfile;
import com.reejuven8.ninemo.clinical.model.enums.BiologicalSex;
import com.reejuven8.ninemo.clinical.repository.GrowthMeasurementRepository;
import com.reejuven8.ninemo.clinical.service.pediatric.WhoLmsTable.LmsPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class GrowthChartService {

    private final ChildAccessGuard childAccessGuard;
    private final GrowthMeasurementRepository growthMeasurementRepository;

    private static final double Z_CONCERN_LOW  = -2.0;
    private static final double Z_CONCERN_HIGH  =  2.0;
    private static final double Z_SEVERE_LOW   = -3.0;
    private static final int    PERCENTILE_CROSSING_THRESHOLD = 2;

    public GrowthMeasurement recordMeasurement(UUID childId, UUID userId, GrowthInputRequest request) {
        ChildProfile child = childAccessGuard.requireOwnedChild(childId, userId); // IS-027

        boolean isBoy = child.getBiologicalSex() == BiologicalSex.MALE;
        int ageMonths = request.getAgeInMonths();

        Map<String, Double> zScores  = new LinkedHashMap<>();
        Map<String, Integer> percentiles = new LinkedHashMap<>();
        List<String> alertFlags = new ArrayList<>();

        // Weight-for-age
        LmsPoint wazLms = WhoLmsTable.interpolate(
            isBoy ? WhoLmsTable.WAZ_BOYS : WhoLmsTable.WAZ_GIRLS, ageMonths);
        double waz = WhoLmsTable.zScore(wazLms, request.getWeightKg());
        zScores.put("weight_for_age", round2(waz));
        percentiles.put("weight_for_age", WhoLmsTable.percentile(waz));

        // Height-for-age
        LmsPoint hazLms = WhoLmsTable.interpolate(
            isBoy ? WhoLmsTable.HAZ_BOYS : WhoLmsTable.HAZ_GIRLS, ageMonths);
        double haz = WhoLmsTable.zScore(hazLms, request.getHeightCm());
        zScores.put("height_for_age", round2(haz));
        percentiles.put("height_for_age", WhoLmsTable.percentile(haz));

        // Head circumference-for-age (0–24 months only)
        if (request.getHeadCircumferenceCm() != null && ageMonths <= 24) {
            java.util.NavigableMap<Integer, LmsPoint> hczTable = isBoy ? WhoLmsTable.HCZ_BOYS : WhoLmsTable.HCZ_GIRLS;
            LmsPoint hczLms = WhoLmsTable.interpolate(hczTable, ageMonths);
            double hcz = WhoLmsTable.zScore(hczLms, request.getHeadCircumferenceCm());
            zScores.put("head_circumference_for_age", round2(hcz));
            percentiles.put("head_circumference_for_age", WhoLmsTable.percentile(hcz));
            flagZ(alertFlags, "head_circumference", hcz);
        }

        flagZ(alertFlags, "weight", waz);
        flagZ(alertFlags, "height", haz);

        // Compare against previous measurement to detect percentile line crossings
        Optional<GrowthMeasurement> prev =
            growthMeasurementRepository.findTop1ByChildIdOrderByMeasurementDateDesc(childId.toString());

        Map<String, Integer> previousPercentiles = prev.map(GrowthMeasurement::getPercentiles).orElse(null);
        int crossedLines = 0;
        if (previousPercentiles != null) {
            crossedLines = (int) percentiles.entrySet().stream()
                .filter(e -> previousPercentiles.containsKey(e.getKey()))
                .filter(e -> percentileLinesCrossed(previousPercentiles.get(e.getKey()), e.getValue()) >= PERCENTILE_CROSSING_THRESHOLD)
                .count();
            if (crossedLines > 0) {
                alertFlags.add("PERCENTILE_CROSSING: dropped across ≥2 major lines for " + crossedLines + " indicator(s)");
            }
        }

        GrowthMeasurement measurement = GrowthMeasurement.builder()
            .childId(childId.toString())
            .ageInMonths(ageMonths)
            .measurementDate(Instant.now())
            .heightCm(request.getHeightCm())
            .weightKg(request.getWeightKg())
            .headCircumferenceCm(request.getHeadCircumferenceCm())
            .zScores(zScores)
            .percentiles(percentiles)
            .previousPercentiles(previousPercentiles)
            .alertFlags(alertFlags)
            .crossedPercentileLines(crossedLines)
            .createdAt(Instant.now())
            .build();

        return growthMeasurementRepository.save(measurement);
    }

    public List<GrowthMeasurement> getHistory(UUID childId, UUID userId) {
        childAccessGuard.requireOwnedChild(childId, userId); // IS-027
        return growthMeasurementRepository.findByChildIdOrderByMeasurementDateDesc(childId.toString());
    }

    private void flagZ(List<String> flags, String metric, double z) {
        if (z < Z_SEVERE_LOW) {
            flags.add(metric.toUpperCase() + "_Z_SEVERE_UNDERNUTRITION (Z=" + round2(z) + ")");
        } else if (z < Z_CONCERN_LOW) {
            flags.add(metric.toUpperCase() + "_Z_UNDERNUTRITION (Z=" + round2(z) + ")");
        } else if (z > Z_CONCERN_HIGH) {
            flags.add(metric.toUpperCase() + "_Z_OVERWEIGHT (Z=" + round2(z) + ")");
        }
    }

    // WHO major percentile channels: 3, 15, 50, 85, 97
    private static final int[] WHO_CHANNELS = {3, 15, 50, 85, 97};

    private int percentileLinesCrossed(int from, int to) {
        int count = 0;
        for (int line : WHO_CHANNELS) {
            if ((from >= line && to < line) || (from < line && to >= line)) count++;
        }
        return count;
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}

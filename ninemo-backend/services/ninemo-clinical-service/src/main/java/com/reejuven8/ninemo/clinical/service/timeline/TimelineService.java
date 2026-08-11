package com.reejuven8.ninemo.clinical.service.timeline;

import com.reejuven8.common.exception.ResourceNotFoundException;
import com.reejuven8.ninemo.clinical.model.document.TimelineFeed;
import com.reejuven8.ninemo.clinical.model.dto.TimelineResponse;
import com.reejuven8.ninemo.clinical.model.entity.PregnancyProfile;
import com.reejuven8.ninemo.clinical.repository.PregnancyProfileRepository;
import com.reejuven8.ninemo.clinical.repository.TimelineFeedRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TimelineService {

    private final PregnancyProfileRepository pregnancyProfileRepository;
    private final TimelineFeedRepository timelineFeedRepository;

    public TimelineResponse getCurrentWeek(UUID userId) {
        PregnancyProfile profile = getActiveProfile(userId);
        int week = computeGestationalWeek(profile.getEddDate());
        return buildResponse(profile, week);
    }

    public TimelineResponse getWeek(UUID userId, int week) {
        PregnancyProfile profile = getActiveProfile(userId);
        return buildResponse(profile, Math.min(Math.max(week, 1), 42));
    }

    private PregnancyProfile getActiveProfile(UUID userId) {
        return pregnancyProfileRepository.findByUserIdAndIsActiveTrue(userId)
            .orElseThrow(() -> new ResourceNotFoundException("PregnancyProfile", "active profile for user " + userId));
    }

    public static int computeGestationalWeek(LocalDate eddDate) {
        // LMP = EDD - 280 days; gestational_week = days_since_lmp / 7 + 1
        LocalDate lmpEquivalent = eddDate.minusDays(280);
        long daysSinceLmp = ChronoUnit.DAYS.between(lmpEquivalent, LocalDate.now());
        int week = (int) (daysSinceLmp / 7) + 1;
        return Math.min(Math.max(week, 1), 42);
    }

    public static int computeTrimester(int week) {
        if (week <= 13) return 1;
        if (week <= 27) return 2;
        return 3;
    }

    private TimelineResponse buildResponse(PregnancyProfile profile, int week) {
        int trimester = computeTrimester(week);

        // Try to fetch pre-seeded content from MongoDB; fall back to generated content
        return timelineFeedRepository
            .findByPregnancyProfileIdAndGestationalWeek(profile.getId().toString(), week)
            .map(feed -> TimelineResponse.builder()
                .gestationalWeek(feed.getGestationalWeek())
                .trimester(feed.getTrimester())
                .babyDevelopment(feed.getBabyDevelopment())
                .maternalChanges(feed.getMaternalChanges())
                .scheduledMilestones(feed.getScheduledMilestones())
                .dietTips(feed.getDietTips())
                .yogaRoutine(feed.getYogaRoutine())
                .build())
            .orElseGet(() -> TimelineResponse.builder()
                .gestationalWeek(week)
                .trimester(trimester)
                .babyDevelopment(Map.of("summary", "Week " + week + " — content will be seeded."))
                .maternalChanges(List.of())
                .scheduledMilestones(upcomingMilestones(week))
                .dietTips(List.of())
                .build());
    }

    private List<Map<String, Object>> upcomingMilestones(int week) {
        // Key clinical milestones; used when timeline feed not yet seeded
        if (week < 12) return List.of(Map.of("name", "First trimester scan", "week", 12));
        if (week < 20) return List.of(Map.of("name", "Anomaly scan", "week", 20));
        if (week < 24) return List.of(Map.of("name", "GDM screening (OGTT)", "week", 24));
        if (week < 28) return List.of(Map.of("name", "Third trimester begins", "week", 28));
        if (week < 36) return List.of(Map.of("name", "Group B strep test", "week", 36));
        return List.of(Map.of("name", "Full term — monitor closely", "week", week));
    }
}

package com.reejuven8.ninemo.clinical.service.pediatric;

import com.reejuven8.ninemo.clinical.model.dto.ChildProfileResponse;
import com.reejuven8.ninemo.clinical.model.dto.ChildProfileUpdateRequest;
import com.reejuven8.ninemo.clinical.model.entity.ChildProfile;
import com.reejuven8.ninemo.clinical.model.entity.PregnancyProfile;
import com.reejuven8.ninemo.clinical.model.enums.BiologicalSex;
import com.reejuven8.ninemo.clinical.publisher.MilestoneReminderPublisher;
import com.reejuven8.ninemo.clinical.repository.ChildProfileRepository;
import com.reejuven8.ninemo.clinical.repository.PregnancyProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ModeTransitionService {

    private final PregnancyProfileRepository pregnancyProfileRepository;
    private final ChildProfileRepository childProfileRepository;
    private final VaccinationScheduleService vaccinationScheduleService;
    private final MilestoneReminderPublisher milestoneReminderPublisher;
    private final ChildAccessGuard childAccessGuard;

    /**
     * IS-027: pregnancyProfileId is client-supplied, so ownership is verified before anything
     * is mutated. IS-029: {@code request} is optional — when present, the baby's name, sex,
     * date of birth and birth measurements are captured at transition time instead of being
     * silently dropped and defaulted to null/OTHER/today.
     */
    @Transactional
    public ChildProfileResponse transitionToPostnatalMode(UUID pregnancyProfileId, UUID userId,
                                                          ChildProfileUpdateRequest request) {
        PregnancyProfile pregnancy = childAccessGuard.requireOwnedPregnancy(pregnancyProfileId, userId);

        pregnancy.setActive(false);
        pregnancy.setDeliveryDate(LocalDate.now());
        pregnancyProfileRepository.save(pregnancy);

        // Create ChildProfile if not already created at registration
        ChildProfile child = childProfileRepository
            .findByPregnancyProfileId(pregnancyProfileId)
            .orElseGet(() -> {
                ChildProfile newChild = ChildProfile.builder()
                    .pregnancyProfileId(pregnancyProfileId)
                    .parentUserId(pregnancy.getUserId())
                    .dateOfBirth(LocalDate.now())
                    .biologicalSex(BiologicalSex.OTHER) // overridden by request when supplied
                    .isActive(true)
                    .build();
                return childProfileRepository.save(newChild);
            });

        if (request != null) {
            ChildProfileService.applyUpdate(child, request);
            child = childProfileRepository.save(child);
        }

        // Generate IAP vaccination schedule
        vaccinationScheduleService.generateSchedule(child.getId());

        // Publish first postnatal milestone reminder
        milestoneReminderPublisher.publishMilestoneReminder(
            pregnancy.getUserId().toString(),
            "POSTNATAL_VISIT_WEEK_1",
            0
        );

        log.info("Transitioned pregnancyProfileId={} to postnatal; childId={}", pregnancyProfileId, child.getId());
        return ChildProfileResponse.from(child);
    }
}

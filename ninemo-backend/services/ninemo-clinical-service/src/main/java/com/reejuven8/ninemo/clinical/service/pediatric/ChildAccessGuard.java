package com.reejuven8.ninemo.clinical.service.pediatric;

import com.reejuven8.common.exception.ForbiddenException;
import com.reejuven8.common.exception.ResourceNotFoundException;
import com.reejuven8.ninemo.clinical.model.entity.ChildProfile;
import com.reejuven8.ninemo.clinical.model.entity.PregnancyProfile;
import com.reejuven8.ninemo.clinical.repository.ChildProfileRepository;
import com.reejuven8.ninemo.clinical.repository.PregnancyProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * IS-027 / NM-B-169 — ownership enforcement for child-mode resources.
 *
 * Before this existed, GrowthController / VaccinationController / MilestoneController /
 * ModeTransitionController did a bare findById on a client-supplied UUID with no check that
 * the record belonged to the caller: any authenticated user who knew a childId could read
 * another child's growth data, vaccination schedule and milestones — and mark their
 * vaccinations completed. Every child-scoped entry point now routes through here.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ChildAccessGuard {

    private final ChildProfileRepository childProfileRepository;
    private final PregnancyProfileRepository pregnancyProfileRepository;

    public ChildProfile requireOwnedChild(UUID childId, UUID userId) {
        ChildProfile child = childProfileRepository.findById(childId)
            .orElseThrow(() -> new ResourceNotFoundException("ChildProfile", childId.toString()));
        if (!child.getParentUserId().equals(userId)) {
            log.warn("Ownership violation: userId={} attempted to access childId={}", userId, childId);
            throw new ForbiddenException("childId=" + childId + " does not belong to userId=" + userId);
        }
        return child;
    }

    public PregnancyProfile requireOwnedPregnancy(UUID pregnancyProfileId, UUID userId) {
        PregnancyProfile pregnancy = pregnancyProfileRepository.findById(pregnancyProfileId)
            .orElseThrow(() -> new ResourceNotFoundException("PregnancyProfile", pregnancyProfileId.toString()));
        if (!pregnancy.getUserId().equals(userId)) {
            log.warn("Ownership violation: userId={} attempted to access pregnancyProfileId={}",
                userId, pregnancyProfileId);
            throw new ForbiddenException(
                "pregnancyProfileId=" + pregnancyProfileId + " does not belong to userId=" + userId);
        }
        return pregnancy;
    }
}

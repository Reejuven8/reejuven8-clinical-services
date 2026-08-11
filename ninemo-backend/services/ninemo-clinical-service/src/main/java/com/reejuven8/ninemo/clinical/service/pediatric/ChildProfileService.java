package com.reejuven8.ninemo.clinical.service.pediatric;

import com.reejuven8.ninemo.clinical.model.dto.ChildProfileResponse;
import com.reejuven8.ninemo.clinical.model.dto.ChildProfileUpdateRequest;
import com.reejuven8.ninemo.clinical.model.entity.ChildProfile;
import com.reejuven8.ninemo.clinical.repository.ChildProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * NM-B-169 / IS-028 — child profile read + update.
 *
 * Without the list endpoint a childId was obtainable exactly once, inline from the
 * mode-transition response; after a reinstall the whole child-mode UI became unreachable.
 */
@Service
@RequiredArgsConstructor
public class ChildProfileService {

    private final ChildProfileRepository childProfileRepository;
    private final ChildAccessGuard childAccessGuard;

    @Transactional(readOnly = true)
    public List<ChildProfileResponse> listForParent(UUID userId) {
        return childProfileRepository.findByParentUserIdAndIsActiveTrueOrderByDateOfBirthDesc(userId)
            .stream()
            .map(ChildProfileResponse::from)
            .toList();
    }

    @Transactional(readOnly = true)
    public ChildProfileResponse get(UUID childId, UUID userId) {
        return ChildProfileResponse.from(childAccessGuard.requireOwnedChild(childId, userId));
    }

    @Transactional
    public ChildProfileResponse update(UUID childId, UUID userId, ChildProfileUpdateRequest request) {
        ChildProfile child = childAccessGuard.requireOwnedChild(childId, userId);
        applyUpdate(child, request);
        return ChildProfileResponse.from(childProfileRepository.save(child));
    }

    /**
     * Null-safe partial apply — shared with the mode-transition flow, which may carry the same
     * body at creation time. Package-private so ModeTransitionService can reuse it.
     */
    static void applyUpdate(ChildProfile child, ChildProfileUpdateRequest request) {
        if (request == null) return;
        if (request.getChildName() != null) child.setChildName(request.getChildName());
        if (request.getBiologicalSex() != null) child.setBiologicalSex(request.getBiologicalSex());
        if (request.getBirthWeightKg() != null) child.setBirthWeightKg(request.getBirthWeightKg());
        if (request.getBirthHeightCm() != null) child.setBirthHeightCm(request.getBirthHeightCm());
        if (request.getHeadCircumferenceCm() != null) child.setHeadCircumferenceCm(request.getHeadCircumferenceCm());
        if (request.getBloodGroup() != null) child.setBloodGroup(request.getBloodGroup());
        if (request.getDateOfBirth() != null) {
            if (request.getDateOfBirth().isAfter(LocalDate.now())) {
                throw new IllegalArgumentException("dateOfBirth cannot be in the future");
            }
            child.setDateOfBirth(request.getDateOfBirth());
        }
    }
}

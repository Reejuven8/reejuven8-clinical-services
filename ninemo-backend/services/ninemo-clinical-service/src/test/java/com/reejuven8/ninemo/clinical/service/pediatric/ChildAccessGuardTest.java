package com.reejuven8.ninemo.clinical.service.pediatric;

import com.reejuven8.common.exception.ForbiddenException;
import com.reejuven8.common.exception.ResourceNotFoundException;
import com.reejuven8.ninemo.clinical.model.entity.ChildProfile;
import com.reejuven8.ninemo.clinical.model.entity.PregnancyProfile;
import com.reejuven8.ninemo.clinical.repository.ChildProfileRepository;
import com.reejuven8.ninemo.clinical.repository.PregnancyProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/** IS-027 — the regression that let any user read any child by UUID. */
@ExtendWith(MockitoExtension.class)
class ChildAccessGuardTest {

    @Mock ChildProfileRepository childProfileRepository;
    @Mock PregnancyProfileRepository pregnancyProfileRepository;

    ChildAccessGuard guard;

    @BeforeEach
    void setUp() {
        guard = new ChildAccessGuard(childProfileRepository, pregnancyProfileRepository);
    }

    @Test
    void requireOwnedChild_returnsChildForItsOwnParent() {
        UUID parentId = UUID.randomUUID();
        UUID childId = UUID.randomUUID();
        ChildProfile child = ChildProfile.builder().id(childId).parentUserId(parentId).build();
        when(childProfileRepository.findById(childId)).thenReturn(Optional.of(child));

        assertSame(child, guard.requireOwnedChild(childId, parentId));
    }

    @Test
    void requireOwnedChild_rejectsAnotherUsersChild() {
        UUID childId = UUID.randomUUID();
        ChildProfile child = ChildProfile.builder().id(childId).parentUserId(UUID.randomUUID()).build();
        when(childProfileRepository.findById(childId)).thenReturn(Optional.of(child));

        assertThrows(ForbiddenException.class, () -> guard.requireOwnedChild(childId, UUID.randomUUID()));
    }

    @Test
    void requireOwnedChild_unknownChildIsNotFound() {
        UUID childId = UUID.randomUUID();
        when(childProfileRepository.findById(childId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> guard.requireOwnedChild(childId, UUID.randomUUID()));
    }

    @Test
    void requireOwnedPregnancy_rejectsAnotherUsersPregnancy() {
        UUID pregnancyId = UUID.randomUUID();
        PregnancyProfile pregnancy = PregnancyProfile.builder().id(pregnancyId).userId(UUID.randomUUID()).build();
        when(pregnancyProfileRepository.findById(pregnancyId)).thenReturn(Optional.of(pregnancy));

        assertThrows(ForbiddenException.class,
            () -> guard.requireOwnedPregnancy(pregnancyId, UUID.randomUUID()));
    }

    @Test
    void requireOwnedPregnancy_returnsPregnancyForItsOwner() {
        UUID userId = UUID.randomUUID();
        UUID pregnancyId = UUID.randomUUID();
        PregnancyProfile pregnancy = PregnancyProfile.builder().id(pregnancyId).userId(userId).build();
        when(pregnancyProfileRepository.findById(pregnancyId)).thenReturn(Optional.of(pregnancy));

        assertSame(pregnancy, guard.requireOwnedPregnancy(pregnancyId, userId));
    }
}

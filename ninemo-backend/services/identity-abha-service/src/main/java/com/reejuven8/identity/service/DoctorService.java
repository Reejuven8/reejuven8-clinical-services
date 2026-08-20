package com.reejuven8.identity.service;

import com.reejuven8.common.exception.ResourceNotFoundException;
import com.reejuven8.identity.model.dto.DoctorSummaryResponse;
import com.reejuven8.identity.model.entity.DoctorProfile;
import com.reejuven8.identity.model.entity.User;
import com.reejuven8.identity.model.enums.UserRole;
import com.reejuven8.identity.repository.DoctorProfileRepository;
import com.reejuven8.identity.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DoctorService {

    private final UserRepository userRepository;
    private final DoctorProfileRepository doctorProfileRepository;

    /** Resolves a doctor by phone number, so a patient can grant consent without knowing a raw UUID. */
    public DoctorSummaryResponse searchByPhone(String phoneNumber) {
        User user = userRepository.findByPhoneNumber(phoneNumber)
            .filter(u -> u.getRole() == UserRole.DOCTOR)
            .orElseThrow(() -> new ResourceNotFoundException("Doctor", phoneNumber));

        DoctorProfile profile = doctorProfileRepository.findByUserId(user.getId())
            .orElseThrow(() -> new ResourceNotFoundException("DoctorProfile", user.getId().toString()));

        return new DoctorSummaryResponse(
            user.getId(),
            user.getFirstName(),
            user.getLastName(),
            profile.getSpecialization(),
            profile.getQualifications()
        );
    }
}

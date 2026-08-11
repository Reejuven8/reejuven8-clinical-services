package com.reejuven8.identity.service;

import com.reejuven8.common.exception.ResourceNotFoundException;
import com.reejuven8.common.exception.UnauthorizedException;
import com.reejuven8.identity.model.dto.OtpRequest;
import com.reejuven8.identity.model.dto.RegisterRequest;
import com.reejuven8.identity.model.dto.TokenResponse;
import com.reejuven8.identity.model.entity.PatientProfile;
import com.reejuven8.identity.model.entity.User;
import com.reejuven8.identity.model.enums.UserRole;
import com.reejuven8.identity.repository.PatientProfileRepository;
import com.reejuven8.identity.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PatientProfileRepository patientProfileRepository;
    private final OtpService otpService;
    private final TokenService tokenService;

    @Transactional
    public void sendOtp(OtpRequest request) {
        otpService.generateAndStore(request.phoneNumber());
    }

    @Transactional
    public TokenResponse register(RegisterRequest request) {
        if (userRepository.existsByPhoneNumber(request.phoneNumber())) {
            throw new UnauthorizedException("Phone number already registered");
        }
        User user = User.builder()
            .firstName(request.firstName())
            .middleName(request.middleName())
            .lastName(request.lastName())
            .phoneNumber(request.phoneNumber())
            .role(request.role())
            .build();
        user = userRepository.save(user);

        if (request.role() == UserRole.PATIENT) {
            PatientProfile profile = PatientProfile.builder()
                .user(user)
                .dateOfBirth(request.dateOfBirth())
                .biologicalSex(request.biologicalSex())
                .build();
            patientProfileRepository.save(profile);
        }

        otpService.generateAndStore(request.phoneNumber());
        return tokenService.issueTokenPair(user);
    }

    @Transactional
    public TokenResponse loginWithOtp(String phoneNumber, String otp) {
        otpService.verifyAndConsume(phoneNumber, otp);
        User user = userRepository.findByPhoneNumber(phoneNumber)
            .orElseThrow(() -> new ResourceNotFoundException("User", phoneNumber));
        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new UnauthorizedException("Account is disabled");
        }
        return tokenService.issueTokenPair(user);
    }

    public TokenResponse refresh(String bearerToken) {
        String refreshToken = stripBearer(bearerToken);
        return tokenService.refresh(refreshToken);
    }

    public void logout(String bearerToken) {
        String accessToken = stripBearer(bearerToken);
        tokenService.logout(accessToken);
    }

    private String stripBearer(String header) {
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        throw new UnauthorizedException("Authorization header missing or malformed");
    }
}

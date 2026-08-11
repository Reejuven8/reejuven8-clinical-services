package com.reejuven8.ninemo.community.service;

import com.reejuven8.ninemo.community.model.DueDateClub;
import com.reejuven8.ninemo.community.model.dto.ClubResponse;
import com.reejuven8.ninemo.community.repository.DueDateClubRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DueDateClubService {

    private final DueDateClubRepository clubRepository;

    public DueDateClub join(String userId, String dueDateMonth, String alias) {
        DueDateClub club = clubRepository.findByDueDateMonth(dueDateMonth)
            .orElseGet(() -> clubRepository.save(newClub(dueDateMonth)));

        boolean alreadyMember = club.getMembers().stream()
            .anyMatch(m -> m.getUserId().equals(userId));

        if (alreadyMember) return club;

        List<DueDateClub.Member> members = new ArrayList<>(club.getMembers());
        members.add(DueDateClub.Member.builder().userId(userId).alias(alias).build());
        club.setMembers(members);
        club.setMemberCount(members.size());
        return clubRepository.save(club);
    }

    public List<ClubResponse> listActive(String requestingUserId) {
        return clubRepository.findAll().stream()
            .filter(c -> Boolean.TRUE.equals(c.getIsActive()))
            .map(c -> toResponse(c, requestingUserId))
            .collect(Collectors.toList());
    }

    public List<DueDateClub.Channel> getChannels(String clubId) {
        return clubRepository.findById(clubId)
            .map(DueDateClub::getChannels)
            .orElseThrow(() -> new IllegalArgumentException("Club not found: " + clubId));
    }

    public ClubResponse getClub(String clubId, String requestingUserId) {
        DueDateClub club = clubRepository.findById(clubId)
            .orElseThrow(() -> new IllegalArgumentException("Club not found: " + clubId));
        return toResponse(club, requestingUserId);
    }

    private DueDateClub newClub(String dueDateMonth) {
        return DueDateClub.builder()
            .clubName("Due Date Club — " + dueDateMonth)
            .dueDateMonth(dueDateMonth)
            .members(new ArrayList<>())
            .channels(List.of(
                DueDateClub.Channel.builder()
                    .channelId(UUID.randomUUID().toString())
                    .name("General").description("General discussion").isDefault(true).build(),
                DueDateClub.Channel.builder()
                    .channelId(UUID.randomUUID().toString())
                    .name("Questions").description("Ask anything").build(),
                DueDateClub.Channel.builder()
                    .channelId(UUID.randomUUID().toString())
                    .name("Milestones").description("Share your milestones").build()
            ))
            .build();
    }

    private ClubResponse toResponse(DueDateClub club, String userId) {
        boolean isMember = club.getMembers().stream()
            .anyMatch(m -> m.getUserId().equals(userId));
        return ClubResponse.builder()
            .id(club.getId())
            .clubName(club.getClubName())
            .dueDateMonth(club.getDueDateMonth())
            .memberCount(club.getMemberCount() != null ? club.getMemberCount() : 0)
            .isMember(isMember)
            .channels(club.getChannels().stream()
                .map(ch -> ClubResponse.ChannelDto.builder()
                    .channelId(ch.getChannelId())
                    .name(ch.getName())
                    .description(ch.getDescription())
                    .isDefault(Boolean.TRUE.equals(ch.getIsDefault()))
                    .build())
                .collect(Collectors.toList()))
            .build();
    }
}

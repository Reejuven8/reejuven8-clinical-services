package com.reejuven8.ninemo.community.model.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data @Builder
public class ClubResponse {
    private String id;
    private String clubName;
    private String dueDateMonth;
    private int memberCount;
    private boolean isMember;
    private String callerAlias;
    private List<ChannelDto> channels;

    @Data @Builder
    public static class ChannelDto {
        private String channelId;
        private String name;
        private String description;
        private boolean isDefault;
    }
}

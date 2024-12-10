package com.mulmeong.event.contest.produce;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ContestVoteUpdateEvent {

    private Long contestId;
    private String postUuid;
    private Integer count;

    public static ContestVoteUpdateEvent toDto(
            Long contestId, String postUuid, Integer count) {
        return ContestVoteUpdateEvent.builder()
                .contestId(contestId)
                .postUuid(postUuid)
                .count(count)
                .build();
    }

}

package com.mulmeong.event.contest.produce;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ContestVoteRecordEvent {

    private Long contestId;
    private String memberUuid;
    private String postUuid;

    public static ContestVoteRecordEvent toDto(Long contestId, String memberUuid, String postUuid) {
        return ContestVoteRecordEvent.builder()
                .contestId(contestId)
                .memberUuid(memberUuid)
                .postUuid(postUuid)
                .build();
    }
}

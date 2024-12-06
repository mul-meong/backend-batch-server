package com.mulmeong.event.contest.produce;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ContestStatusEvent {

    private Long contestId;

    public static ContestStatusEvent toDto(Long contestId) {
        return ContestStatusEvent.builder()
                .contestId(contestId)
                .build();
    }
}

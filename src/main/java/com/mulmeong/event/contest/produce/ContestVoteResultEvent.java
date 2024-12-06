package com.mulmeong.event.contest.produce;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ContestVoteResultEvent {

    private Long contestId;
    private String memberUuid;
    private String postUuid;
    private Long badgeId;
    private Long voteCount;
    private Byte ranking;

    public static ContestVoteResultEvent toDto(
            Long contestId, String memberUuid, String postUuid, Long badgeId, Long voteCount, Byte ranking) {
        return ContestVoteResultEvent.builder()
                .contestId(contestId)
                .memberUuid(memberUuid)
                .postUuid(postUuid)
                .badgeId(badgeId)
                .voteCount(voteCount)
                .ranking(ranking)
                .build();
    }

}

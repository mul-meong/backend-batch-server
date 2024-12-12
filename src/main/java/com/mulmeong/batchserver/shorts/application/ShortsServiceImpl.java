package com.mulmeong.batchserver.shorts.application;

import com.mulmeong.batchserver.shorts.domain.document.ShortsRead;
import com.mulmeong.batchserver.shorts.infrastructure.repository.ShortsReadRepository;
import com.mulmeong.batchserver.utility.infrastructure.repository.DislikesRepository;
import com.mulmeong.batchserver.utility.infrastructure.repository.LikesRepository;
import com.mulmeong.event.utility.consume.DislikesCreateEvent;
import com.mulmeong.event.utility.consume.LikesCreateEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class ShortsServiceImpl implements ShortsService {

    private final ShortsReadRepository shortsReadRepository;
    private final LikesRepository likesRepository;
    private final DislikesRepository dislikesRepository;

    @Override
    public void likeCountRenew(LikesCreateEvent message) {

        ShortsRead shortsReadUpdate = shortsReadRepository.findByShortsUuid(message.getKindUuid()).orElseThrow();
        Long count = likesRepository.countByKindAndKindUuidAndStatus(message.getKind(), message.getKindUuid(), true);
        log.info("count: {}", count);
        shortsReadRepository.save(message.toShortsReadEntity(shortsReadUpdate, count));

    }

    @Override
    public void dislikeCountRenew(DislikesCreateEvent message) {
        ShortsRead shortsReadUpdate = shortsReadRepository.findByShortsUuid(message.getKindUuid()).orElseThrow();
        Long count = dislikesRepository.countByKindAndKindUuidAndStatus(message.getKind(), message.getKindUuid(), true);
        log.info("count: {}", count);
        shortsReadRepository.save(message.toShortsReadEntity(shortsReadUpdate, count));
    }

}
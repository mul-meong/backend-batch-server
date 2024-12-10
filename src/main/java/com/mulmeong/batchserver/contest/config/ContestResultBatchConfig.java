package com.mulmeong.batchserver.contest.config;

import com.mulmeong.batchserver.application.EventPublisher;
import com.mulmeong.batchserver.contest.entity.contest.Contest;
import com.mulmeong.batchserver.contest.entity.contest.ContestPost;
import com.mulmeong.batchserver.contest.entity.contestRead.ContestPostRead;
import com.mulmeong.batchserver.contest.infrastructure.repository.ContestPostRepository;
import com.mulmeong.batchserver.contest.infrastructure.repository.ContestPostReadRepository;
import com.mulmeong.batchserver.contest.infrastructure.repository.ContestRepository;
import com.mulmeong.event.contest.produce.ContestStatusEvent;
import com.mulmeong.event.contest.produce.ContestVoteRecordEvent;
import com.mulmeong.event.contest.produce.ContestVoteResultEvent;
import com.mulmeong.event.contest.produce.ContestVoteUpdateEvent;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.*;
import java.util.stream.Collectors;


@Configuration
@EnableScheduling
@EnableBatchProcessing(dataSourceRef = "contestDataSource", transactionManagerRef = "contestTransactionManager")
@RequiredArgsConstructor
public class ContestResultBatchConfig {

    private static final Logger log = LoggerFactory.getLogger(ContestBatchConfig.class);
    private final ContestRepository contestRepository;
    private final ContestPostRepository contestPostRepository;
    private final ContestPostReadRepository contestPostReadRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final PlatformTransactionManager transactionManager;
    private final EventPublisher eventPublisher;
    private static final String VOTE_COUNT_KEY = "contest:%d:post:votes";
    private static final String VOTER_SET_KEY = "contest:%d:post:%s:voters";
    private final JobRepository jobRepository;
    private final JobLauncher jobLauncher;


    @Scheduled(cron = "0 0 0 * * MON") // 매주 월요일 자정에 실행
    public void runJob() throws Exception {
        JobParameters jobParameters = new JobParametersBuilder()
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();
        log.info("Running job {}", jobParameters);

        jobLauncher.run(voteResultJob(), jobParameters);
    }

    @Bean
    public Job voteResultJob() {
        return new JobBuilder("voteResult", jobRepository)
                .start(lastRenewStep())
                .next(voteResultStep())
                .next(topVoteRankingStep())
                .next(updateContestStatusStep())
                .build();
    }

    @Bean
    public Step lastRenewStep() {
        return new StepBuilder("lastRenewStep", jobRepository)
                .<Contest, List<ContestVoteUpdateEvent>>chunk(10, transactionManager)
                .reader(contestOnReader())
                .processor(lastRenewProcessor())
                .writer(lastRenewWriter())
                .build();
    }

    @Bean
    @StepScope
    public ItemReader<Contest> contestOnReader() {
        return new ItemReader<>() {
            private final Iterator<Contest> iterator = contestRepository
                    .findByStatusTrue()
                    .iterator();


            @Override
            public Contest read() {
                if (iterator.hasNext()) {
                    Contest contest = iterator.next();
                    log.info("Read contest: {}", contest.getName());
                    return contest;
                } else {
                    return null;
                }
            }
        };
    }

    @Bean
    public ItemProcessor<Contest, List<ContestVoteUpdateEvent>> lastRenewProcessor() {
        return contest -> {
            Long contestId = contest.getId();
            log.info("contestId: {}", contestId);
            Set<String> voteCountKeys = redisTemplate.keys(String.format(VOTE_COUNT_KEY, contestId));

            if (voteCountKeys != null) {
                return voteCountKeys.stream()
                        .flatMap(voteCountKey -> {
                            Set<ZSetOperations.TypedTuple<String>> voteData = redisTemplate
                                    .opsForZSet().rangeWithScores(voteCountKey, 0, -1);
                            return voteData != null ? voteData.stream() : null;
                        })
                        .filter(Objects::nonNull)
                        .map(entry -> ContestVoteUpdateEvent.toDto(
                                contestId, entry.getValue(), Objects.requireNonNull(entry.getScore()).intValue()))
                        .collect(Collectors.toList());
            }
            return null;
        };
    }

    @Bean
    public ItemWriter<List<ContestVoteUpdateEvent>> lastRenewWriter() {
        return items -> {
            for (List<ContestVoteUpdateEvent> eventList : items) {
                for (ContestVoteUpdateEvent event : eventList) {
                    // 이벤트 발행
                    log.info("1st send: {}", event);
                    eventPublisher.send(event);

                    // Redis에서 해당 포스트의 데이터 삭제
                    Long contestId = event.getContestId();
                    String voteCountKey = String.format(VOTE_COUNT_KEY, contestId);
                    redisTemplate.opsForZSet().remove(voteCountKey, event.getPostUuid());
                }
            }
        };
    }

    @Bean
    public Step voteResultStep() {
        return new StepBuilder("voteResultStep", jobRepository)
                .<Contest, List<ContestVoteRecordEvent>>chunk(10, transactionManager)
                .reader(contestOnReader())
                .processor(contestRecordProcessor())
                .writer(contestRecordWriter())
                .build();
    }

    @Bean
    public ItemProcessor<Contest, List<ContestVoteRecordEvent>> contestRecordProcessor() {
        return contest -> {
            Long contestId = contest.getId();
            List<ContestPost> posts = contestPostRepository.getAllByContestId(contestId);
            List<ContestVoteRecordEvent> events = new ArrayList<>();

            for (ContestPost post : posts) {
                String postUuid = post.getPostUuid(); // ContestPost에서 postUuid 가져오기

                // Redis에서 해당 포스트에 투표한 사람들을 조회
                Set<String> voters = redisTemplate.opsForSet()
                        .members(String.format(VOTER_SET_KEY, contestId, postUuid));


                if (voters != null) {
                    voters.stream()
                            .filter(Objects::nonNull)
                            .forEach(voter -> {
                                events.add(ContestVoteRecordEvent.toDto(contestId, voter, postUuid));
                            });
                }
            }
            return events.isEmpty() ? null : events;
        };
    }

    @Bean
    public ItemWriter<List<ContestVoteRecordEvent>> contestRecordWriter() {
        return items -> {
            for (List<ContestVoteRecordEvent> eventList : items) {
                for (ContestVoteRecordEvent event : eventList) {
                    // 이벤트 발행
                    log.info("2nd send: {}", event);
                    eventPublisher.send(event);

                    // Redis에서 해당 포스트의 데이터 삭제
                    Long contestId = event.getContestId();
                    String postUuid = event.getPostUuid();
                    String voteCountKey = String.format(VOTER_SET_KEY, contestId, postUuid);
                    redisTemplate.opsForSet().remove(voteCountKey, event.getMemberUuid());
                }
            }
        };
    }

    @Bean
    public Step topVoteRankingStep() {
        return new StepBuilder("topVoteRankingStep", jobRepository)
                .<Contest, List<ContestVoteResultEvent>>chunk(10, transactionManager)
                .reader(contestOnReader())
                .processor(topVoteRankingProcessor())
                .writer(topVoteRankingWriter())
                .build();
    }

    @Bean
    public ItemProcessor<Contest, List<ContestVoteResultEvent>> topVoteRankingProcessor() {
        return contest -> {
            Long contestId = contest.getId();
            // 해당 Contest의 postUuid와 voteCount 정보를 가져옴
            List<ContestPostRead> contestPostReads = contestPostReadRepository
                    .findByContestId(contestId);

            // 투표수 기준으로 내림차순 정렬하고, 상위 3개 포스트만 가져옴
            List<ContestPostRead> topPosts = contestPostReads.stream()
                    .sorted(Comparator.comparingLong(ContestPostRead::getVoteCount).reversed())
                    .limit(3)
                    .toList();

            // 상위 3개의 포스트에 대해 1, 2, 3위 등수를 부여
            Byte ranking = 1;
            List<ContestVoteResultEvent> resultEvents = new ArrayList<>();
            for (ContestPostRead post : topPosts) {
                resultEvents.add(ContestVoteResultEvent.toDto(contestId,
                        post.getMemberUuid(),
                        post.getPostUuid(),
                        contest.getBadgeId(),
                        post.getVoteCount(),
                        ranking));
                ranking++;
            }

            log.info("result: {}", topPosts);

            // 결과 이벤트 반환
            return resultEvents;
        };
    }

    @Bean
    public ItemWriter<List<ContestVoteResultEvent>> topVoteRankingWriter() {
        return items -> {
            for (List<ContestVoteResultEvent> eventList : items) {
                for (ContestVoteResultEvent event : eventList) {
                    // 이벤트 발행
                    log.info("3rd send: {}", event);
                    eventPublisher.send(event);
                }
            }
        };
    }

    @Bean
    public Step updateContestStatusStep() {
        return new StepBuilder("updateContestStatusStep", jobRepository)
                .<Contest, List<ContestStatusEvent>>chunk(10, transactionManager)
                .reader(contestOnReader())
                .processor(updateContestStatusProcessor())
                .writer(contestStatusAlter())
                .build();
    }

    @Bean
    public ItemProcessor<Contest, List<ContestStatusEvent>> updateContestStatusProcessor() {
        return contest -> {
            List<ContestStatusEvent> events = new ArrayList<>();

            events.add(ContestStatusEvent.toDto(contest.getId()));

            return events;
        };
    }

    @Bean
    public ItemWriter<List<ContestStatusEvent>> contestStatusAlter() {
        return items -> {
            for (List<ContestStatusEvent> eventList : items) {
                for (ContestStatusEvent event : eventList) {
                    // 이벤트 발행
                    log.info("4th send: {}", event);
                    eventPublisher.send(event);
                }
            }
        };
    }
}

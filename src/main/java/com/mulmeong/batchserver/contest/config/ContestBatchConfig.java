package com.mulmeong.batchserver.contest.config;

import com.mulmeong.batchserver.application.EventPublisher;
import com.mulmeong.batchserver.contest.entity.contest.Contest;
import com.mulmeong.batchserver.contest.infrastructure.repository.ContestRepository;
import com.mulmeong.event.contest.produce.ContestVoteUpdateEvent;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.*;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
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
public class ContestBatchConfig {
    private static final Logger log = LoggerFactory.getLogger(ContestBatchConfig.class);
    private final ContestRepository contestRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final PlatformTransactionManager transactionManager;
    private final EventPublisher eventPublisher;
    private static final String VOTE_COUNT_KEY = "contest:%d:post:votes";
    private final JobRepository jobRepository;
    private final JobLauncher jobLauncher;


    @Scheduled(fixedRate = 300000)
    public void runRenewJob() throws Exception {
        JobParameters jobParameters = new JobParametersBuilder()
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();
        log.info("Running job {}", jobParameters);

        jobLauncher.run(voteRenewJob(), jobParameters);
    }

    @Bean
    public Job voteRenewJob() {
        return new JobBuilder("voteRenew", jobRepository)
                .start(voteRenewStep())
                .build();
    }

    @Bean
    public Step voteRenewStep() {
        return new StepBuilder("voteRenewStep", jobRepository)
                .<Contest, List<ContestVoteUpdateEvent>>chunk(10, transactionManager)
                .reader(contestReader())
                .processor(contestProcessor())
                .writer(contestWriter())
                .build();
    }

    @Bean
    @StepScope
    public ItemReader<Contest> contestReader() {
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
    public ItemProcessor<Contest, List<ContestVoteUpdateEvent>> contestProcessor() {
        return contest -> {
            Long contestId = contest.getId();
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
    public ItemWriter<List<ContestVoteUpdateEvent>> contestWriter() {
        return items -> {
            for (List<ContestVoteUpdateEvent> eventList : items) {
                for (ContestVoteUpdateEvent event : eventList) {
                    // 이벤트 발행
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
    public StepExecutionListener loggingStepExecutionListener() {
        return new StepExecutionListener() {
            @Override
            public void beforeStep(StepExecution stepExecution) {
                log.info("Starting step: {}", stepExecution.getStepName());
            }

            @Override
            public ExitStatus afterStep(StepExecution stepExecution) {
                log.info("Completed step: {} with status: {}", stepExecution.getStepName(), stepExecution.getStatus());
                return stepExecution.getExitStatus();
            }
        };
    }
}
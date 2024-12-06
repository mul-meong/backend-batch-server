package com.mulmeong.batchserver.application;

import com.mulmeong.event.contest.produce.ContestStatusEvent;
import com.mulmeong.event.contest.produce.ContestVoteRecordEvent;
import com.mulmeong.event.contest.produce.ContestVoteResultEvent;
import com.mulmeong.event.contest.produce.ContestVoteUpdateEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class EventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${event.contest.pub.topics.contest-vote-update.name}")
    private String contestVoteUpdateEventTopic;
    @Value("${event.contest.pub.topics.contest-vote-record.name}")
    private String contestVoteRecordEventTopic;
    @Value("${event.contest.pub.topics.contest-result.name}")
    private String contestVoteResultEventTopic;
    @Value("${event.contest.pub.topics.contest-status-alter.name}")
    private String contestStatusAlterEventTopic;


    public void send(String topic, Object event) {
        log.info("Publishing event: {}", event);
        kafkaTemplate.send(topic, event);
    }

    public void send(ContestVoteUpdateEvent event) {
        kafkaTemplate.send(contestVoteUpdateEventTopic, event);
        log.info("vote update topic: {}", contestVoteUpdateEventTopic);
    }

    public void send(ContestVoteRecordEvent event) {
        kafkaTemplate.send(contestVoteRecordEventTopic, event);
        log.info("vote record topic: {}", contestVoteRecordEventTopic);
    }

    public void send(ContestVoteResultEvent event) {
        kafkaTemplate.send(contestVoteResultEventTopic, event);
        log.info("vote result topic: {}", contestVoteResultEventTopic);
    }

    public void send(ContestStatusEvent event) {
        kafkaTemplate.send(contestStatusAlterEventTopic, event);
        log.info("contest status topic: {}", contestStatusAlterEventTopic);
    }
}

package com.project.cadence.producers;

import com.project.cadence.dto.Topics;
import com.project.cadence.events.StreamSongEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class StreamSongProducer {
    private final KafkaTemplate<String, StreamSongEvent> kafkaTemplate;

    public void send(StreamSongEvent event) {
        log.info("Attempting to send message to topic: {}, event: {}", Topics.STREAM_SONG_TOPIC, event);
        kafkaTemplate.send(Topics.STREAM_SONG_TOPIC, event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("Message sent successfully to topic: {}, event: {}", Topics.STREAM_SONG_TOPIC, event);
                    } else {
                        log.error("Failed to send message to topic: {}, event: {}", Topics.STREAM_SONG_TOPIC, event, ex);
                    }
                });
    }
}

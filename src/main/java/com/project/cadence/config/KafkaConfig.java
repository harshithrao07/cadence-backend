package com.project.cadence.config;

import com.project.cadence.dto.Topics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
@EnableKafka
public class KafkaConfig {

    @Bean
    public NewTopic recordCreatedTopic() {
        return TopicBuilder.name(Topics.RECORD_CREATED_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic streamSongTopic() {
        return TopicBuilder.name(Topics.STREAM_SONG_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic userCreatedTopic() {
        return TopicBuilder.name(Topics.USER_CREATED_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }
}

package com.saurabh.notification.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Value("${notification.kafka.topic}")
    private String topicName;

    @Value("${notification.kafka.partitions:10}")
    private int partitions;

    @Value("${notification.kafka.replicas:1}")
    private int replicas;

    @Bean
    public NewTopic notificationShardsTopic() {
        return TopicBuilder.name(topicName)
                .partitions(partitions)
                .replicas(replicas)
                .build();
    }
}

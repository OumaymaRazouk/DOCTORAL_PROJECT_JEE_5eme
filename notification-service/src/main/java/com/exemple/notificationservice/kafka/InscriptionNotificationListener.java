package com.exemple.notificationservice.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class InscriptionNotificationListener {

    private static final Logger log = LoggerFactory.getLogger(InscriptionNotificationListener.class);

    @KafkaListener(topics = "${app.notifications.topic:inscription-events}", groupId = "notification-service")
    public void onMessage(ConsumerRecord<String, String> record) {
        log.info("Notification recue depuis Kafka: topic={}, key={}, payload={}",
                record.topic(), record.key(), record.value());
        // TODO: brancher ici l'envoi d'email / SMS / notifications temps reel
    }
}

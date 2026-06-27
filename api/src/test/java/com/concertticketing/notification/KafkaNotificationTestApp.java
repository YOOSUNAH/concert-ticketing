package com.concertticketing.notification;

import com.concertticketing.domain.notification.ProcessedEventGuard;
import com.concertticketing.domain.notification.sender.NotificationSender;
import com.concertticketing.domain.notification.service.NotificationService;
import com.concertticketing.infra.notification.CaffeineProcessedEventGuard;
import com.concertticketing.infra.notification.NotificationConsumer;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.kafka.autoconfigure.KafkaAutoConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * Kafka 알림 흐름만 떼어 띄우는 최소 테스트 컨텍스트.
 *
 * <p>DB/Redis 등은 빼고 {@link KafkaAutoConfiguration}만 가져와(@ImportAutoConfiguration)
 * 발행→소비→재시도→DLT 경로를 {@code @EmbeddedKafka}(인메모리 브로커)로 검증한다. Docker 불필요.
 *
 * <p>알림 sender만 테스트용({@link RecordingNotificationSender})으로 바꾸고, 나머지
 * (Consumer/Service/멱등 가드)는 운영과 동일한 실제 구현을 쓴다.
 */
@SpringBootConfiguration
@ImportAutoConfiguration(KafkaAutoConfiguration.class)
public class KafkaNotificationTestApp {

    @Bean
    public RecordingNotificationSender recordingNotificationSender() {
        return new RecordingNotificationSender();
    }

    @Bean
    public NotificationService notificationService(NotificationSender sender) {
        return new NotificationService(sender);
    }

    @Bean
    public ProcessedEventGuard processedEventGuard() {
        return new CaffeineProcessedEventGuard();
    }

    @Bean
    public NotificationConsumer notificationConsumer(NotificationService notificationService,
                                                     ProcessedEventGuard processedEventGuard) {
        return new NotificationConsumer(notificationService, processedEventGuard);
    }
}

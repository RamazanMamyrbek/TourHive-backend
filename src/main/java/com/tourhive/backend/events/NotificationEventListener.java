package com.tourhive.backend.events;

import com.tourhive.backend.services.notifications.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationService notificationService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onNotificationEvent(NotificationEvent event) {
        log.debug("Received NotificationEvent AFTER_COMMIT: recipient={}, type={}", event.recipientUserId(), event.type());
        notificationService.handleNotificationEvent(event);
    }
}

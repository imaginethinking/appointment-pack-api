package net.imaginethinking.appointmentpack.event;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * Publishes application events so audit, analytics and notification work can be handled separately.
 */
@Component
@RequiredArgsConstructor
public class AppEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    /**
     * Publishes the application events event for the completed change.
     */
    public void publish(AppEvent event) {
        applicationEventPublisher.publishEvent(Objects.requireNonNull(event, "Event must not be null"));
    }
}
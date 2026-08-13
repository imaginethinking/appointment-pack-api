package net.imaginethinking.appointmentpack.event;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
@RequiredArgsConstructor
public class AppEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    public void publish(AppEvent event) {
        applicationEventPublisher.publishEvent(Objects.requireNonNull(event, "Event must not be null"));
    }
}
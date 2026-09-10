package net.imaginethinking.appointmentpack.analytics;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Stores operational analytics events in their own transaction.
 */
@Service
@RequiredArgsConstructor
public class OperationalEventRecorder {

    private final OperationalEventRepository operationalEventRepository;

    /**
     * Skips source events that have already been recorded and saves new events in a separate transaction.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(OperationalEvent event) {
        if (operationalEventRepository.existsBySourceEventId(event.getSourceEventId())) {
            return;
        }

        operationalEventRepository.saveAndFlush(event);
    }
}
package net.imaginethinking.appointmentpack.analytics;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Saves operational analytics events without allowing an analytics failure to break the original user action.
 */
@Service
@RequiredArgsConstructor
public class OperationalEventRecorder {

    private final OperationalEventRepository operationalEventRepository;

    /**
     * Saves the operational event when it has not already been recorded and ignores analytics failures.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(OperationalEvent event) {
        if (operationalEventRepository.existsBySourceEventId(event.getSourceEventId())) {
            return;
        }

        operationalEventRepository.saveAndFlush(event);
    }
}
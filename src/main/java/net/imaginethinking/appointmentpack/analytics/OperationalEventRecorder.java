package net.imaginethinking.appointmentpack.analytics;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OperationalEventRecorder {

    private final OperationalEventRepository operationalEventRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(OperationalEvent event) {
        if (operationalEventRepository.existsBySourceEventId(event.getSourceEventId())) {
            return;
        }

        operationalEventRepository.saveAndFlush(event);
    }
}
package net.imaginethinking.appointmentpack.analytics;

import net.imaginethinking.appointmentpack.event.analytics.ApplicationPage;
import net.imaginethinking.appointmentpack.event.analytics.PageViewedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OperationalEventRecorderTest {

    @Mock
    private OperationalEventRepository operationalEventRepository;

    private OperationalEventRecorder recorder;

    private final OperationalEventFactory factory = new OperationalEventFactory();

    @BeforeEach
    void setUp() {
        recorder = new OperationalEventRecorder(operationalEventRepository);
    }

    @Test
    void shouldPersistNewOperationalEvent() {
        OperationalEvent event = factory.from(PageViewedEvent.create(UUID.randomUUID(), ApplicationPage.DASHBOARD));

        when(operationalEventRepository.existsBySourceEventId(event.getSourceEventId())).thenReturn(false);

        recorder.record(event);

        verify(operationalEventRepository).saveAndFlush(event);
    }

    @Test
    void shouldIgnoreDuplicateSourceEvent() {
        OperationalEvent event = factory.from(PageViewedEvent.create(UUID.randomUUID(), ApplicationPage.DASHBOARD));

        when(operationalEventRepository.existsBySourceEventId(event.getSourceEventId())).thenReturn(true);

        recorder.record(event);

        verify(operationalEventRepository, never()).saveAndFlush(event);
    }
}
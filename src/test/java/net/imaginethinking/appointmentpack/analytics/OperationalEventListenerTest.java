package net.imaginethinking.appointmentpack.analytics;

import net.imaginethinking.appointmentpack.event.analytics.ApplicationPage;
import net.imaginethinking.appointmentpack.event.analytics.PageViewedEvent;
import net.imaginethinking.appointmentpack.event.auth.AuthenticationAction;
import net.imaginethinking.appointmentpack.event.auth.AuthenticationEvent;
import net.imaginethinking.appointmentpack.event.auth.AuthenticationOutcome;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

/**
 * Checks the operational event listener behaviour covered by this test class.
 */
@ExtendWith(MockitoExtension.class)
class OperationalEventListenerTest {

    @Mock
    private OperationalEventFactory operationalEventFactory;

    @Mock
    private OperationalEventRecorder operationalEventRecorder;

    private OperationalEventListener listener;

    /**
     * Creates the common fixtures and mocks used by each test.
     */
    @BeforeEach
    void setUp() {
        listener = new OperationalEventListener(operationalEventFactory, operationalEventRecorder);
    }

    @Test
    void shouldConvertAndRecordApplicationEvent() {
        PageViewedEvent source = PageViewedEvent.create(UUID.randomUUID(), ApplicationPage.APPOINTMENTS);

        OperationalEvent operationalEvent = new OperationalEventFactory().from(source);

        when(operationalEventFactory.from(source)).thenReturn(operationalEvent);

        listener.handle(source);

        verify(operationalEventRecorder).record(operationalEvent);
    }

    @Test
    void shouldNotPropagateAnalyticsPersistenceFailure() {
        AuthenticationEvent source = AuthenticationEvent.create(
                UUID.randomUUID(),
                AuthenticationAction.LOGIN,
                AuthenticationOutcome.SUCCEEDED);

        OperationalEvent operationalEvent = new OperationalEventFactory().from(source);

        when(operationalEventFactory.from(source)).thenReturn(operationalEvent);

        doThrow(new RuntimeException("database unavailable")).when(operationalEventRecorder).record(operationalEvent);

        assertDoesNotThrow(() -> listener.handle(source));
    }
}
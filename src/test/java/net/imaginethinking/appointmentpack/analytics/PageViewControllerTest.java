package net.imaginethinking.appointmentpack.analytics;

import net.imaginethinking.appointmentpack.event.AppEvent;
import net.imaginethinking.appointmentpack.event.AppEventPublisher;
import net.imaginethinking.appointmentpack.event.analytics.ApplicationPage;
import net.imaginethinking.appointmentpack.event.analytics.PageViewedEvent;
import net.imaginethinking.appointmentpack.security.AuthenticatedUserIdResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PageViewControllerTest {

    @Mock
    private AppEventPublisher appEventPublisher;

    @Mock
    private AuthenticatedUserIdResolver authenticatedUserIdResolver;

    @Mock
    private Jwt jwt;

    private PageViewController controller;

    @BeforeEach
    void setUp() {
        controller = new PageViewController(appEventPublisher, authenticatedUserIdResolver);
    }

    @Test
    void shouldPublishPageViewForAuthenticatedUser() {
        UUID userId = UUID.randomUUID();

        when(authenticatedUserIdResolver.resolve(jwt)).thenReturn(userId);

        ResponseEntity<Void> response = controller.recordPageView(
                jwt,
                new PageViewRequest(ApplicationPage.APPOINTMENT_PACKS));

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());

        ArgumentCaptor<AppEvent> eventCaptor = ArgumentCaptor.forClass(AppEvent.class);

        verify(appEventPublisher).publish(eventCaptor.capture());

        PageViewedEvent event = (PageViewedEvent) eventCaptor.getValue();

        assertEquals(userId, event.actorUserId());
        assertEquals(ApplicationPage.APPOINTMENT_PACKS, event.page());
    }
}
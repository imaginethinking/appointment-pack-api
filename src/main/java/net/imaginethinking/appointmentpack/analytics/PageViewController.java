package net.imaginethinking.appointmentpack.analytics;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.imaginethinking.appointmentpack.event.AppEventPublisher;
import net.imaginethinking.appointmentpack.event.analytics.ApplicationPage;
import net.imaginethinking.appointmentpack.event.analytics.PageViewedEvent;
import net.imaginethinking.appointmentpack.security.AuthenticatedUserIdResolver;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/analytics/page-views")
@RequiredArgsConstructor
public class PageViewController {

    private final AppEventPublisher appEventPublisher;
    private final AuthenticatedUserIdResolver authenticatedUserIdResolver;

    @PostMapping
    public ResponseEntity<Void> recordPageView(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody @Valid PageViewRequest request) {
        if (jwt == null && request.page() != ApplicationPage.LANDING) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Authentication is required for this page view"
            );
        }

        UUID actorUserId = jwt == null ? null : authenticatedUserIdResolver.resolve(jwt);

        appEventPublisher.publish(PageViewedEvent.create(actorUserId, request.page()));

        return ResponseEntity.noContent().build();
    }
}
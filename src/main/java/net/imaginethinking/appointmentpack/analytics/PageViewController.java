package net.imaginethinking.appointmentpack.analytics;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.imaginethinking.appointmentpack.event.AppEventPublisher;
import net.imaginethinking.appointmentpack.event.analytics.PageViewedEvent;
import net.imaginethinking.appointmentpack.security.AuthenticatedUserIdResolver;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
            @RequestBody @Valid PageViewRequest request
    ) {
        UUID authenticatedUserId = authenticatedUserIdResolver.resolve(jwt);

        appEventPublisher.publish(
                PageViewedEvent.create(
                        authenticatedUserId,
                        request.page()
                )
        );

        return ResponseEntity.noContent().build();
    }
}
package net.imaginethinking.appointmentpack.analytics;

import jakarta.validation.constraints.NotNull;
import net.imaginethinking.appointmentpack.event.analytics.ApplicationPage;

/**
 * Carries the coarse application page recorded for analytics.
 */
public record PageViewRequest(
        @NotNull(message = "Page is required")
        ApplicationPage page
) {
}
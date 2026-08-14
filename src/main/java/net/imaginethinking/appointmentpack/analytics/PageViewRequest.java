package net.imaginethinking.appointmentpack.analytics;

import jakarta.validation.constraints.NotNull;
import net.imaginethinking.appointmentpack.event.analytics.ApplicationPage;

public record PageViewRequest(
        @NotNull(message = "Page is required")
        ApplicationPage page
) {
}
package net.imaginethinking.appointmentpack.patientaccess;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateCarerInvitationRequest(
        @NotNull
        UUID carerUserId
) {
}

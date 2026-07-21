package net.imaginethinking.appointmentpack.patientcareraccess;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateCarerInvitationRequest(
        @NotNull
        UUID carerUserId
) {
}

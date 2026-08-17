package net.imaginethinking.appointmentpack.auth;

public record AccountSecurityResponse(
        boolean mfaEnabled
) {
}

package net.imaginethinking.appointmentpack.auth;

public record MfaConfirmRequest(
        String code
) {
}

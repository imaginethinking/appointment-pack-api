package net.imaginethinking.appointmentpack.user;

import net.imaginethinking.appointmentpack.common.TextNormalizer;

import java.util.Locale;

public final class EmailAddressNormalizer {

    private EmailAddressNormalizer() {
    }

    public static String normalise(String email) {
        String normalisedEmail = TextNormalizer.stripToNull(email);

        if (normalisedEmail == null) {
            return null;
        }

        return normalisedEmail.toLowerCase(Locale.ROOT);
    }
}
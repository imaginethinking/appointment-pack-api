package net.imaginethinking.appointmentpack.user;

import net.imaginethinking.appointmentpack.common.TextNormalizer;

import java.util.Locale;

/**
 * Normalises email addresses before they are compared or stored.
 */
public final class EmailAddressNormalizer {

    /**
     * Prevents the utility class from being instantiated.
     */
    private EmailAddressNormalizer() {
    }

    /**
     * Trims an email address and converts it to lower case so comparisons use the same format.
     */
    public static String normalise(String email) {
        String normalisedEmail = TextNormalizer.stripToNull(email);

        if (normalisedEmail == null) {
            return null;
        }

        return normalisedEmail.toLowerCase(Locale.ROOT);
    }
}
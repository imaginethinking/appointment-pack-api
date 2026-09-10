package net.imaginethinking.appointmentpack.common;

/**
 * Provides small helpers for cleaning text before it is stored.
 */
public final class TextNormalizer {

    /**
     * Prevents the utility class from being instantiated.
     */
    private TextNormalizer() {
    }

    /**
     * Removes leading and trailing whitespace from a text value.
     */
    public static String strip(String value) {
        return value == null ? null : value.strip();
    }

    /**
     * Trims optional text and returns null when no characters remain.
     */
    public static String stripToNull(String value) {
        String strippedValue = strip(value);

        if (strippedValue == null || strippedValue.isEmpty()) {
            return null;
        }

        return strippedValue;
    }
}
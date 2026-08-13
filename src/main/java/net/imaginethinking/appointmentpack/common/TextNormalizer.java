package net.imaginethinking.appointmentpack.common;

public final class TextNormalizer {

    private TextNormalizer() {
    }

    public static String strip(String value) {
        return value == null ? null : value.strip();
    }

    public static String stripToNull(String value) {
        String strippedValue = strip(value);

        if (strippedValue == null || strippedValue.isEmpty()) {
            return null;
        }

        return strippedValue;
    }
}
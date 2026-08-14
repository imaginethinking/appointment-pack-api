package net.imaginethinking.appointmentpack.document.processing.context;

import java.util.List;

public record RedactionContext(
        List<String> knownValues
) {

    public RedactionContext {
        knownValues = knownValues == null
                ? List.of()
                : List.copyOf(knownValues);
    }

    public static RedactionContext empty() {
        return new RedactionContext(List.of());
    }
}
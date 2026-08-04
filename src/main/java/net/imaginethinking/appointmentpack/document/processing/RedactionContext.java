package net.imaginethinking.appointmentpack.document.processing;

import java.util.List;

public record RedactionContext(
        List<String> knownValues
) {

    public RedactionContext {
        knownValues = knownValues == null
                ? List.of()
                : List.copyOf(knownValues);
    }
}
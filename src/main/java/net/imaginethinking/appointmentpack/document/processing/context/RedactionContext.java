package net.imaginethinking.appointmentpack.document.processing.context;

import java.util.List;

/**
 * Keeps the values needed while redaction is being processed.
 */
public record RedactionContext(
        List<String> knownValues
) {

    /**
     * Copies the known patient values so the redaction context cannot be changed after it is created.
     */
    public RedactionContext {
        knownValues = knownValues == null
                ? List.of()
                : List.copyOf(knownValues);
    }

    /**
     * Creates an empty redaction context for document types that do not need known patient values.
     */
    public static RedactionContext empty() {
        return new RedactionContext(List.of());
    }
}
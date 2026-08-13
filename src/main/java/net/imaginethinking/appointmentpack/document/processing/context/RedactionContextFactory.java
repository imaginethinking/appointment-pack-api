package net.imaginethinking.appointmentpack.document.processing.context;

import net.imaginethinking.appointmentpack.address.Address;
import net.imaginethinking.appointmentpack.common.TextNormalizer;
import net.imaginethinking.appointmentpack.document.Document;
import net.imaginethinking.appointmentpack.patientrecord.PatientRecord;
import net.imaginethinking.appointmentpack.profile.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Component
public class RedactionContextFactory {

    private static final DateTimeFormatter UK_NUMERIC_DATE = DateTimeFormatter.ofPattern("dd/MM/uuuu", Locale.UK);
    private static final DateTimeFormatter UK_DASH_DATE = DateTimeFormatter.ofPattern("dd-MM-uuuu", Locale.UK);
    private static final DateTimeFormatter UK_DOT_DATE = DateTimeFormatter.ofPattern("dd.MM.uuuu", Locale.UK);
    private static final DateTimeFormatter LONG_DATE = DateTimeFormatter.ofPattern("d MMMM uuuu", Locale.UK);
    private static final DateTimeFormatter SHORT_MONTH_DATE = DateTimeFormatter.ofPattern("d MMM uuuu", Locale.UK);

    public RedactionContext create(Document document) {
        PatientRecord patientRecord = document.getPatientRecord();

        Profile profile = patientRecord.getProfile();

        Set<String> knownValues = new LinkedHashSet<>();

        addValue(knownValues, profile.getFirstName());
        addValue(knownValues, profile.getLastName());

        addFullNameValues(knownValues, profile.getFirstName(), profile.getLastName());
        addDateValues(knownValues, profile.getDateOfBirth());
        addIdentifierValues(knownValues, patientRecord.getNhsNumber());
        addIdentifierValues(knownValues, patientRecord.getChiNumber());
        addIdentifierValues(knownValues, patientRecord.getHcNumber());
        addAddressValues(knownValues, profile.getAddress());

        return new RedactionContext(new ArrayList<>(knownValues));
    }

    private void addFullNameValues(Set<String> values, String firstName, String lastName) {
        if (firstName == null || firstName.isBlank() || lastName == null || lastName.isBlank()) {
            return;
        }

        addValue(values, firstName + " " + lastName);
        addValue(values, lastName + ", " + firstName);
    }

    private void addDateValues(Set<String> values, LocalDate date) {
        if (date == null) {
            return;
        }

        List<DateTimeFormatter> formatters = List.of(
                DateTimeFormatter.ISO_LOCAL_DATE,
                UK_NUMERIC_DATE,
                UK_DASH_DATE,
                UK_DOT_DATE,
                LONG_DATE,
                SHORT_MONTH_DATE);

        for (DateTimeFormatter formatter : formatters) {
            addValue(values, formatter.format(date));
        }
    }

    private void addIdentifierValues(Set<String> values, String identifier) {
        if (identifier == null || identifier.isBlank()) {
            return;
        }

        addValue(values, identifier);

        String compactIdentifier = identifier.replaceAll("[\\s-]", "");

        addValue(values, compactIdentifier);

        if (compactIdentifier.matches("\\d{10}")) {
            addValue(
                    values,
                    compactIdentifier.substring(0, 3) + " " + compactIdentifier.substring(
                            3,
                            6) + " " + compactIdentifier.substring(6));

            addValue(values, compactIdentifier.substring(0, 6) + " " + compactIdentifier.substring(6));
        }
    }

    private void addAddressValues(Set<String> values, Address address) {
        if (address == null) {
            return;
        }

        addValue(values, address.getAddressLine1());
        addValue(values, address.getAddressLine2());
        addValue(values, address.getTownCity());
        addValue(values, address.getCounty());

        addPostcodeValues(values, address.getPostcode());

        List<String> addressParts = new ArrayList<>();

        addAddressPart(addressParts, address.getAddressLine1());
        addAddressPart(addressParts, address.getAddressLine2());
        addAddressPart(addressParts, address.getTownCity());
        addAddressPart(addressParts, address.getCounty());
        addAddressPart(addressParts, address.getPostcode());

        if (!addressParts.isEmpty()) {
            addValue(values, String.join(", ", addressParts));
            addValue(values, String.join(" ", addressParts));
        }
    }

    private void addPostcodeValues(Set<String> values, String postcode) {
        if (postcode == null || postcode.isBlank()) {
            return;
        }

        addValue(values, postcode);
        addValue(values, postcode.replaceAll("\\s+", ""));
    }

    private void addAddressPart(List<String> parts, String value) {
        if (value == null || value.isBlank()) {
            return;
        }

        parts.add(TextNormalizer.strip(value));
    }

    private void addValue(Set<String> values, String value) {
        if (value == null) {
            return;
        }

        String normalisedValue = TextNormalizer.strip(value);

        if (!normalisedValue.isBlank()) {
            values.add(normalisedValue);
        }
    }
}
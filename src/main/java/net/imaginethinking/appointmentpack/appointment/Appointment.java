package net.imaginethinking.appointmentpack.appointment;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import net.imaginethinking.appointmentpack.address.Address;
import net.imaginethinking.appointmentpack.common.ArchivableEntity;
import net.imaginethinking.appointmentpack.document.Document;
import net.imaginethinking.appointmentpack.patientrecord.PatientRecord;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Stores an appointment together with optional source document information.
 */
@Getter
@Setter
@Entity
@Table(
        name = "appointments",
        uniqueConstraints = {@UniqueConstraint(
                name = "uk_appointment_source_document",
                columnNames = "source_document_id"
        )}
)
public class Appointment extends ArchivableEntity {

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "patient_record_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_appointment_patient_record"
            )
    )
    private PatientRecord patientRecord;

    @Column(
            name = "appointment_date",
            nullable = false
    )
    private LocalDate date;

    @Column(
            name = "start_time",
            nullable = false
    )
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    @Column(
            name = "service",
            length = 250
    )
    private String service;

    @Column(
            name = "appointment_type",
            length = 250
    )
    private String appointmentType;

    @Column(
            name = "clinician_or_team",
            length = 250
    )
    private String clinicianOrTeam;

    @Column(
            name = "location_name",
            length = 250
    )
    private String locationName;

    @Embedded
    @AttributeOverrides(
            {@AttributeOverride(
                    name = "addressLine1",
                    column = @Column(
                            name = "location_address_line_1",
                            length = 150
                    )
            ), @AttributeOverride(
                    name = "addressLine2",
                    column = @Column(
                            name = "location_address_line_2",
                            length = 150
                    )
            ), @AttributeOverride(
                    name = "townCity",
                    column = @Column(
                            name = "location_town_city",
                            length = 100
                    )
            ), @AttributeOverride(
                    name = "county",
                    column = @Column(
                            name = "location_county",
                            length = 100
                    )
            ), @AttributeOverride(
                    name = "postcode",
                    column = @Column(
                            name = "location_postcode",
                            length = 20
                    )
            ), @AttributeOverride(
                    name = "country",
                    column = @Column(
                            name = "location_country",
                            length = 100
                    )
            )}
    )
    private Address address;

    @Column(
            name = "notes",
            length = 2000
    )
    private String notes;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "source_document_id",
            unique = true,
            foreignKey = @ForeignKey(
                    name = "fk_appointment_source_document"
            )
    )
    private Document sourceDocument;
}
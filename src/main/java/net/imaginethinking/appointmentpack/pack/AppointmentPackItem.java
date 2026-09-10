package net.imaginethinking.appointmentpack.pack;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import net.imaginethinking.appointmentpack.common.BaseEntity;

import java.util.UUID;

/**
 * Records the type and source ID of an item included in a generated Appointment Pack.
 */
@Getter
@Setter
@Entity
@Table(
        name = "appointment_pack_items",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_appointment_pack_item_resource",
                        columnNames = {
                                "appointment_pack_id",
                                "resource_type",
                                "resource_id"
                        }
                )
        }
)
public class AppointmentPackItem extends BaseEntity {

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "appointment_pack_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_appointment_pack_item_pack"
            )
    )
    private AppointmentPack appointmentPack;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "resource_type",
            nullable = false,
            length = 50
    )
    private AppointmentPackItemType resourceType;

    @Column(
            name = "resource_id",
            nullable = false
    )
    private UUID resourceId;

    @Column(
            name = "display_order",
            nullable = false
    )
    private int displayOrder;
}
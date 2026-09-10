package net.imaginethinking.appointmentpack.bloodtest;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import net.imaginethinking.appointmentpack.common.BaseEntity;

import java.math.BigDecimal;

/**
 * Stores one result row belonging to a blood test.
 */
@Getter
@Setter
@Entity
@Table(
        name = "blood_test_results",
        indexes = {
                @Index(
                        name = "idx_blood_test_result_analyte_key",
                        columnList = "analyte_key"
                )
        }
)
public class BloodTestResult extends BaseEntity {

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "blood_test_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_blood_test_result_blood_test"
            )
    )
    private BloodTest bloodTest;

    @Column(
            name = "analyte_name",
            nullable = false,
            length = 200
    )
    private String analyteName;

    @Column(
            name = "analyte_key",
            nullable = false,
            length = 200
    )
    private String analyteKey;

    @Column(
            name = "result_value",
            nullable = false,
            length = 100
    )
    private String resultValue;

    @Column(
            name = "numeric_value",
            precision = 30,
            scale = 10
    )
    private BigDecimal numericValue;

    @Column(
            name = "unit",
            length = 100
    )
    private String unit;

    @Column(
            name = "reference_range",
            length = 150
    )
    private String referenceRange;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "result_flag",
            length = 30
    )
    private BloodTestResultFlag flag;

    @Column(
            name = "display_order",
            nullable = false
    )
    private int displayOrder;
}
package net.imaginethinking.appointmentpack.bloodtest;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import net.imaginethinking.appointmentpack.common.BaseEntity;

import java.math.BigDecimal;

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
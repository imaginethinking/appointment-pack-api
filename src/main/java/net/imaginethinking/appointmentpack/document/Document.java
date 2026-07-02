package net.imaginethinking.appointmentpack.document;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import net.imaginethinking.appointmentpack.common.BaseEntity;
import net.imaginethinking.appointmentpack.user.User;

@Getter
@Setter
@Entity
@Table(name = "documents")
public class Document extends BaseEntity {
    private String name;

    private String extractedText;
    private String extractedDate;
    private String extractedSummary;
    private String aiSummary;
    private String approvedSummary;

    @Enumerated(EnumType.STRING)
    @Column
    private DocumentType type;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false
    )
    private User user;
}

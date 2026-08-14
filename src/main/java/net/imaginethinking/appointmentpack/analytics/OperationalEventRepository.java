package net.imaginethinking.appointmentpack.analytics;

import net.imaginethinking.appointmentpack.event.auth.AuthenticationAction;
import net.imaginethinking.appointmentpack.event.auth.AuthenticationOutcome;
import net.imaginethinking.appointmentpack.event.processing.DocumentProcessingFailureReason;
import net.imaginethinking.appointmentpack.event.processing.DocumentProcessingOperation;
import net.imaginethinking.appointmentpack.event.processing.ProcessingOutcome;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface OperationalEventRepository extends JpaRepository<OperationalEvent, UUID> {

    boolean existsBySourceEventId(UUID sourceEventId);

    @Query(
            """
                    select count(event)
                    from OperationalEvent event
                    where event.occurredAt >= :from
                      and event.occurredAt < :to
                    """
    )
    long countInRange(
            @Param("from") Instant from,
            @Param("to") Instant to
    );

    @Query(
            """
                    select count(distinct event.userId)
                    from OperationalEvent event
                    where event.userId is not null
                      and event.occurredAt >= :from
                      and event.occurredAt < :to
                      and (
                            event.category in :activityCategories
                            or (
                                event.authenticationAction in :successfulAuthenticationActions
                                and event.authenticationOutcome = :successfulOutcome
                            )
                      )
                    """
    )
    long countDistinctActiveUsersInRange(
            @Param("activityCategories") Collection<OperationalEventCategory> activityCategories,
            @Param("successfulAuthenticationActions") Collection<AuthenticationAction> successfulAuthenticationActions,
            @Param("successfulOutcome") AuthenticationOutcome successfulOutcome,
            @Param("from") Instant from,
            @Param("to") Instant to
    );

    @Query(
            """
                    select count(event)
                    from OperationalEvent event
                    where event.authenticationAction = :action
                      and event.occurredAt >= :from
                      and event.occurredAt < :to
                    """
    )
    long countAuthenticationActionInRange(
            @Param("action") AuthenticationAction action,
            @Param("from") Instant from,
            @Param("to") Instant to
    );

    @Query(
            """
                    select count(event)
                    from OperationalEvent event
                    where event.authenticationAction = :action
                      and event.authenticationOutcome = :outcome
                      and event.occurredAt >= :from
                      and event.occurredAt < :to
                    """
    )
    long countAuthenticationInRange(
            @Param("action") AuthenticationAction action,
            @Param("outcome") AuthenticationOutcome outcome,
            @Param("from") Instant from,
            @Param("to") Instant to
    );

    @Query(
            """
                    select count(event)
                    from OperationalEvent event
                    where event.processingOperation = :operation
                      and event.processingOutcome = :outcome
                      and event.occurredAt >= :from
                      and event.occurredAt < :to
                    """
    )
    long countProcessingInRange(
            @Param("operation") DocumentProcessingOperation operation,
            @Param("outcome") ProcessingOutcome outcome,
            @Param("from") Instant from,
            @Param("to") Instant to
    );

    @Query(
            """
                    select avg(event.durationMs)
                    from OperationalEvent event
                    where event.processingOperation = :operation
                      and event.processingOutcome = :outcome
                      and event.durationMs is not null
                      and event.occurredAt >= :from
                      and event.occurredAt < :to
                    """
    )
    Double averageProcessingDurationInRange(
            @Param("operation") DocumentProcessingOperation operation,
            @Param("outcome") ProcessingOutcome outcome,
            @Param("from") Instant from,
            @Param("to") Instant to
    );

    @Query(
            """
                    select count(event)
                    from OperationalEvent event
                    where event.processingFailureReason = :reason
                      and event.occurredAt >= :from
                      and event.occurredAt < :to
                    """
    )
    long countProcessingFailureReasonInRange(
            @Param("reason") DocumentProcessingFailureReason reason,
            @Param("from") Instant from,
            @Param("to") Instant to
    );

    @Query(
            """
                    select event.patientResourceType as resourceType,
                           event.patientAction as action,
                           count(event) as eventCount
                    from OperationalEvent event
                    where event.category = :category
                      and event.occurredAt >= :from
                      and event.occurredAt < :to
                    group by event.patientResourceType, event.patientAction
                    order by count(event) desc
                    """
    )
    List<PatientActivityCountProjection> countPatientActivityInRange(
            @Param("category") OperationalEventCategory category,
            @Param("from") Instant from,
            @Param("to") Instant to
    );

    @Query(
            """
                    select event.page as page,
                           count(event) as eventCount
                    from OperationalEvent event
                    where event.category = :category
                      and event.page is not null
                      and event.occurredAt >= :from
                      and event.occurredAt < :to
                    group by event.page
                    order by count(event) desc
                    """
    )
    List<PageViewCountProjection> countPageViewsInRange(
            @Param("category") OperationalEventCategory category,
            @Param("from") Instant from,
            @Param("to") Instant to
    );

    @Query(
            value = """
                    select event
                    from OperationalEvent event
                    where event.occurredAt >= :from
                      and event.occurredAt < :to
                    order by event.occurredAt desc, event.id desc
                    """,
            countQuery = """
                    select count(event)
                    from OperationalEvent event
                    where event.occurredAt >= :from
                      and event.occurredAt < :to
                    """
    )
    Page<OperationalEvent> findEventsInRange(
            @Param("from") Instant from,
            @Param("to") Instant to,
            Pageable pageable
    );

    @Query(
            value = """
                    select event
                    from OperationalEvent event
                    where event.category = :category
                      and event.occurredAt >= :from
                      and event.occurredAt < :to
                    order by event.occurredAt desc, event.id desc
                    """,
            countQuery = """
                    select count(event)
                    from OperationalEvent event
                    where event.category = :category
                      and event.occurredAt >= :from
                      and event.occurredAt < :to
                    """
    )
    Page<OperationalEvent> findEventsByCategoryInRange(
            @Param("category") OperationalEventCategory category,
            @Param("from") Instant from,
            @Param("to") Instant to,
            Pageable pageable
    );
}
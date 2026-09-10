package net.imaginethinking.appointmentpack.analytics;

import net.imaginethinking.appointmentpack.event.patient.PatientActivityAction;
import net.imaginethinking.appointmentpack.event.patient.PatientResourceType;

/**
 * Provides the values returned by the grouped patient activity count projection query.
 */
public interface PatientActivityCountProjection {

    /**
     * Returns the patient resource type for this grouped activity count.
     */
    PatientResourceType getResourceType();

    /**
     * Returns the patient activity action for this grouped count.
     */
    PatientActivityAction getAction();

    /**
     * Returns the number of matching patient activity events.
     */
    long getEventCount();
}
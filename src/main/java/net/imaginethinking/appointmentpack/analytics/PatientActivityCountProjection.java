package net.imaginethinking.appointmentpack.analytics;

import net.imaginethinking.appointmentpack.event.patient.PatientActivityAction;
import net.imaginethinking.appointmentpack.event.patient.PatientResourceType;

public interface PatientActivityCountProjection {

    PatientResourceType getResourceType();

    PatientActivityAction getAction();

    long getEventCount();
}
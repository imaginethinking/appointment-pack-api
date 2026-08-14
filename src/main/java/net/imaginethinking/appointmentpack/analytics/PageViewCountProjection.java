package net.imaginethinking.appointmentpack.analytics;

import net.imaginethinking.appointmentpack.event.analytics.ApplicationPage;

public interface PageViewCountProjection {

    ApplicationPage getPage();

    long getEventCount();
}
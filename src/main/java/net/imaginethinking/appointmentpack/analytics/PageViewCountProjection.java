package net.imaginethinking.appointmentpack.analytics;

import net.imaginethinking.appointmentpack.event.analytics.ApplicationPage;

/**
 * Provides the values returned by the grouped page view count projection query.
 */
public interface PageViewCountProjection {

    /**
     * Returns the application page for this grouped page view count.
     */
    ApplicationPage getPage();

    /**
     * Returns the number of recorded views for the page.
     */
    long getEventCount();
}
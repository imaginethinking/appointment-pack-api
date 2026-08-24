package net.imaginethinking.appointmentpack.analytics;

import net.imaginethinking.appointmentpack.event.analytics.ApplicationPage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static net.imaginethinking.appointmentpack.testsupport.ValidationTestSupport.assertInvalidField;
import static net.imaginethinking.appointmentpack.testsupport.ValidationTestSupport.assertValid;

class PageViewRequestValidationTest {

    @ParameterizedTest
    @EnumSource(ApplicationPage.class)
    void shouldAcceptEverySupportedApplicationPage(
            ApplicationPage page) {
        assertValid(new PageViewRequest(page));
    }

    @Test
    void shouldRejectNullApplicationPage() {
        assertInvalidField(new PageViewRequest(null), "page");
    }
}
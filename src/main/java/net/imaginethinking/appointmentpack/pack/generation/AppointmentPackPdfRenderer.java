package net.imaginethinking.appointmentpack.pack.generation;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * Renders the prepared Appointment Pack model into PDF bytes using the pack template and stylesheet.
 */
@Service
public class AppointmentPackPdfRenderer {

    private static final String TEMPLATE_NAME = "appointment-pack/appointment-pack";
    private static final String STYLESHEET_PATH = "appointment-pack/appointment-pack.css";

    private final SpringTemplateEngine templateEngine;
    private final String stylesheet;

    /**
     * Creates the PDF renderer using the shared Thymeleaf template engine.
     */
    public AppointmentPackPdfRenderer(SpringTemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
        this.stylesheet = loadStylesheet();
    }

    /**
     * Renders the pack template with the prepared values and converts the resulting HTML into PDF bytes.
     */
    public byte[] render(AppointmentPackRenderModel renderModel) {
        Context context = new Context(Locale.UK);

        context.setVariable("model", renderModel);

        context.setVariable("stylesheet", stylesheet);

        String html = templateEngine.process(TEMPLATE_NAME, context);

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            PdfRendererBuilder builder = new PdfRendererBuilder();

            builder.useFastMode();
            builder.withHtmlContent(html, null);
            builder.toStream(outputStream);
            builder.run();

            return outputStream.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to generate appointment pack PDF", exception);
        }
    }

    /**
     * Loads the Appointment Pack stylesheet from the application resources.
     */
    private String loadStylesheet() {
        ClassPathResource resource = new ClassPathResource(STYLESHEET_PATH);

        try (InputStream inputStream = resource.getInputStream()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to load appointment pack stylesheet", exception);
        }
    }
}
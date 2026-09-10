package net.imaginethinking.appointmentpack.document.processing.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import net.imaginethinking.appointmentpack.document.processing.context.DocumentExtractionContext;
import net.imaginethinking.appointmentpack.document.processing.context.DocumentSummarisationContext;
import net.imaginethinking.appointmentpack.document.processing.context.RedactionContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

import java.net.SocketTimeoutException;
import java.net.http.HttpClient;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Calls the private document processing service and turns its responses into the errors used by this application.
 */
@Component
public class DocumentProcessingClient {

    private static final String EXTRACTION_ENDPOINT = "/internal/v1/documents/extract";
    private static final String SUMMARISATION_ENDPOINT = "/internal/v1/documents/summarise";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    /**
     * Creates the document processing client client and applies the configured connection and response timeouts.
     */
    public DocumentProcessingClient(
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper,
            @Value("${appointment-pack.document-processing.base-url}") String baseUrl,
            @Value("${appointment-pack.document-processing.api-key}") String apiKey,
            @Value("${appointment-pack.document-processing.connect-timeout-seconds:5}") long connectTimeoutSeconds,
            @Value("${appointment-pack.document-processing.response-timeout-seconds:120}") long responseTimeoutSeconds) {
        validateTimeout("Connection timeout", connectTimeoutSeconds);
        validateTimeout("Response timeout", responseTimeoutSeconds);

        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(connectTimeoutSeconds))
                .build();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);

        requestFactory.setReadTimeout(Duration.ofSeconds(responseTimeoutSeconds));

        this.restClient = restClientBuilder.requestFactory(requestFactory)
                .baseUrl(baseUrl)
                .defaultHeader("X-Internal-Api-Key", apiKey)
                .build();

        this.objectMapper = objectMapper;
    }

    /**
     * Builds the multipart extraction request, sends the stored document for processing and checks the returned
     * document ID.
     */
    public DocumentExtractionResponse extract(DocumentExtractionContext context, Resource documentResource) {
        try {
            MultiValueMap<String, Object> requestParts = createExtractionRequestParts(context, documentResource);

            DocumentExtractionResponse response = restClient.post()
                    .uri(EXTRACTION_ENDPOINT)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(requestParts)
                    .retrieve()
                    .body(DocumentExtractionResponse.class);

            validateResponse(context, response);

            return response;
        } catch (JsonProcessingException exception) {
            throw new DocumentProcessingException("Failed to create document extraction request", exception);
        } catch (ResourceAccessException exception) {
            throw mapResourceAccessFailure(exception);
        } catch (RestClientResponseException exception) {
            throw mapExtractionHttpFailure(exception);
        } catch (RestClientException exception) {
            throw new DocumentProcessingException("Document extraction service request failed", exception);
        }
    }

    /**
     * Sends the approved deidentified text for summarisation and checks the returned summary before using it.
     */
    public DocumentSummaryResponse summarise(DocumentSummarisationContext context) {
        DocumentSummaryClientRequest request = new DocumentSummaryClientRequest(
                context.documentId(),
                context.approvedDeidentifiedText());

        try {
            DocumentSummaryResponse response = restClient.post()
                    .uri(SUMMARISATION_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(DocumentSummaryResponse.class);

            validateSummaryResponse(context, response);

            return response;
        } catch (ResourceAccessException exception) {
            throw mapResourceAccessFailure(exception);
        } catch (RestClientResponseException exception) {
            throw mapSummarisationHttpFailure(exception);
        } catch (RestClientException exception) {
            throw new DocumentProcessingException("Document summarisation service request failed", exception);
        }
    }

    /**
     * Maps extraction HTTP errors to the application errors shown for document processing failures.
     */
    private RuntimeException mapExtractionHttpFailure(
            RestClientResponseException exception) {
        return switch (exception.getStatusCode().value()) {
            case 413 -> new ResponseStatusException(
                    HttpStatus.PAYLOAD_TOO_LARGE,
                    "Document exceeds the processing size limit");

            case 415 -> new ResponseStatusException(
                    HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "Document type is not supported by the processing service");

            case 422 -> new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_CONTENT,
                    "Document text could not be extracted");

            case 503 -> new DocumentProcessingUnavailableException(exception);

            case 504 -> new DocumentProcessingTimeoutException(exception);

            default -> new DocumentProcessingException("Document extraction service returned an unexpected response");
        };
    }

    /**
     * Maps summarisation HTTP errors to the application errors used by the consultation workflow.
     */
    private RuntimeException mapSummarisationHttpFailure(
            RestClientResponseException exception) {
        return switch (exception.getStatusCode().value()) {
            case 413 -> new ResponseStatusException(
                    HttpStatus.PAYLOAD_TOO_LARGE,
                    "Approved de-identified text exceeds the processing size limit");

            case 503 -> new DocumentProcessingUnavailableException(exception);

            case 504 -> new DocumentProcessingTimeoutException(exception);

            default ->
                    new DocumentProcessingException("Document summarisation service returned an unexpected response");
        };
    }

    /**
     * Separates processing timeouts from other connection failures when the processing service cannot be reached.
     */
    private RuntimeException mapResourceAccessFailure(
            ResourceAccessException exception) {
        if (hasCause(exception, HttpConnectTimeoutException.class)) {
            return new DocumentProcessingUnavailableException(exception);
        }

        if (hasCause(exception, HttpTimeoutException.class) || hasCause(exception, SocketTimeoutException.class)) {
            return new DocumentProcessingTimeoutException(exception);
        }

        return new DocumentProcessingUnavailableException(exception);
    }

    /**
     * Checks an exception and its causes for the requested failure type.
     */
    private boolean hasCause(Throwable throwable, Class<? extends Throwable> causeType) {
        Throwable current = throwable;

        while (current != null) {
            if (causeType.isInstance(current)) {
                return true;
            }

            current = current.getCause();
        }

        return false;
    }

    /**
     * Builds the multipart fields expected by the document extraction endpoint.
     */
    private MultiValueMap<String, Object> createExtractionRequestParts(
            DocumentExtractionContext context,
            Resource documentResource) throws JsonProcessingException {
        MultiValueMap<String, Object> requestParts = new LinkedMultiValueMap<>();

        requestParts.add("documentId", context.documentId().toString());

        requestParts.add("documentType", context.documentType().name());

        requestParts.add("redactionContext", createRedactionContextPart(context.redactionContext()));

        requestParts.add("file", createFilePart(context, documentResource));

        return requestParts;
    }

    /**
     * Serialises the known patient values included with a consultation extraction request.
     */
    private HttpEntity<String> createRedactionContextPart(
            RedactionContext redactionContext) throws JsonProcessingException {
        HttpHeaders headers = new HttpHeaders();

        headers.setContentType(MediaType.APPLICATION_JSON);

        headers.setContentDisposition(ContentDisposition.formData().name("redactionContext").build());

        String json = objectMapper.writeValueAsString(redactionContext);

        return new HttpEntity<>(json, headers);
    }

    /**
     * Creates the multipart file part using the stored document resource and original file name.
     */
    private HttpEntity<Resource> createFilePart(DocumentExtractionContext context, Resource documentResource) {
        HttpHeaders headers = new HttpHeaders();

        headers.setContentType(MediaType.parseMediaType(context.contentType()));

        headers.setContentDisposition(ContentDisposition.formData()
                .name("file")
                .filename(context.originalFileName(), StandardCharsets.UTF_8)
                .build());

        return new HttpEntity<>(documentResource, headers);
    }

    /**
     * Checks that the extraction response belongs to the document that was sent for processing.
     */
    private void validateResponse(DocumentExtractionContext context, DocumentExtractionResponse response) {
        if (response == null) {
            throw new DocumentProcessingException("Document extraction service returned an empty response");
        }

        if (!context.documentId().equals(response.documentId())) {
            throw new DocumentProcessingException("Document extraction response contains an unexpected document ID");
        }
    }

    /**
     * Checks that the summary response belongs to the requested document and contains summary text.
     */
    private void validateSummaryResponse(DocumentSummarisationContext context, DocumentSummaryResponse response) {
        if (response == null) {
            throw new DocumentProcessingException("Document summarisation service returned an empty response");
        }

        if (!context.documentId().equals(response.documentId())) {
            throw new DocumentProcessingException("Document summarisation response contains an unexpected document ID");
        }
    }

    /**
     * Rejects zero or negative processing timeout settings during application startup.
     */
    private void validateTimeout(String name, long timeoutSeconds) {
        if (timeoutSeconds <= 0) {
            throw new IllegalArgumentException(name + " must be greater than zero");
        }
    }
}
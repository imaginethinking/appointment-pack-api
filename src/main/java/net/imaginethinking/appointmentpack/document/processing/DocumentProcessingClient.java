package net.imaginethinking.appointmentpack.document.processing;

import net.imaginethinking.appointmentpack.document.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Component
public class DocumentProcessingClient {

    private static final String EXTRACTION_ENDPOINT =
            "/internal/v1/documents/extract";

    private final RestClient restClient;

    public DocumentProcessingClient(
            RestClient.Builder restClientBuilder,
            @Value("${appointment-pack.document-processing.base-url}")
            String baseUrl,
            @Value("${appointment-pack.document-processing.api-key}")
            String apiKey,
            @Value("${appointment-pack.document-processing.connect-timeout-seconds:5}")
            long connectTimeoutSeconds,
            @Value("${appointment-pack.document-processing.response-timeout-seconds:120}")
            long responseTimeoutSeconds
    ) {
        validateTimeout("Connection timeout", connectTimeoutSeconds);
        validateTimeout("Response timeout", responseTimeoutSeconds);

        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(connectTimeoutSeconds))
                .build();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);

        requestFactory.setReadTimeout(Duration.ofSeconds(responseTimeoutSeconds));

        this.restClient = restClientBuilder
                .requestFactory(requestFactory)
                .baseUrl(baseUrl)
                .defaultHeader("X-Internal-Api-Key", apiKey)
                .build();
    }

    public DocumentExtractionResponse extract(
            Document document,
            Resource documentResource
    ) {
        MultiValueMap<String, Object> requestParts =
                createExtractionRequestParts(
                        document,
                        documentResource
                );

        try {
            DocumentExtractionResponse response = restClient
                    .post()
                    .uri(EXTRACTION_ENDPOINT)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(requestParts)
                    .retrieve()
                    .body(DocumentExtractionResponse.class);

            validateResponse(document, response);

            return response;
        } catch (RestClientException exception) {
            throw new DocumentProcessingException("Document extraction service request failed", exception);
        }
    }

    private MultiValueMap<String, Object> createExtractionRequestParts(
            Document document,
            Resource documentResource
    ) {
        MultiValueMap<String, Object> requestParts = new LinkedMultiValueMap<>();

        requestParts.add(
                "documentId",
                document.getId().toString()
        );

        requestParts.add(
                "documentType",
                document.getDocumentType().name()
        );

        requestParts.add(
                "file",
                createFilePart(
                        document,
                        documentResource
                )
        );

        return requestParts;
    }

    private HttpEntity<Resource> createFilePart(
            Document document,
            Resource documentResource
    ) {
        HttpHeaders headers = new HttpHeaders();

        headers.setContentType(MediaType.parseMediaType(document.getContentType()));

        headers.setContentDisposition(
                ContentDisposition
                        .formData()
                        .name("file")
                        .filename(
                                document.getOriginalFileName(),
                                StandardCharsets.UTF_8
                        )
                        .build()
        );

        return new HttpEntity<>(documentResource, headers);
    }

    private void validateResponse(
            Document document,
            DocumentExtractionResponse response
    ) {
        if (response == null) {
            throw new DocumentProcessingException("Document extraction service returned an empty response");
        }

        if (!document.getId().equals(response.documentId())) {
            throw new DocumentProcessingException("Document extraction response contains an unexpected document ID");
        }
    }

    private void validateTimeout(
            String name,
            long timeoutSeconds
    ) {
        if (timeoutSeconds <= 0) {
            throw new IllegalArgumentException(
                    name + " must be greater than zero"
            );
        }
    }
}
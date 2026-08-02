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

@Component
public class DocumentProcessingClient {

    private static final String PROCESSING_ENDPOINT = "/internal/v1/documents/process";

    private final RestClient restClient;

    public DocumentProcessingClient(
            RestClient.Builder restClientBuilder,
            @Value("${appointment-pack.document-processing.base-url}")
            String baseUrl,
            @Value("${appointment-pack.document-processing.api-key}")
            String apiKey
    ) {
        //TODO Make this https later so it has tls?
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .build();

        this.restClient = restClientBuilder
                .requestFactory(new JdkClientHttpRequestFactory(httpClient))
                .baseUrl(baseUrl)
                .defaultHeader("X-Internal-Api-Key", apiKey)
                .build();
    }

    public DocumentProcessingResponse process(
            Document document,
            Resource documentResource
    ) {
        MultiValueMap<String, Object> requestParts = createRequestParts(document, documentResource);

        try {
            DocumentProcessingResponse response = restClient
                    .post()
                    .uri(PROCESSING_ENDPOINT)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(requestParts)
                    .retrieve()
                    .body(DocumentProcessingResponse.class);

            validateResponse(document, response);

            return response;
        } catch (RestClientException exception) {
            throw new DocumentProcessingException("Document processing service request failed", exception);
        }
    }

    private MultiValueMap<String, Object> createRequestParts(
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
                createFilePart(document, documentResource)
        );

        return requestParts;
    }

    private HttpEntity<Resource> createFilePart(
            Document document,
            Resource documentResource
    ) {
        HttpHeaders headers = new HttpHeaders();

        headers.setContentType(
                MediaType.parseMediaType(document.getContentType())
        );

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

        return new HttpEntity<>(
                documentResource,
                headers
        );
    }

    private void validateResponse(
            Document document,
            DocumentProcessingResponse response
    ) {
        if (response == null) {
            throw new DocumentProcessingException(
                    "Document processing service returned an empty response"
            );
        }

        if (!document.getId().equals(response.documentId())) {
            throw new DocumentProcessingException(
                    "Document processing response contains an unexpected document ID"
            );
        }
    }
}
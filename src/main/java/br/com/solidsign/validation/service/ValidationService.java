package br.com.solidsign.validation.service;

import br.com.solidsign.validation.model.ValidationReportsResponseDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Service
public class ValidationService {

    private static final Logger log = LoggerFactory.getLogger(ValidationService.class);

    private final RestTemplate restTemplate;

    @Value("${solidsign.api.base-url}")
    private String baseUrl;

    @Value("${solidsign.api.authorization}")
    private String authorization;

    @Value("${solidsign.batch.input-path}")
    private String batchInputPath;

    public ValidationService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Validates XML files (XAdES signatures) from the configured input directory.
     */
    public ValidationReportsResponseDTO validateBatch() throws IOException {
        File dir = new File(batchInputPath);
        File[] xmlFiles = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".xml"));
        if (xmlFiles == null || xmlFiles.length == 0) {
            throw new IllegalArgumentException("No XML files found in: " + batchInputPath);
        }

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        for (File xml : xmlFiles) {
            body.add("document", new FileSystemResource(xml));
        }
        log.info("Validating {} XML file(s) from {}", xmlFiles.length, batchInputPath);
        return callApi(body);
    }

    /**
     * Validates XML files received via multipart form upload.
     * @param files uploaded XML files with XAdES signatures
     */
    public ValidationReportsResponseDTO validateForm(List<MultipartFile> files) throws IOException {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        for (MultipartFile mf : files) {
            Path tmp = Files.createTempFile("solidsign-xml-", ".xml");
            mf.transferTo(tmp);
            tmp.toFile().deleteOnExit();
            String originalName = mf.getOriginalFilename();
            body.add("document", new FileSystemResource(tmp.toFile()) {
                @Override public String getFilename() { return originalName; }
            });
        }
        log.info("Validating {} uploaded XML file(s)", files.size());
        return callApi(body);
    }

    private ValidationReportsResponseDTO callApi(MultiValueMap<String, Object> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.set(HttpHeaders.AUTHORIZATION, authorization);

        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);
        String url = baseUrl.replaceAll("/+$", "") + "/solidsign/dsig/validation/verify-xml";

        ResponseEntity<ValidationReportsResponseDTO> response =
            restTemplate.exchange(url, HttpMethod.POST, request, ValidationReportsResponseDTO.class);

        return response.getBody();
    }
}

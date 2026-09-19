package br.com.solidsign.validation.controller;

import br.com.solidsign.validation.model.ValidationReportsResponseDTO;
import br.com.solidsign.validation.service.ValidationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * Exposes two validation endpoints for XAdES/XML signatures:
 *   POST /api/xml/validate/batch — reads XML files from the server-side input directory
 *   POST /api/xml/validate/form  — receives XML files via multipart form upload
 */
@RestController
@RequestMapping("/api/xml/validate")
public class ValidationController {

    private final ValidationService service;

    public ValidationController(ValidationService service) {
        this.service = service;
    }

    /**
     * Validates all XML files found in the configured {@code solidsign.batch.input-path}.
     *
     * Example:
     *   curl -X POST http://localhost:8096/api/xml/validate/batch
     */
    @PostMapping("/batch")
    public ResponseEntity<ValidationReportsResponseDTO> validateBatch() throws IOException {
        return ResponseEntity.ok(service.validateBatch());
    }

    /**
     * Validates XML files sent as multipart form data.
     *
     * Example:
     *   curl -X POST http://localhost:8096/api/xml/validate/form \
     *        -F "document=@/path/to/signed.xml" \
     *        -F "document=@/path/to/other.xml"
     */
    @CrossOrigin
    @PostMapping("/form")
    public ResponseEntity<ValidationReportsResponseDTO> validateForm(
            @RequestPart("document") List<MultipartFile> files,
            @RequestPart(value = "authorization", required = false) String authorization,
            @RequestPart(value = "baseUrl", required = false) String baseUrl) throws IOException {
        return ResponseEntity.ok(service.validateForm(files, authorization, baseUrl));
    }
}

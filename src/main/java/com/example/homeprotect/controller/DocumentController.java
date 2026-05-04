package com.example.homeprotect.controller;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;

import com.example.homeprotect.controller.docs.DocumentControllerDocs;
import com.example.homeprotect.service.DocumentService;

import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/documents")
public class DocumentController implements DocumentControllerDocs {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping(value = "/ocr", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ResponseEntity<Map<String, Object>>> scanDocument(
            @RequestPart("file") FilePart file,
            @RequestPart("docType") String docType) {
        return documentService.scanDocument(file, docType)
            .map(data -> {
              Map<String, Object> inner = new LinkedHashMap<>();
              inner.put("sessionId", data.getSessionId());
              inner.put("safe", data.isSafe());

              Map<String, Object> response = new LinkedHashMap<>();
              response.put("status", "success");
              response.put("data", inner);

              return ResponseEntity.ok(response);
            });
    }
}

package io.covena.document.adapter.in.web;

import io.covena.document.application.port.in.UploadDocumentCommand;
import io.covena.document.application.port.in.UploadDocumentUseCase;
import io.covena.document.application.port.in.UploadedDocumentResult;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {
    private final UploadDocumentUseCase uploadDocumentUseCase;

    public DocumentController(UploadDocumentUseCase uploadDocumentUseCase) {
        this.uploadDocumentUseCase = uploadDocumentUseCase;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UploadResponse> uploadDocument(@RequestPart("file") MultipartFile file) {
        UploadDocumentCommand uploadDocumentCommand = new UploadDocumentCommand(file.getOriginalFilename(),
            file.getSize(),
            file.getContentType(),
            file::getInputStream);
        UploadedDocumentResult uploadedDocumentResult = uploadDocumentUseCase.uploadDocument(uploadDocumentCommand);
        UploadResponse uploadResponse = UploadResponse.from(uploadedDocumentResult);
        URI location = ServletUriComponentsBuilder
            .fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(uploadResponse.id())
            .toUri();
        return ResponseEntity.created(location).body(uploadResponse);
    }
}

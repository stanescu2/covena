# ADR-0003: Text extraction for PDF and DOCX in knowledge-service

- **Status:** Proposed
- **Date:** 2026-11-11
- **Deciders:** Adriana Stanescu
- **Scope:** knowledge-service (document ingestion pipeline)

## Context and problem

knowledge-service consumes `DocumentUploaded` events, fetches the file content from document-service and turns it into
searchable chunks with embeddings stored in pgvector.
The first step of this pipeline is text extraction: reading the raw bytes of an uploaded file and producing text plus
metadata.

document-service accepts exactly two content types (`ContentType`): PDF and DOCX.

Covena answers questions with citations, so every chunk must carry metadata that points back to its source:
the `documentId` and, for PDF files, the page number.
Spring AI's `TokenTextSplitter` copies the metadata of each input document to every chunk it produces, so page-level
metadata must already exist when the text is extracted.

Spring AI 2.0.x (we use 2.0.2, built on Spring Boot 4.1.2) offers two document readers relevant to us:

- `PagePdfDocumentReader` (`spring-ai-pdf-document-reader`, based on PDFBox): one Spring AI `Document` per page, with
  `page_number` and `file_name` in the metadata.
- `TikaDocumentReader` (`spring-ai-tika-document-reader`): many formats, one `Document` for the whole file, no page
  metadata. In Spring AI 2.0.x it depends on Apache Tika 3.3.1.

The parent POM already manages `tika-core` 4.1.0, used by document-service for content-type detection.
Adding `TikaDocumentReader` would put Tika 3.x parsers next to `tika-core` 4.x on the same classpath, which is not a
supported combination.
Spring AI 2.1 moves to Tika 4.0, but on 2026-11-11 it was still a release candidate and requires Spring Boot 4.2.

We must decide how knowledge-service extracts text from PDF and DOCX files.

## Decision drivers

- Page-level metadata for PDF (needed for citations).
- A consistent Apache Tika version across the build (no Tika 3 and Tika 4 mix).
- Stay on released versions: Spring Boot 4.1.2 and Spring AI 2.0.2, no release candidates.
- Supported dependencies: the Tika 3.x line reaches end of support in March 2027.
- Low implementation and maintenance effort for a single engineer.

## Considered options

1. **Spring AI `TikaDocumentReader` for both formats.**
2. **Spring AI `PagePdfDocumentReader` for PDF + own Tika 4 adapter for DOCX.**
3. **Own Tika 4 adapter for both formats.**
4. **Upgrade to Spring AI 2.1 and use its Tika 4 based `TikaDocumentReader`.**

## Decision outcome

Chosen option: "Spring AI `PagePdfDocumentReader` for PDF + own Tika 4 adapter for DOCX", because it gives page-level
metadata for PDF out of the box, keeps a single Tika version (4.x) in the build and uses only released versions.

The application layer depends on a single port for text extraction; two adapters implement it and are selected by
content type. The domain and application layers do not use Spring AI's `Document` class: the adapters translate it to
our own model.

## Consequences

### Positive

- PDF chunks carry `page_number`, so answers can cite the page.
- One Tika version (4.x) for the whole build, managed in the parent POM.
- No release candidates: Spring Boot and Spring AI stay on their current released versions.
- The port isolates the rest of knowledge-service from the reader libraries: replacing an adapter later (for example
  with Spring AI 2.1's reader) does not change the application layer.

### Negative / trade-offs

- Two extraction implementations instead of one; accepted because each is small and both sit behind the same port.
- The DOCX adapter is our own code to maintain and test; mitigated by a unit test with a small DOCX file in
  `src/test/resources`.
- DOCX has no fixed pages, so DOCX chunks have no page number; accepted, citations for DOCX point to the document only.
- Neither adapter performs OCR: scanned PDFs (images without a text layer) produce empty pages; mitigated by detecting
  empty extracted text and publishing `DocumentFailed` with a clear reason instead of marking the document `READY`
  with zero chunks.

## Pros and cons of the options

### Spring AI `TikaDocumentReader` for both formats

- ✅ Good, because one reader covers both formats with almost no code.
- ❌ Bad, because in Spring AI 2.0.x it brings Tika 3.3.1, which conflicts with `tika-core` 4.1.0 managed in the parent
  POM.
- ❌ Bad, because it returns the whole file as one document, without page metadata, so PDF citations would lose the page.
- ❌ Bad, because the Tika 3.x line reaches end of support in March 2027.

### Spring AI `PagePdfDocumentReader` for PDF + own Tika 4 adapter for DOCX

- ✅ Good, because PDF pages come with `page_number` out of the box.
- ✅ Good, because the build keeps a single Tika version (4.x).
- ✅ Good, because it uses only released versions.
- ❌ Bad, because there are two extraction implementations and the DOCX one is our own code.

### Own Tika 4 adapter for both formats

- ✅ Good, because one library and one implementation for both formats.
- ❌ Bad, because page boundaries for PDF are not a simple API call: they require custom handling of Tika's XHTML output,
  which is more code to write and test than using `PagePdfDocumentReader`.

### Upgrade to Spring AI 2.1

- ✅ Good, because `TikaDocumentReader` would use Tika 4 and could cover DOCX without our own adapter.
- ❌ Bad, because Spring AI 2.1 was a release candidate and requires Spring Boot 4.2, also not released at decision time.
- ❌ Bad, because it would still not give page metadata for PDF.

## More information

- Planned: upgrade to Spring AI 2.1 as soon as it and Spring Boot 4.2 are GA; then replace the DOCX adapter.
- Training, tab 20 "knowledge-service, pas cu pas", part 2: details verified in the Spring AI v2.0.2 source.
- Apache Tika roadmap (version support table): https://tika.apache.org/docs/4.0.x/roadmap.html

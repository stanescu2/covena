CREATE TABLE documents
(
    id             UUID PRIMARY KEY,
    file_name      VARCHAR(255)             NOT NULL,
    content_type   VARCHAR(20)              NOT NULL,
    file_size      BIGINT                   NOT NULL,
    uploaded_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    status         VARCHAR(20)              NOT NULL,
    failure_reason TEXT,
    version        BIGINT                   NOT NULL,
    CONSTRAINT chk_documents_failure_reason CHECK ((status <> 'FAILED' AND failure_reason IS NULL) OR
                                                   (status = 'FAILED' AND failure_reason IS NOT NULL AND
                                                    btrim(failure_reason) <> ''))
);

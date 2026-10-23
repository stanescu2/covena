CREATE TABLE outbox_events
(
    id             UUID PRIMARY KEY,
    aggregate_type VARCHAR(50)              NOT NULL,
    aggregate_id   UUID                     NOT NULL,
    event_type     VARCHAR(100)             NOT NULL,
    payload        JSONB                    NOT NULL,
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    published_at   TIMESTAMP WITH TIME ZONE NULL
);
CREATE INDEX idx_outbox_events_unpublished ON outbox_events (created_at) WHERE published_at IS NULL;

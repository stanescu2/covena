package io.covena.document.domain;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DocumentIdTest {
    @Test
    void shouldRejectNullUuid() {
        var ex = assertThrows(IllegalArgumentException.class, () -> new DocumentId(null));
        assertEquals("UUID cannot be null", ex.getMessage());
    }

    @Test
    void shouldPreserveUuidValue() {
        UUID uuid = UUID.randomUUID();
        DocumentId documentId = new DocumentId(uuid);
        assertEquals(uuid, documentId.uuidValue());
    }

    @Test
    void shouldGenerateNonNullId() {
        assertNotNull(DocumentId.newId());
    }

    @Test
    void shouldGenerateDistinctIds() {
        assertNotEquals(DocumentId.newId(), DocumentId.newId());
    }

    @Test
    void shouldBeEqualWhenUuidIsTheSame() {
        UUID uuid = UUID.randomUUID();
        DocumentId documentId1 = new DocumentId(uuid);
        DocumentId documentId2 = new DocumentId(uuid);
        assertEquals(documentId1, documentId2);
    }

    @Test
    void shouldBeSerializable() {
        DocumentId documentId = new DocumentId(UUID.randomUUID());
        DocumentId serializedDeserializedId = assertDoesNotThrow(() -> serializeDeserializeDocumentId(documentId));
        assertEquals(documentId, serializedDeserializedId);
    }

    DocumentId serializeDeserializeDocumentId(DocumentId documentId) throws IOException, ClassNotFoundException {

        var baos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(documentId);
        }
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()))) {
            return (DocumentId) ois.readObject();
        }
    }
}

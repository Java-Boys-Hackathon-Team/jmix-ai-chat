package ru.javaboys.jmixaichat.repository;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EmbeddingRepositoryTest {

    @Test
    void serializesVectorInPgvectorFormat() {
        assertEquals("[0.25,-0.5,1.0]", EmbeddingRepository.vectorLiteral(new float[]{0.25f, -0.5f, 1.0f}));
    }

    @Test
    void rejectsNonFiniteCoordinates() {
        assertThrows(IllegalArgumentException.class,
                () -> EmbeddingRepository.vectorLiteral(new float[]{1.0f, Float.NaN}));
    }
}

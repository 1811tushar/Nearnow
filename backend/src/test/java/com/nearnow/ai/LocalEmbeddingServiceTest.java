package com.nearnow.ai;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LocalEmbeddingServiceTest {
    private final LocalEmbeddingService service = new LocalEmbeddingService();

    @Test void embeddingIsDeterministicAndFixedSize() {
        float[] a = service.embed("fresh apples red");
        float[] b = service.embed("fresh apples red");
        assertEquals(LocalEmbeddingService.DIMENSIONS, a.length);
        assertArrayEquals(a, b);
    }

    @Test void blankInputProducesZeroVector() {
        float[] a = service.embed("   ");
        float sum = 0f; for (float v : a) sum += v; assertEquals(0.0f, sum);
    }
}

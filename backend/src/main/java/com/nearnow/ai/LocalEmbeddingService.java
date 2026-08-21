package com.nearnow.ai;

import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Zero-cost, deterministic local text embedding.
 *
 * This deliberately uses no external AI API and no downloaded model. Tokens
 * are hashed into a fixed 128-dimensional vector and normalized. It is not
 * a transformer model; it is a lightweight local vector representation that
 * keeps pgvector-based ranking fully runnable offline and free.
 */
@Service
public class LocalEmbeddingService {
    public static final int DIMENSIONS = 128;
    private static final Pattern NON_WORD = Pattern.compile("[^a-z0-9]+", Pattern.CASE_INSENSITIVE);

    public float[] embed(String text) {
        float[] vector = new float[DIMENSIONS];
        if (text == null || text.isBlank()) return vector;

        String normalized = Normalizer.normalize(text.toLowerCase(Locale.ROOT), Normalizer.Form.NFKD);
        String[] tokens = NON_WORD.split(normalized);
        for (String token : tokens) {
            if (token.isBlank()) continue;
            int h = token.hashCode();
            int index = Math.floorMod(h, DIMENSIONS);
            vector[index] += 1.0f;
            // Character n-grams add some similarity for related word forms.
            for (int i = 0; i + 2 < token.length(); i++) {
                int gramHash = (token.substring(i, i + 3) + "#").hashCode();
                vector[Math.floorMod(gramHash, DIMENSIONS)] += 0.25f;
            }
        }

        double norm = 0.0;
        for (float value : vector) norm += value * value;
        if (norm == 0.0) return vector;
        float scale = (float) (1.0 / Math.sqrt(norm));
        for (int i = 0; i < vector.length; i++) vector[i] *= scale;
        return vector;
    }
}

package com.nearnow.ai;

import com.nearnow.product.ProductResponseDTO;
import com.nearnow.product.ProductService;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@Service
public class SemanticSearchService {
    private final LocalEmbeddingService embeddingService;
    private final ProductEmbeddingRepository productEmbeddingRepository;
    private final ProductService productService;

    public SemanticSearchService(LocalEmbeddingService embeddingService,
                                 ProductEmbeddingRepository productEmbeddingRepository,
                                 ProductService productService) {
        this.embeddingService = embeddingService;
        this.productEmbeddingRepository = productEmbeddingRepository;
        this.productService = productService;
    }

    public List<ProductResponseDTO> search(String query, int limit) {
        float[] queryEmbedding = embeddingService.embed(query);
        List<Long> rankedIds = productEmbeddingRepository.findNearestProductIds(queryEmbedding, limit);
        if (rankedIds.isEmpty()) {
            return productService.searchProducts(query, PageRequest.of(0, limit, Sort.by("name").ascending())).getContent();
        }

        Map<Long, ProductResponseDTO> byId = new HashMap<>();
        for (ProductResponseDTO p : productService.getProductsByIds(rankedIds)) byId.put(p.getId(), p);
        return rankedIds.stream().map(byId::get).filter(p -> p != null).toList();
    }
}

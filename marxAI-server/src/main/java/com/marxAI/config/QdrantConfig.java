package com.marxAI.config;

import com.marxAI.exception.VectorStoreException;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.qdrant.QdrantEmbeddingStore;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import io.qdrant.client.grpc.Collections.Distance;
import io.qdrant.client.grpc.Collections.VectorParams;
import java.util.concurrent.ExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the Qdrant gRPC client and the {@link EmbeddingStore} bean that {@code QdrantService}
 * reads/writes chunk vectors through. The collection backing the store is created on startup if
 * it doesn't already exist, sized for Gemini's {@code text-embedding-004} (768 dimensions)
 * with cosine distance.
 */
@Configuration
@EnableConfigurationProperties(QdrantProperties.class)
public class QdrantConfig {

    private static final Logger log = LoggerFactory.getLogger(QdrantConfig.class);

    /** Output size of Gemini's {@code text-embedding-004}, the model {@code EmbeddingService} calls. */
    static final int EMBEDDING_DIMENSIONS = 768;

    @Bean(destroyMethod = "close")
    public QdrantClient qdrantClient(QdrantProperties properties) {
        return new QdrantClient(QdrantGrpcClient.newBuilder(properties.host(), properties.grpcPort(), false)
                .build());
    }

    @Bean
    public EmbeddingStore<TextSegment> embeddingStore(QdrantClient qdrantClient, QdrantProperties properties) {
        ensureCollectionExists(qdrantClient, properties.collectionName());
        return QdrantEmbeddingStore.builder()
                .client(qdrantClient)
                .collectionName(properties.collectionName())
                .build();
    }

    private void ensureCollectionExists(QdrantClient client, String collectionName) {
        try {
            if (Boolean.TRUE.equals(client.collectionExistsAsync(collectionName).get())) {
                return;
            }
            client.createCollectionAsync(
                            collectionName,
                            VectorParams.newBuilder()
                                    .setSize(EMBEDDING_DIMENSIONS)
                                    .setDistance(Distance.Cosine)
                                    .build())
                    .get();
            log.info("Created Qdrant collection '{}'", collectionName);
        } catch (InterruptedException | ExecutionException ex) {
            throw new VectorStoreException("Failed to verify/create Qdrant collection '" + collectionName + "'", ex);
        }
    }
}

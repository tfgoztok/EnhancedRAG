package cs544.project.EnhancedRAG.config;

import cs544.project.EnhancedRAG.model.DocumentType;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.redis.RedisVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import redis.clients.jedis.JedisPooled;

import java.util.Map;
import java.util.HashMap;

@Configuration
public class MultiStoreVectorConfiguration {

    @Bean
    @Primary
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        return template;
    }

    @Bean
    public JedisPooled jedisPooled() {
        return new JedisPooled("localhost", 6379);
    }

    @Bean("pdfVectorStore")
    public VectorStore pdfVectorStore(EmbeddingModel embeddingModel, JedisPooled jedisPooled) {
        return createRedisVectorStore(embeddingModel, jedisPooled, DocumentType.PDF);
    }

    @Bean("markdownVectorStore")
    public VectorStore markdownVectorStore(EmbeddingModel embeddingModel, JedisPooled jedisPooled) {
        return createRedisVectorStore(embeddingModel, jedisPooled, DocumentType.MARKDOWN);
    }

    @Bean("jsonVectorStore")
    public VectorStore jsonVectorStore(EmbeddingModel embeddingModel, JedisPooled jedisPooled) {
        return createRedisVectorStore(embeddingModel, jedisPooled, DocumentType.JSON);
    }

    @Bean("textVectorStore")
    public VectorStore textVectorStore(EmbeddingModel embeddingModel, JedisPooled jedisPooled) {
        return createRedisVectorStore(embeddingModel, jedisPooled, DocumentType.TEXT);
    }

    @Bean
    public Map<DocumentType, VectorStore> vectorStoreMap(
            @Qualifier("pdfVectorStore") VectorStore pdfVectorStore,
            @Qualifier("markdownVectorStore") VectorStore markdownVectorStore,
            @Qualifier("jsonVectorStore") VectorStore jsonVectorStore,
            @Qualifier("textVectorStore") VectorStore textVectorStore) {
        
        Map<DocumentType, VectorStore> storeMap = new HashMap<>();
        storeMap.put(DocumentType.PDF, pdfVectorStore);
        storeMap.put(DocumentType.MARKDOWN, markdownVectorStore);
        storeMap.put(DocumentType.JSON, jsonVectorStore);
        storeMap.put(DocumentType.TEXT, textVectorStore);
        
        return storeMap;
    }

    private VectorStore createRedisVectorStore(EmbeddingModel embeddingModel, 
                                             JedisPooled jedisPooled, 
                                             DocumentType documentType) {
        
        return RedisVectorStore.builder(jedisPooled, embeddingModel)
                .indexName(documentType.getIndexName())
                .prefix(documentType.getPrefix())
                .initializeSchema(true)
                .build();
    }
}

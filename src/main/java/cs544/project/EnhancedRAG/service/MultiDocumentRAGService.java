package cs544.project.EnhancedRAG.service;

import cs544.project.EnhancedRAG.model.DocumentSource;
import cs544.project.EnhancedRAG.model.DocumentType;
import cs544.project.EnhancedRAG.model.MultiDocumentResponse;
import cs544.project.EnhancedRAG.model.StoreStatus;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import redis.clients.jedis.JedisPooled;

import java.util.*;
import java.util.logging.Logger;

@Service
public class MultiDocumentRAGService {

    private static final Logger logger = Logger.getLogger(MultiDocumentRAGService.class.getName());
    
    private final ChatClient chatClient;
    private final Advisor multiDocumentRetrievalAdvisor;
    private final Map<DocumentType, VectorStore> vectorStores;
    private final RedisTemplate<String, Object> redisTemplate;
    private final JedisPooled jedisPooled;

    public MultiDocumentRAGService(ChatClient chatClient, 
                                 Advisor multiDocumentRetrievalAdvisor,
                                 Map<DocumentType, VectorStore> vectorStores,
                                 RedisTemplate<String, Object> redisTemplate,
                                 JedisPooled jedisPooled) {
        this.chatClient = chatClient;
        this.multiDocumentRetrievalAdvisor = multiDocumentRetrievalAdvisor;
        this.vectorStores = vectorStores;
        this.redisTemplate = redisTemplate;
        this.jedisPooled = jedisPooled;
    }

    public MultiDocumentResponse queryMultipleStores(String question) {
        logger.info("Processing multi-document query: " + question);
        
        try {
            // Use the single multi-document advisor to get answer from all stores
            String answer = chatClient.prompt()
                    .user(question)
                    .advisors(multiDocumentRetrievalAdvisor)
                    .call()
                    .content();
            
            if (answer == null || answer.trim().isEmpty()) {
                return new MultiDocumentResponse(
                    "I couldn't find relevant information in any of the document stores to answer your question.",
                    new ArrayList<>(),
                    new HashMap<>(),
                    0.0
                );
            }
            
            // Create a simple source attribution since we're using a single advisor
            List<DocumentSource> allSources = new ArrayList<>();
            Map<String, Integer> sourceBreakdown = new HashMap<>();
            
            // Create a single source entry representing the multi-document search
            DocumentSource source = new DocumentSource(
                    "multi-document",
                    answer,
                    0.8, // Default confidence
                    "multi-document-rag",
                    "Retrieved using single RetrievalAugmentationAdvisor across all stores"
            );
            allSources.add(source);
            sourceBreakdown.put("multi-document", 1);
            
            logger.info("Successfully generated multi-document response");
            
            return new MultiDocumentResponse(answer, allSources, sourceBreakdown, 0.8);
            
        } catch (Exception e) {
            logger.severe("Error processing multi-document query: " + e.getMessage());
            return new MultiDocumentResponse(
                "An error occurred while processing your question: " + e.getMessage(),
                new ArrayList<>(),
                new HashMap<>(),
                0.0
            );
        }
    }

    public StoreStatus getStoreStatus() {
        Map<DocumentType, Integer> documentCounts = new HashMap<>();
        Map<DocumentType, Boolean> storeHealth = new HashMap<>();
        
        for (DocumentType type : DocumentType.values()) {
            try {
                VectorStore store = vectorStores.get(type);
                if (store != null) {
                    // For demo purposes, we'll estimate document counts
                    // In a real implementation, you'd query the actual store
                    documentCounts.put(type, getEstimatedDocumentCount(type));
                    storeHealth.put(type, true);
                } else {
                    documentCounts.put(type, 0);
                    storeHealth.put(type, false);
                }
            } catch (Exception e) {
                logger.warning("Health check failed for " + type + " store: " + e.getMessage());
                documentCounts.put(type, 0);
                storeHealth.put(type, false);
            }
        }
        
        String redisStatus = checkRedisConnection();
        
        return new StoreStatus(documentCounts, storeHealth, redisStatus);
    }

    private String checkRedisConnection() {
        try {
            redisTemplate.hasKey("health-check");
            return "Connected";
        } catch (Exception e) {
            return "Disconnected: " + e.getMessage();
        }
    }

    private int getEstimatedDocumentCount(DocumentType type) {
        try {
            // Query Redis directly using FT.INFO command to get real document counts
            Object result = jedisPooled.sendCommand(
                redis.clients.jedis.Protocol.Command.valueOf("FT.INFO"),
                type.getIndexName()
            );
            
            if (result instanceof List<?>) {
                List<?> infoList = (List<?>) result;
                // Parse the FT.INFO response to extract document count
                // The response format typically includes "num_docs" field
                for (int i = 0; i < infoList.size() - 1; i++) {
                    if ("num_docs".equals(String.valueOf(infoList.get(i)))) {
                        return Integer.parseInt(String.valueOf(infoList.get(i + 1)));
                    }
                }
            }
            
            logger.warning("Could not parse document count from FT.INFO response for " + type);
            return 0;
            
        } catch (Exception e) {
            // Fall back to counting keys with the document prefix
            try {
                Set<String> keys = jedisPooled.keys(type.getPrefix() + "*");
                int count = keys != null ? keys.size() : 0;
                logger.info("Retrieved embedding count for " + type + " using key pattern: " + count);
                return count;
            } catch (Exception fallbackException) {
                logger.warning("Failed to get document count for " + type + 
                             ". FT.INFO error: " + e.getMessage() + 
                             ", Keys fallback error: " + fallbackException.getMessage());
                return 0;
            }
        }
    }

    public List<String> getDemoQueries() {
        return Arrays.asList(
            "How do I implement JWT authentication in Spring Boot and what are the security considerations?",
            "What are the recommended database connection pool settings for high-traffic applications?",
            "How do I set up caching with Redis in Spring Boot and monitor its effectiveness?"
        );
    }

    public MultiDocumentResponse processDemoQuery(int queryIndex) {
        List<String> demoQueries = getDemoQueries();
        if (queryIndex < 0 || queryIndex >= demoQueries.size()) {
            throw new IllegalArgumentException("Invalid query index");
        }
        
        return queryMultipleStores(demoQueries.get(queryIndex));
    }
}

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
    private final Map<DocumentType, Advisor> ragAdvisorMap;
    private final Map<DocumentType, VectorStore> vectorStores;
    private final RedisTemplate<String, Object> redisTemplate;
    private final JedisPooled jedisPooled;

    public MultiDocumentRAGService(ChatClient chatClient, 
                                 Map<DocumentType, Advisor> ragAdvisorMap,
                                 Map<DocumentType, VectorStore> vectorStores,
                                 RedisTemplate<String, Object> redisTemplate,
                                 JedisPooled jedisPooled) {
        this.chatClient = chatClient;
        this.ragAdvisorMap = ragAdvisorMap;
        this.vectorStores = vectorStores;
        this.redisTemplate = redisTemplate;
        this.jedisPooled = jedisPooled;
    }

    public MultiDocumentResponse queryMultipleStores(String question) {
        logger.info("Processing multi-document query: " + question);
        
        try {
            List<DocumentSource> allSources = new ArrayList<>();
            Map<String, Integer> sourceBreakdown = new HashMap<>();
            List<String> answers = new ArrayList<>();
            
            // Query each document type using its specific advisor
            for (Map.Entry<DocumentType, Advisor> entry : ragAdvisorMap.entrySet()) {
                DocumentType type = entry.getKey();
                Advisor advisor = entry.getValue();
                
                try {
                    String typeSpecificAnswer = chatClient.prompt()
                            .user(question)
                            .advisors(advisor)
                            .call()
                            .content();
                    
                    if (typeSpecificAnswer != null && !typeSpecificAnswer.trim().isEmpty()) {
                        answers.add("From " + type.name().toLowerCase() + " documents: " + typeSpecificAnswer);
                        
                        // Create a document source for tracking
                        DocumentSource source = new DocumentSource(
                                type.name().toLowerCase(),
                                typeSpecificAnswer,
                                0.8, // Default confidence since we don't have access to similarity scores
                                "RAG-generated-from-" + type.name().toLowerCase(),
                                "Retrieved using RetrievalAugmentationAdvisor"
                        );
                        allSources.add(source);
                        sourceBreakdown.merge(type.name().toLowerCase(), 1, Integer::sum);
                    }
                } catch (Exception e) {
                    logger.warning("Error querying " + type + " store: " + e.getMessage());
                }
            }
            
            if (answers.isEmpty()) {
                return new MultiDocumentResponse(
                    "I couldn't find relevant information in any of the document stores to answer your question.",
                    new ArrayList<>(),
                    new HashMap<>(),
                    0.0
                );
            }
            
            // Synthesize final answer by combining all type-specific answers
            String synthesisPrompt = buildSynthesisPrompt(question, answers);
            String finalAnswer = chatClient.prompt()
                    .user(synthesisPrompt)
                    .call()
                    .content();
            
            // Calculate average confidence
            double totalConfidence = calculateTotalConfidence(allSources);
            
            logger.info("Successfully generated multi-document response from " + allSources.size() + " sources");
            
            return new MultiDocumentResponse(finalAnswer, allSources, sourceBreakdown, totalConfidence);
            
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

    private String buildSynthesisPrompt(String originalQuestion, List<String> typeSpecificAnswers) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are tasked with synthesizing information from multiple document types to provide a comprehensive answer.\n\n");
        prompt.append("Original Question: ").append(originalQuestion).append("\n\n");
        prompt.append("I have gathered the following information from different document types:\n\n");
        
        for (int i = 0; i < typeSpecificAnswers.size(); i++) {
            prompt.append(i + 1).append(". ").append(typeSpecificAnswers.get(i)).append("\n\n");
        }
        
        prompt.append("Please synthesize this information into a coherent, comprehensive answer. ");
        prompt.append("Highlight the key points from each source and show how they relate to each other. ");
        prompt.append("If there are conflicting information, please note the discrepancies. ");
        prompt.append("Make your response well-structured and informative.");
        
        return prompt.toString();
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

    private double calculateTotalConfidence(List<DocumentSource> sources) {
        if (sources.isEmpty()) {
            return 0.0;
        }
        
        double totalConfidence = sources.stream()
                .mapToDouble(DocumentSource::getConfidence)
                .average()
                .orElse(0.0);
        
        // Apply a penalty for having fewer sources
        double sourcePenalty = Math.min(1.0, sources.size() / 4.0);
        
        return totalConfidence * sourcePenalty;
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

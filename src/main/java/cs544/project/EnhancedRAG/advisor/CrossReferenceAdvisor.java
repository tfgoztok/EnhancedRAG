package cs544.project.EnhancedRAG.advisor;

import cs544.project.EnhancedRAG.model.DocumentType;
import cs544.project.EnhancedRAG.model.DocumentSource;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class CrossReferenceAdvisor {

    private final Map<DocumentType, VectorStore> vectorStores;
    private final int topK;

    public CrossReferenceAdvisor(Map<DocumentType, VectorStore> vectorStores) {
        this.vectorStores = vectorStores;
        this.topK = 3; // Default top K results
    }

    public List<DocumentSource> retrieveFromAllStores(String query) {
        List<DocumentSource> allSources = new ArrayList<>();
        
        for (Map.Entry<DocumentType, VectorStore> entry : vectorStores.entrySet()) {
            DocumentType type = entry.getKey();
            VectorStore store = entry.getValue();
            
            try {
                SearchRequest searchRequest = SearchRequest.builder()
                        .query(query)
                        .topK(topK / vectorStores.size()) // Distribute topK across stores
                        .build();
                
                List<Document> documents = store.similaritySearch(searchRequest);
                
                for (Document doc : documents) {
                    DocumentSource source = new DocumentSource(
                            type.name().toLowerCase(),
                            doc.getText(),
                            calculateConfidence(doc),
                            extractDocumentName(doc),
                            doc.getMetadata().toString()
                    );
                    allSources.add(source);
                }
            } catch (Exception e) {
                // Log error but continue with other stores
                System.err.println("Error retrieving from " + type + " store: " + e.getMessage());
            }
        }
        
        // Sort by confidence and return top results
        return allSources.stream()
                .sorted((a, b) -> Double.compare(b.getConfidence(), a.getConfidence()))
                .limit(topK)
                .collect(Collectors.toList());
    }

    public String buildEnhancedPrompt(String originalQuery, List<DocumentSource> sources) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Based on the following context from multiple document types, please answer the question: ")
              .append(originalQuery).append("\n\n");
        
        prompt.append("Context from different sources:\n");
        
        Map<String, List<DocumentSource>> sourcesByType = sources.stream()
                .collect(Collectors.groupingBy(DocumentSource::getType));
        
        for (Map.Entry<String, List<DocumentSource>> entry : sourcesByType.entrySet()) {
            String type = entry.getKey();
            List<DocumentSource> typeSources = entry.getValue();
            
            prompt.append("\n--- ").append(type.toUpperCase()).append(" SOURCES ---\n");
            for (int i = 0; i < typeSources.size(); i++) {
                DocumentSource source = typeSources.get(i);
                prompt.append("Source ").append(i + 1).append(" (")
                      .append(source.getDocumentName()).append("):\n")
                      .append(source.getContent()).append("\n\n");
            }
        }
        
        prompt.append("Please synthesize information from these different document types to provide a comprehensive answer. ");
        prompt.append("Indicate which sources support different parts of your answer.");
        
        return prompt.toString();
    }

    private double calculateConfidence(Document document) {
        // Simple confidence calculation based on metadata
        Map<String, Object> metadata = document.getMetadata();
        if (metadata.containsKey("distance")) {
            Object distanceObj = metadata.get("distance");
            if (distanceObj instanceof Number) {
                double distance = ((Number) distanceObj).doubleValue();
                return Math.max(0.0, 1.0 - distance); // Convert distance to confidence
            }
        }
        return 0.8; // Default confidence
    }

    private String extractDocumentName(Document document) {
        Map<String, Object> metadata = document.getMetadata();
        if (metadata.containsKey("source")) {
            String source = metadata.get("source").toString();
            return source.substring(source.lastIndexOf('/') + 1);
        }
        if (metadata.containsKey("filename")) {
            return metadata.get("filename").toString();
        }
        return "unknown-document";
    }

    public Map<String, Integer> calculateSourceBreakdown(List<DocumentSource> sources) {
        Map<String, Integer> breakdown = new HashMap<>();
        for (DocumentSource source : sources) {
            breakdown.merge(source.getType(), 1, Integer::sum);
        }
        return breakdown;
    }
}

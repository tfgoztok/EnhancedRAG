package cs544.project.EnhancedRAG.service;

import cs544.project.EnhancedRAG.model.DocumentType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Logger;

@Service
public class DocumentIngestionService {

    private static final Logger logger = Logger.getLogger(DocumentIngestionService.class.getName());
    
    private final Map<DocumentType, VectorStore> vectorStores;
    private final ObjectMapper objectMapper;
    private final PathMatchingResourcePatternResolver resolver;

    public DocumentIngestionService(Map<DocumentType, VectorStore> vectorStores) {
        this.vectorStores = vectorStores;
        this.objectMapper = new ObjectMapper();
        this.resolver = new PathMatchingResourcePatternResolver();
    }

    public void ingestAllDocuments() {
        logger.info("Starting document ingestion process...");
        
        ingestDocumentsByType(DocumentType.PDF, "classpath:documents/pdf/**/*.pdf");
        ingestDocumentsByType(DocumentType.MARKDOWN, "classpath:documents/markdown/**/*.md");
        ingestDocumentsByType(DocumentType.JSON, "classpath:documents/json/**/*.json");
        ingestDocumentsByType(DocumentType.TEXT, "classpath:documents/text/**/*.txt");
        
        logger.info("Document ingestion completed!");
    }

    private void ingestDocumentsByType(DocumentType type, String pattern) {
        try {
            Resource[] resources = resolver.getResources(pattern);
            VectorStore vectorStore = vectorStores.get(type);
            
            if (vectorStore == null) {
                logger.warning("Vector store for type " + type + " not found!");
                return;
            }

            List<Document> documents = new ArrayList<>();
            
            for (Resource resource : resources) {
                try {
                    String content = extractContent(resource, type);
                    if (content != null && !content.trim().isEmpty()) {
                        Map<String, Object> metadata = createMetadata(resource, type);
                        Document document = new Document(content, metadata);
                        documents.add(document);
                        logger.info("Prepared document: " + resource.getFilename());
                    }
                } catch (Exception e) {
                    logger.warning("Failed to process " + resource.getFilename() + ": " + e.getMessage());
                }
            }

            if (!documents.isEmpty()) {
                vectorStore.add(documents);
                logger.info("Added " + documents.size() + " documents to " + type + " store");
            } else {
                logger.info("No documents found for pattern: " + pattern);
            }
            
        } catch (IOException e) {
            logger.severe("Failed to load resources for pattern " + pattern + ": " + e.getMessage());
        }
    }

    private String extractContent(Resource resource, DocumentType type) throws IOException {
        try (InputStream inputStream = resource.getInputStream()) {
            switch (type) {
                case PDF:
                    return extractPdfContent(inputStream);
                case JSON:
                    return extractJsonContent(inputStream);
                case MARKDOWN:
                case TEXT:
                default:
                    return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            }
        }
    }

    private String extractPdfContent(InputStream inputStream) throws IOException {
        try {
            // For demo purposes, since we're using text file as PDF placeholder
            // In real implementation, you'd use PDFBox properly
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            // If PDF parsing fails, return a placeholder content
            return "PDF content could not be extracted: " + e.getMessage();
        }
    }

    private String extractJsonContent(InputStream inputStream) throws IOException {
        JsonNode jsonNode = objectMapper.readTree(inputStream);
        // For JSON documents, we'll create a formatted string representation
        StringBuilder content = new StringBuilder();
        
        if (jsonNode.isObject()) {
            jsonNode.fieldNames().forEachRemaining(fieldName -> {
                JsonNode fieldValue = jsonNode.get(fieldName);
                content.append(fieldName).append(": ").append(fieldValue.asText()).append("\\n");
            });
        } else {
            content.append(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonNode));
        }
        
        return content.toString();
    }

    private Map<String, Object> createMetadata(Resource resource, DocumentType type) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("source", resource.toString());
        metadata.put("filename", resource.getFilename());
        metadata.put("document_type", type.name().toLowerCase());
        metadata.put("ingestion_timestamp", System.currentTimeMillis());
        
        try {
            metadata.put("content_length", resource.contentLength());
        } catch (IOException e) {
            metadata.put("content_length", -1);
        }
        
        return metadata;
    }

    public int getDocumentCount(DocumentType type) {
        // This is a simplified count - in a real implementation you might query the vector store
        try {
            String pattern = getPatternForType(type);
            Resource[] resources = resolver.getResources(pattern);
            return resources.length;
        } catch (IOException e) {
            logger.warning("Failed to count documents for type " + type + ": " + e.getMessage());
            return 0;
        }
    }

    private String getPatternForType(DocumentType type) {
        switch (type) {
            case PDF:
                return "classpath:documents/pdf/**/*.pdf";
            case MARKDOWN:
                return "classpath:documents/markdown/**/*.md";
            case JSON:
                return "classpath:documents/json/**/*.json";
            case TEXT:
                return "classpath:documents/text/**/*.txt";
            default:
                return "classpath:documents/**/*";
        }
    }

    public void ingestSingleDocument(DocumentType type, String filename, String content) {
        VectorStore vectorStore = vectorStores.get(type);
        if (vectorStore == null) {
            throw new IllegalArgumentException("Vector store for type " + type + " not found!");
        }

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("source", "manual-upload");
        metadata.put("filename", filename);
        metadata.put("document_type", type.name().toLowerCase());
        metadata.put("ingestion_timestamp", System.currentTimeMillis());
        metadata.put("content_length", content.length());

        Document document = new Document(content, metadata);
        vectorStore.add(List.of(document));
        
        logger.info("Successfully ingested single document: " + filename + " to " + type + " store");
    }
}

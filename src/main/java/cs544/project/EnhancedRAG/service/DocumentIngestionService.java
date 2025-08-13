package cs544.project.EnhancedRAG.service;

import cs544.project.EnhancedRAG.model.DocumentType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.Loader;
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
    
    // Chunking configuration
    private static final int MAX_CHUNK_SIZE = 6000; // Conservative limit for text-embedding-ada-002
    private static final int CHUNK_OVERLAP = 200;   // Overlap between chunks to maintain context
    
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
                        // Create base metadata for all chunks
                        Map<String, Object> baseMetadata = createMetadata(resource, type);
                        
                        // Split content into chunks if it's too large
                        List<String> chunks = chunkContent(content);
                        
                        for (int i = 0; i < chunks.size(); i++) {
                            String chunk = chunks.get(i);
                            if (!chunk.trim().isEmpty()) {
                                // Create metadata for each chunk
                                Map<String, Object> chunkMetadata = new HashMap<>(baseMetadata);
                                chunkMetadata.put("chunk_index", i);
                                chunkMetadata.put("total_chunks", chunks.size());
                                chunkMetadata.put("chunk_size", chunk.length());
                                
                                Document document = new Document(chunk, chunkMetadata);
                                documents.add(document);
                            }
                        }
                        
                        logger.info("Prepared document: " + resource.getFilename() + 
                                   " (split into " + chunks.size() + " chunks)");
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
            // For PDFBox 3.0.1, use the Loader class
            try (PDDocument document = Loader.loadPDF(inputStream.readAllBytes())) {
                PDFTextStripper stripper = new PDFTextStripper();
                
                // Configure PDFTextStripper to be more lenient with font issues
                stripper.setSortByPosition(true);
                stripper.setSuppressDuplicateOverlappingText(false);
                
                return stripper.getText(document);
            }
        } catch (Exception e) {
            // If PDF parsing fails, log the error and return a meaningful message
            logger.severe("Failed to extract PDF content: " + e.getMessage());
            return "PDF content could not be extracted: " + e.getMessage();
        }
    }

    /**
     * Splits content into manageable chunks to avoid token limits
     */
    private List<String> chunkContent(String content) {
        List<String> chunks = new ArrayList<>();
        
        if (content == null || content.trim().isEmpty()) {
            return chunks;
        }
        
        // If content is smaller than max chunk size, return as single chunk
        if (content.length() <= MAX_CHUNK_SIZE) {
            chunks.add(content);
            return chunks;
        }
        
        // Split content into chunks
        int start = 0;
        while (start < content.length()) {
            int end = Math.min(start + MAX_CHUNK_SIZE, content.length());
            
            // Try to break at word boundaries to maintain context
            if (end < content.length()) {
                int lastSpaceInChunk = content.lastIndexOf(' ', end);
                int lastNewlineInChunk = content.lastIndexOf('\n', end);
                int lastPeriodInChunk = content.lastIndexOf('.', end);
                
                // Find the best breaking point
                int breakPoint = Math.max(lastSpaceInChunk, Math.max(lastNewlineInChunk, lastPeriodInChunk));
                if (breakPoint > start + (MAX_CHUNK_SIZE / 2)) { // Don't break too early
                    end = breakPoint + 1;
                }
            }
            
            String chunk = content.substring(start, end).trim();
            if (!chunk.isEmpty()) {
                chunks.add(chunk);
            }
            
            // Move start position with overlap for context continuity
            start = Math.max(start + 1, end - CHUNK_OVERLAP);
        }
        
        return chunks;
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

        // Create base metadata
        Map<String, Object> baseMetadata = new HashMap<>();
        baseMetadata.put("source", "manual-upload");
        baseMetadata.put("filename", filename);
        baseMetadata.put("document_type", type.name().toLowerCase());
        baseMetadata.put("ingestion_timestamp", System.currentTimeMillis());
        baseMetadata.put("content_length", content.length());

        // Split content into chunks if necessary
        List<String> chunks = chunkContent(content);
        List<Document> documents = new ArrayList<>();
        
        for (int i = 0; i < chunks.size(); i++) {
            String chunk = chunks.get(i);
            if (!chunk.trim().isEmpty()) {
                Map<String, Object> chunkMetadata = new HashMap<>(baseMetadata);
                chunkMetadata.put("chunk_index", i);
                chunkMetadata.put("total_chunks", chunks.size());
                chunkMetadata.put("chunk_size", chunk.length());
                
                Document document = new Document(chunk, chunkMetadata);
                documents.add(document);
            }
        }
        
        vectorStore.add(documents);
        
        logger.info("Successfully ingested single document: " + filename + " to " + type + 
                   " store (split into " + chunks.size() + " chunks)");
    }
}

package cs544.project.EnhancedRAG.controller;

import cs544.project.EnhancedRAG.model.DocumentType;
import cs544.project.EnhancedRAG.model.MultiDocumentResponse;
import cs544.project.EnhancedRAG.model.StoreStatus;
import cs544.project.EnhancedRAG.service.DocumentIngestionService;
import cs544.project.EnhancedRAG.service.MultiDocumentRAGService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

@RestController
@RequestMapping("/api/rag")
@CrossOrigin(origins = "*")
public class RAGController {

    private static final Logger logger = Logger.getLogger(RAGController.class.getName());
    
    private final DocumentIngestionService documentIngestionService;
    private final MultiDocumentRAGService multiDocumentRAGService;

    public RAGController(DocumentIngestionService documentIngestionService,
                        MultiDocumentRAGService multiDocumentRAGService) {
        this.documentIngestionService = documentIngestionService;
        this.multiDocumentRAGService = multiDocumentRAGService;
    }

    @PostMapping("/ingest")
    public ResponseEntity<Map<String, String>> ingestDocuments() {
        logger.info("Starting document ingestion via API");
        
        try {
            documentIngestionService.ingestAllDocuments();
            
            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "All documents have been successfully ingested into their respective vector stores");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.severe("Error during document ingestion: " + e.getMessage());
            
            Map<String, String> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Failed to ingest documents: " + e.getMessage());
            
            return ResponseEntity.status(500).body(response);
        }
    }

    @PostMapping("/ingest/single")
    public ResponseEntity<Map<String, String>> ingestSingleDocument(
            @RequestParam DocumentType type,
            @RequestParam String filename,
            @RequestBody String content) {
        
        logger.info("Ingesting single document: " + filename + " of type: " + type);
        
        try {
            documentIngestionService.ingestSingleDocument(type, filename, content);
            
            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Document successfully ingested into " + type + " store");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.severe("Error ingesting single document: " + e.getMessage());
            
            Map<String, String> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Failed to ingest document: " + e.getMessage());
            
            return ResponseEntity.status(500).body(response);
        }
    }

    @PostMapping("/query")
    public ResponseEntity<MultiDocumentResponse> queryMultipleStores(@RequestBody Map<String, String> request) {
        String question = request.get("question");
        
        if (question == null || question.trim().isEmpty()) {
            MultiDocumentResponse errorResponse = new MultiDocumentResponse(
                "Please provide a valid question",
                List.of(),
                Map.of(),
                0.0
            );
            return ResponseEntity.badRequest().body(errorResponse);
        }
        
        logger.info("Processing query: " + question);
        
        try {
            MultiDocumentResponse response = multiDocumentRAGService.queryMultipleStores(question);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.severe("Error processing query: " + e.getMessage());
            
            MultiDocumentResponse errorResponse = new MultiDocumentResponse(
                "An error occurred while processing your query: " + e.getMessage(),
                List.of(),
                Map.of(),
                0.0
            );
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    @GetMapping("/status")
    public ResponseEntity<StoreStatus> getStoreStatus() {
        logger.info("Fetching store status");
        
        try {
            StoreStatus status = multiDocumentRAGService.getStoreStatus();
            return ResponseEntity.ok(status);
            
        } catch (Exception e) {
            logger.severe("Error fetching store status: " + e.getMessage());
            return ResponseEntity.status(500).build();
        }
    }

    @GetMapping("/demo/queries")
    public ResponseEntity<List<String>> getDemoQueries() {
        try {
            List<String> demoQueries = multiDocumentRAGService.getDemoQueries();
            return ResponseEntity.ok(demoQueries);
            
        } catch (Exception e) {
            logger.severe("Error fetching demo queries: " + e.getMessage());
            return ResponseEntity.status(500).build();
        }
    }

    @PostMapping("/demo/query/{index}")
    public ResponseEntity<MultiDocumentResponse> processDemoQuery(@PathVariable int index) {
        logger.info("Processing demo query with index: " + index);
        
        try {
            MultiDocumentResponse response = multiDocumentRAGService.processDemoQuery(index);
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            MultiDocumentResponse errorResponse = new MultiDocumentResponse(
                "Invalid query index: " + index,
                List.of(),
                Map.of(),
                0.0
            );
            return ResponseEntity.badRequest().body(errorResponse);
            
        } catch (Exception e) {
            logger.severe("Error processing demo query: " + e.getMessage());
            
            MultiDocumentResponse errorResponse = new MultiDocumentResponse(
                "An error occurred while processing the demo query: " + e.getMessage(),
                List.of(),
                Map.of(),
                0.0
            );
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> healthCheck() {
        Map<String, String> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "Enhanced RAG Multi-Document System");
        health.put("timestamp", String.valueOf(System.currentTimeMillis()));
        
        return ResponseEntity.ok(health);
    }
}

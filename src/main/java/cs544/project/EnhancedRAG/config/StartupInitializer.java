package cs544.project.EnhancedRAG.config;

import cs544.project.EnhancedRAG.service.DocumentIngestionService;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.logging.Logger;

@Component
public class StartupInitializer {

    private static final Logger logger = Logger.getLogger(StartupInitializer.class.getName());
    
    private final DocumentIngestionService documentIngestionService;

    public StartupInitializer(DocumentIngestionService documentIngestionService) {
        this.documentIngestionService = documentIngestionService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initializeApplication() {
        logger.info("Application started - initializing document ingestion...");
        
        try {
            // Ingest all documents during startup to populate the vector stores
            documentIngestionService.ingestAllDocuments();
            logger.info("Document ingestion completed successfully during startup");
        } catch (Exception e) {
            logger.severe("Failed to ingest documents during startup: " + e.getMessage());
            // Don't fail the application startup, but log the error
        }
    }
}

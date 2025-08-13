package cs544.project.EnhancedRAG.model;

import java.util.Map;

public class StoreStatus {
    private Map<DocumentType, Integer> documentCounts;
    private Map<DocumentType, Boolean> storeHealth;
    private String redisConnectionStatus;

    public StoreStatus() {}

    public StoreStatus(Map<DocumentType, Integer> documentCounts, 
                      Map<DocumentType, Boolean> storeHealth, 
                      String redisConnectionStatus) {
        this.documentCounts = documentCounts;
        this.storeHealth = storeHealth;
        this.redisConnectionStatus = redisConnectionStatus;
    }

    // Getters and setters
    public Map<DocumentType, Integer> getDocumentCounts() {
        return documentCounts;
    }

    public void setDocumentCounts(Map<DocumentType, Integer> documentCounts) {
        this.documentCounts = documentCounts;
    }

    public Map<DocumentType, Boolean> getStoreHealth() {
        return storeHealth;
    }

    public void setStoreHealth(Map<DocumentType, Boolean> storeHealth) {
        this.storeHealth = storeHealth;
    }

    public String getRedisConnectionStatus() {
        return redisConnectionStatus;
    }

    public void setRedisConnectionStatus(String redisConnectionStatus) {
        this.redisConnectionStatus = redisConnectionStatus;
    }
}

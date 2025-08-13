package cs544.project.EnhancedRAG.model;

public class DocumentSource {
    private String type;
    private String content;
    private double confidence;
    private String documentName;
    private String metadata;

    public DocumentSource() {}

    public DocumentSource(String type, String content, double confidence, String documentName) {
        this.type = type;
        this.content = content;
        this.confidence = confidence;
        this.documentName = documentName;
    }

    public DocumentSource(String type, String content, double confidence, String documentName, String metadata) {
        this.type = type;
        this.content = content;
        this.confidence = confidence;
        this.documentName = documentName;
        this.metadata = metadata;
    }

    // Getters and setters
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }

    public String getDocumentName() {
        return documentName;
    }

    public void setDocumentName(String documentName) {
        this.documentName = documentName;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }
}

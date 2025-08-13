package cs544.project.EnhancedRAG.model;

public enum DocumentType {
    PDF("idx:pdf", "doc:pdf:"),
    MARKDOWN("idx:markdown", "doc:markdown:"),
    JSON("idx:json", "doc:json:"),
    TEXT("idx:text", "doc:text:");

    private final String indexName;
    private final String prefix;

    DocumentType(String indexName, String prefix) {
        this.indexName = indexName;
        this.prefix = prefix;
    }

    public String getIndexName() {
        return indexName;
    }

    public String getPrefix() {
        return prefix;
    }
}

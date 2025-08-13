package cs544.project.EnhancedRAG.model;

import java.util.List;
import java.util.Map;

public class MultiDocumentResponse {
    private String answer;
    private List<DocumentSource> sources;
    private Map<String, Integer> sourceBreakdown;
    private double totalConfidence;

    public MultiDocumentResponse() {}

    public MultiDocumentResponse(String answer, List<DocumentSource> sources, 
                               Map<String, Integer> sourceBreakdown, double totalConfidence) {
        this.answer = answer;
        this.sources = sources;
        this.sourceBreakdown = sourceBreakdown;
        this.totalConfidence = totalConfidence;
    }

    // Getters and setters
    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public List<DocumentSource> getSources() {
        return sources;
    }

    public void setSources(List<DocumentSource> sources) {
        this.sources = sources;
    }

    public Map<String, Integer> getSourceBreakdown() {
        return sourceBreakdown;
    }

    public void setSourceBreakdown(Map<String, Integer> sourceBreakdown) {
        this.sourceBreakdown = sourceBreakdown;
    }

    public double getTotalConfidence() {
        return totalConfidence;
    }

    public void setTotalConfidence(double totalConfidence) {
        this.totalConfidence = totalConfidence;
    }
}

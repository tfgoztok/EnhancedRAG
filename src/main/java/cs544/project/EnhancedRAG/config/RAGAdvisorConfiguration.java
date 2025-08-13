package cs544.project.EnhancedRAG.config;

import cs544.project.EnhancedRAG.model.DocumentType;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class RAGAdvisorConfiguration {

    @Bean
    public ChatClient chatClient(ChatModel chatModel) {
        return ChatClient.builder(chatModel).build();
    }

    @Bean("pdfRetrievalAdvisor")
    public Advisor pdfRetrievalAdvisor(VectorStore pdfVectorStore) {
        return RetrievalAugmentationAdvisor.builder()
                .documentRetriever(VectorStoreDocumentRetriever.builder()
                        .similarityThreshold(0.50)
                        .vectorStore(pdfVectorStore)
                        .build())
                .build();
    }

    @Bean("markdownRetrievalAdvisor")
    public Advisor markdownRetrievalAdvisor(VectorStore markdownVectorStore) {
        return RetrievalAugmentationAdvisor.builder()
                .documentRetriever(VectorStoreDocumentRetriever.builder()
                        .similarityThreshold(0.50)
                        .vectorStore(markdownVectorStore)
                        .build())
                .build();
    }

    @Bean("jsonRetrievalAdvisor")
    public Advisor jsonRetrievalAdvisor(VectorStore jsonVectorStore) {
        return RetrievalAugmentationAdvisor.builder()
                .documentRetriever(VectorStoreDocumentRetriever.builder()
                        .similarityThreshold(0.50)
                        .vectorStore(jsonVectorStore)
                        .build())
                .build();
    }

    @Bean("textRetrievalAdvisor")
    public Advisor textRetrievalAdvisor(VectorStore textVectorStore) {
        return RetrievalAugmentationAdvisor.builder()
                .documentRetriever(VectorStoreDocumentRetriever.builder()
                        .similarityThreshold(0.50)
                        .vectorStore(textVectorStore)
                        .build())
                .build();
    }

    @Bean
    public Map<DocumentType, Advisor> ragAdvisorMap(
            Advisor pdfRetrievalAdvisor,
            Advisor markdownRetrievalAdvisor,
            Advisor jsonRetrievalAdvisor,
            Advisor textRetrievalAdvisor) {
        
        Map<DocumentType, Advisor> advisorMap = new HashMap<>();
        advisorMap.put(DocumentType.PDF, pdfRetrievalAdvisor);
        advisorMap.put(DocumentType.MARKDOWN, markdownRetrievalAdvisor);
        advisorMap.put(DocumentType.JSON, jsonRetrievalAdvisor);
        advisorMap.put(DocumentType.TEXT, textRetrievalAdvisor);
        
        return advisorMap;
    }
}

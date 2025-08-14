package cs544.project.EnhancedRAG.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class RAGAdvisorConfiguration {

    @Bean
    public ChatClient chatClient(ChatModel chatModel) {
        return ChatClient.builder(chatModel).build();
    }

    @Bean
    public DocumentRetriever multiStoreDocumentRetriever(
            VectorStore pdfVectorStore,
            VectorStore markdownVectorStore,
            VectorStore jsonVectorStore,
            VectorStore textVectorStore) {
        
        return query -> {
            List<Document> allDocuments = new ArrayList<>();
            List<VectorStore> stores = List.of(pdfVectorStore, markdownVectorStore, jsonVectorStore, textVectorStore);
            
            for (VectorStore store : stores) {
                try {
                    SearchRequest searchRequest = SearchRequest.builder()
                        .query(query.toString())
                        .similarityThreshold(0.9)
                        .build();
                    List<Document> documents = store.similaritySearch(searchRequest);
                    allDocuments.addAll(documents);
                } catch (Exception e) {
                    System.err.println("Error searching store: " + e.getMessage());
                }
            }
            
            return allDocuments;
        };
    }

    @Bean
    public Advisor multiDocumentRetrievalAdvisor(DocumentRetriever multiStoreDocumentRetriever) {
        return RetrievalAugmentationAdvisor.builder()
                .documentRetriever(multiStoreDocumentRetriever)
                .queryAugmenter(ContextualQueryAugmenter.builder()
                        .allowEmptyContext(true)
                        .build())
                .build();
    }
}

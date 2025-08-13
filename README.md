# Enhanced RAG Multi-Document System

A comprehensive Multi-Document Cross-Referencing RAG system built with Spring AI and RetrievalAugmentationAdvisor, designed for academic presentation demonstrating advanced RAG capabilities.

## 🚀 Project Overview

This project demonstrates the power of **RetrievalAugmentationAdvisor** for multi-document scenarios, showing capabilities beyond the basic QuestionAnswerAdvisor that students have learned. The system can query multiple document types simultaneously and synthesize information from different sources.

## 🏗️ Architecture

### Core Components

- **4 Different Document Types with Separate Redis Vector Stores**:
  - PDF documents (`idx:pdf`) - Spring Boot/Security documentation
  - Markdown files (`idx:markdown`) - Best practices guides  
  - JSON data (`idx:json`) - Configuration examples and benchmarks
  - Plain text (`idx:text`) - FAQs and troubleshooting guides

- **RetrievalAugmentationAdvisor Configuration**: Custom advisor for multi-store querying with source attribution
- **Cross-Document Synthesis**: Combines information from multiple document types
- **Demo-Ready API**: RESTful endpoints for document ingestion and querying

## 🛠️ Technology Stack

- **Spring Boot 3.5.4** - Application framework
- **Spring AI 1.0.1** - AI integration and vector operations
- **Redis** - Vector storage with multiple indices
- **OpenAI GPT-3.5-turbo** - Language model for response generation
- **OpenAI text-embedding-ada-002** - Text embeddings
- **Maven** - Dependency management and build tool

## 📁 Project Structure

```
src/main/java/cs544/project/EnhancedRAG/
├── config/
│   └── MultiStoreVectorConfiguration.java    # Vector store configuration
├── service/
│   ├── DocumentIngestionService.java         # Document processing and ingestion
│   └── MultiDocumentRAGService.java          # Core RAG business logic
├── controller/
│   └── RAGController.java                    # REST API endpoints
├── model/
│   ├── DocumentType.java                     # Document type enumeration
│   ├── DocumentSource.java                   # Source attribution model
│   ├── MultiDocumentResponse.java            # API response model
│   └── StoreStatus.java                      # System status model
├── advisor/
│   └── CrossReferenceAdvisor.java            # Multi-document retrieval logic
└── EnhancedRagApplication.java               # Main application class

src/main/resources/
├── documents/                                 # Sample documents
│   ├── pdf/spring-security-reference.pdf
│   ├── markdown/*.md                         # Security, performance, caching guides
│   ├── json/*.json                           # Configuration examples
│   └── text/*.txt                            # FAQ and troubleshooting
├── static/index.html                         # Demo web interface
└── application.properties                    # Configuration
```

## 🎯 Demo Scenarios

### Target Queries for Presentation

1. **"How do I implement JWT authentication in Spring Boot and what are the security considerations?"**
   - Combines information from PDF docs, markdown guides, JSON configs, and text FAQs
   - Demonstrates security best practices synthesis

2. **"What are the recommended database connection pool settings for high-traffic applications?"**
   - Pulls configuration from JSON files and performance guides
   - Shows technical specification synthesis

3. **"How do I set up caching with Redis in Spring Boot and monitor its effectiveness?"**
   - Integrates setup guides, configuration examples, and monitoring practices
   - Demonstrates comprehensive implementation guidance

## 🚀 Getting Started

### Prerequisites

- Java 17 or higher (currently using Java 24)
- Docker (for Redis)
- OpenAI API key
- Maven (wrapper included)

### Setup Instructions

1. **Clone the Repository**
   ```bash
   git clone <repository-url>
   cd EnhancedRAG
   ```

2. **Start Redis**
   ```bash
   docker run --name redis-rag -p 6379:6379 -d redis:latest
   ```

3. **Configure OpenAI API Key**
   Update `src/main/resources/application.properties`:
   ```properties
   spring.ai.openai.api-key=your-api-key-here
   ```

4. **Run the Application**
   ```bash
   ./mvnw spring-boot:run
   ```

5. **Open Demo Interface**
   Navigate to: http://localhost:8080

## 📡 API Endpoints

### System Management
- `GET /api/rag/health` - Application health check
- `GET /api/rag/status` - Vector store status and document counts
- `POST /api/rag/ingest` - Ingest all sample documents

### Query Operations
- `POST /api/rag/query` - Query across all document types
  ```json
  {
    "question": "How do I implement JWT authentication?"
  }
  ```

### Demo Endpoints
- `GET /api/rag/demo/queries` - Get predefined demo queries
- `POST /api/rag/demo/query/{index}` - Execute demo query by index

## 🎭 Demo Response Format

```json
{
  "answer": "Synthesized answer combining multiple sources...",
  "sources": [
    {
      "type": "pdf",
      "content": "Relevant excerpt from PDF...",
      "confidence": 0.85,
      "documentName": "spring-security-reference.pdf",
      "metadata": "Additional source information"
    }
  ],
  "sourceBreakdown": {
    "pdf": 2,
    "markdown": 1,
    "json": 1,
    "text": 0
  },
  "totalConfidence": 0.78
}
```

## 🎯 Key Differentiators from Basic RAG

1. **Multi-Document Type Support**: Handles PDF, Markdown, JSON, and text files
2. **Source Attribution**: Clear tracking of information sources
3. **Cross-Document Synthesis**: Combines related information from different document types
4. **Confidence Scoring**: Reliability metrics for each source
5. **Type-Aware Querying**: Optimized retrieval strategies for different content types

## 📊 Sample Documents Included

### Security Documentation (PDF/Markdown)
- JWT authentication implementation
- Spring Security configuration
- Security best practices

### Performance Guides (Markdown/JSON)
- Database connection pooling
- Performance optimization settings
- Benchmarking data

### Caching Implementation (Markdown/JSON/Text)
- Redis setup and configuration
- Cache monitoring strategies
- Troubleshooting guides

## 🔧 Configuration Details

### Vector Store Configuration
- **4 separate Redis indices** for different document types
- **Optimized embedding strategies** for each content type
- **Configurable similarity thresholds** and result limits

### AI Model Configuration
- **GPT-3.5-turbo** for response generation
- **text-embedding-ada-002** for document embeddings
- **Customizable prompt templates** for multi-document synthesis

## 📈 Performance Considerations

- **Distributed querying** across multiple vector stores
- **Result ranking and merging** algorithms
- **Caching strategies** for frequent queries
- **Connection pooling** for Redis operations

## 🎪 Live Demo Features

### Web Interface
- Interactive query testing
- Real-time source breakdown visualization
- System status monitoring
- Document ingestion controls

### Presentation Ready
- **12-minute presentation** optimized
- **Clear technical demonstrations**
- **Impressive multi-document synthesis**
- **Source attribution visualization**

## 🚀 Production Considerations

For production deployment, consider:

- **Security**: Secure OpenAI API key management
- **Scalability**: Redis clustering for large document sets
- **Monitoring**: Comprehensive logging and metrics
- **Performance**: Connection pool optimization
- **Reliability**: Circuit breaker patterns for external services

## 🤝 Contributing

This is an academic demonstration project. For suggestions or improvements, please open an issue or submit a pull request.

## 📄 License

This project is developed for educational purposes as part of CS544 coursework.

---

**Ready for Demo**: This application is fully configured and ready for live demonstration, showcasing advanced Spring AI capabilities with multi-document RAG implementation.

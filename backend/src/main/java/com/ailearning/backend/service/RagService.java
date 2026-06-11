package com.ailearning.backend.service;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.parser.apache.pdfbox.ApachePdfBoxDocumentParser;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RagService {

    private final EmbeddingStore<TextSegment> embeddingStore;
    private final EmbeddingModel embeddingModel;
    
    @Value("${app.file.upload-dir:./uploads}")
    private String uploadDir;

    private final Map<Long, List<String>> userResourceMapping = new ConcurrentHashMap<>();

    public RagService(EmbeddingStore<TextSegment> embeddingStore, EmbeddingModel embeddingModel) {
        this.embeddingStore = embeddingStore;
        this.embeddingModel = embeddingModel;
    }

    public void addResourceToKnowledgeBase(Long userId, Long resourceId, String filePath, String title, String description) {
        try {
            Path path = Paths.get(filePath);
            if (!Files.exists(path)) {
                System.err.println("File not found: " + filePath);
                return;
            }

            DocumentParser parser = getDocumentParser(filePath);
            
            try (InputStream inputStream = new FileInputStream(filePath)) {
                Document document = parser.parse(inputStream);
                
                document.metadata().put("userId", userId.toString());
                document.metadata().put("resourceId", resourceId.toString());
                document.metadata().put("title", title);
                if (description != null && !description.isEmpty()) {
                    document.metadata().put("description", description);
                }

                EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                        .documentSplitter(DocumentSplitters.recursive(500, 100))
                        .embeddingModel(embeddingModel)
                        .embeddingStore(embeddingStore)
                        .build();

                ingestor.ingest(document);

                userResourceMapping.computeIfAbsent(userId, k -> new ArrayList<>()).add(resourceId.toString());
                
                System.out.println("Resource added to knowledge base: " + title);
            }
        } catch (Exception e) {
            System.err.println("Error adding resource to knowledge base: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public List<String> searchRelevantContent(Long userId, String query, int maxResults) {
        try {
            List<EmbeddingMatch<TextSegment>> matches = embeddingStore.findRelevant(
                    embeddingModel.embed(query).content(),
                    maxResults,
                    0.7
            );

            List<String> relevantContents = new ArrayList<>();
            for (EmbeddingMatch<TextSegment> match : matches) {
                TextSegment segment = match.embedded();
                String segmentUserId = segment.metadata().getString("userId");
                
                if (segmentUserId != null && segmentUserId.equals(userId.toString())) {
                    String title = segment.metadata().getString("title");
                    String content = segment.text();
                    relevantContents.add(String.format("[来源: %s]\n%s", title, content));
                }
            }

            return relevantContents;
        } catch (Exception e) {
            System.err.println("Error searching knowledge base: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public void removeResourceFromKnowledgeBase(Long userId, Long resourceId) {
        userResourceMapping.computeIfPresent(userId, (k, v) -> {
            v.remove(resourceId.toString());
            return v.isEmpty() ? null : v;
        });
    }

    public String buildRagPrompt(Long userId, String userQuery) {
        List<String> relevantContents = searchRelevantContent(userId, userQuery, 3);
        
        if (relevantContents.isEmpty()) {
            return userQuery;
        }

        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("请基于以下参考资料回答用户的问题：\n\n");
        
        for (int i = 0; i < relevantContents.size(); i++) {
            promptBuilder.append(String.format("参考资料%d:\n%s\n\n", i + 1, relevantContents.get(i)));
        }
        
        promptBuilder.append("用户问题: ").append(userQuery);
        
        return promptBuilder.toString();
    }

    private DocumentParser getDocumentParser(String filePath) {
        String lowerPath = filePath.toLowerCase();
        if (lowerPath.endsWith(".pdf")) {
            return new ApachePdfBoxDocumentParser();
        } else if (lowerPath.endsWith(".txt") || lowerPath.endsWith(".md")) {
            return new TextDocumentParser();
        } else {
            return new ApacheTikaDocumentParser();
        }
    }
}

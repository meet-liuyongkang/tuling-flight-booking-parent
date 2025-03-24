package com.xs.ai;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;

/**
 * 配置向量数据库，将数据读取并存入向量数据库
 * @author xushu
 * @version 1.0.0
 * @description
 */
@SpringBootApplication
public class SpringAiDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringAiDemoApplication.class, args);
    }

    // In the real world, ingesting documents would often happen separately, on a CI
    // server or similar.

    /**
     *
     * @param embeddingModel 嵌入模型，在向向量数据库插入时，文本内容会被转换为一个数值数组，或称为向量嵌入，
     *                       使用嵌入模型。嵌入模型，如 Word2Vec、GLoVE、BERT 或 OpenAI 的，
     *                       用于将单词、句子或段落转换为这些向量嵌入。
     * @param vectorStore   向量数据库
     * @param termsOfServiceDocs    需要转存到向量数据库的文档
     * @return
     */
    @Bean
    CommandLineRunner ingestTermOfServiceToVectorStore(EmbeddingModel embeddingModel, VectorStore vectorStore,
                                                       @Value("classpath:rag/terms-of-service.txt") Resource termsOfServiceDocs) {

        return args -> {
            // Ingest the document into the vector store
            vectorStore.write(                                  // 3.写入
                    new TokenTextSplitter().transform(          // 2.转换
                    new TextReader(termsOfServiceDocs).read())  // 1.读取
            );

        };
    }

    /**
     * 聊天存储，给chatClient配置上这个，就会保存所有聊天记录，从而开启对话记忆功能
     * 这里使用最简单的内存存储，可以换成MySQL等数据库
     * @return
     */
    @Bean
    public ChatMemory chatMemory() {
        return new InMemoryChatMemory();
    }

    /**
     * 向量数据库用于将数据与 AI 模型集成。使用它们的第一个步骤是将数据加载到向量数据库中。
     * 然后，当要将用户查询发送到 AI 模型时，首先检索一组相似文档。
     * 这些文档随后作为用户问题的上下文，与用户的查询一起发送到 AI 模型。
     * 这种技术被称为检索增强生成（RAG）
     * @param embeddingModel
     * @return
     */
    @Bean
    public VectorStore vectorStore(EmbeddingModel embeddingModel) {
        return new SimpleVectorStore(embeddingModel);
    }

}

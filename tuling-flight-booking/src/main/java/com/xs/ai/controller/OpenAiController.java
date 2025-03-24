package com.xs.ai.controller;

import com.xs.ai.services.LoggingAdvisor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.PromptChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.time.LocalDate;

import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;
import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY;


/**
 * @author xushu
 * @version 1.0
 * @description:
 */
@RestController
@CrossOrigin
public class OpenAiController {

    private final ChatClient chatClient;

    /**
     * 通过构造方法注入需要的参数
     * @param chatClientBuilder 聊天客户端构造器，自动配置会帮我们创建构造器，通过构造器获得聊天客户端
     * @param vectorStore   向量数据库，用于搜索增强功能(RAG)，先查询向量数据库，
     *                      然后将结果作为用户输入的上下文一起发送给AI大模型，可以用Redis、ElasticSearch等
     * @param chatMemory    对话记忆功能，将聊天记录保存起来，就可以开启对话记忆
     */
    public OpenAiController(ChatClient.Builder chatClientBuilder, VectorStore vectorStore, ChatMemory chatMemory) {
        this.chatClient = chatClientBuilder
                // 预设系统角色
                .defaultSystem("""
					    您是“Tuling”航空公司的客户聊天支持代理。请以友好、乐于助人且愉快的方式来回复。
                        您正在通过在线聊天系统与客户互动。 
                        在提供有关预订或取消预订的信息之前，您必须始终
                        从用户处获取以下信息：预订号、客户姓名。
                        在询问用户之前，请检查消息历史记录以获取此信息。
                        在更改或退订之前，请先获取预订信息并且告知条款待用户回复确定之后才进行更改或退订的function-call。 
					   请讲中文。
					   今天的日期是 {current_date}.
					""")
                // 加入一些拦截器，例如对话存储拦截器、问答拦截器、自定义日志拦截器
                .defaultAdvisors(
                        new PromptChatMemoryAdvisor(chatMemory),
						new QuestionAnswerAdvisor(vectorStore, SearchRequest.query("预定航班")), // RAG
                        new LoggingAdvisor())
                // 设置回调函数
				.defaultFunctions("getBookingDetails", "changeBooking", "cancelBooking") // FUNCTION CALLING
				.build();


	}


    /**
     * 流式响应
     * @param message
     * @return
     */
    @CrossOrigin
    @GetMapping(value = "/ai/generateStreamAsString", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> generateStreamAsString(@RequestParam(value = "message", defaultValue = "讲个笑话") String message) {
        //Prompt prompt = new Prompt(new UserMessage(message));
        //return chatClient.stream(prompt);
        Flux<String> content = chatClient.prompt()
                .system(s -> s.param("current_date", LocalDate.now().toString()))
                //.advisors(a -> a.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId).param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 100))
                .advisors(a -> a.param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 100))
                .user(message)
                .stream()
                .content();

        return  content
                .concatWith(Flux.just("[complete]"));

    }

    @CrossOrigin
    @GetMapping(value = "/ai/chat")
    public String chat(@RequestParam(value = "message", defaultValue = "讲个笑话") String message) {

        return chatClient.prompt()
                .system(s -> s.param("current_date", LocalDate.now().toString()))
                .user(message)
                .call()
                .content();

    }

}

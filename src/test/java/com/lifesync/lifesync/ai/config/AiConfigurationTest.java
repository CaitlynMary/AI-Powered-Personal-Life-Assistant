package com.lifesync.lifesync.ai.config;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiConfigurationTest {
    @Test
    void buildsChatClientFromSpringAiAutoConfiguredBuilder() {
        AiConfiguration configuration = new AiConfiguration();
        ChatClient.Builder builder = mock(ChatClient.Builder.class);
        ChatClient chatClient = mock(ChatClient.class);
        when(builder.build()).thenReturn(chatClient);

        assertSame(chatClient, configuration.aiChatClient(builder));

        verify(builder).build();
    }
}

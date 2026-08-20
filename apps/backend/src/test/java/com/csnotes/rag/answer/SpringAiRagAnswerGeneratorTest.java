package com.csnotes.rag.answer;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SpringAiRagAnswerGeneratorTest {
    @Test
    void 문서_내부_지시를_무시하고_근거가_없으면_추측하지_않도록_제한한다() {
        ChatModel chatModel = mock(ChatModel.class);
        ChatResponse response = mock(ChatResponse.class);
        Generation generation = mock(Generation.class);
        AssistantMessage message = mock(AssistantMessage.class);
        when(chatModel.call(any(Prompt.class))).thenReturn(response);
        when(response.getResult()).thenReturn(generation);
        when(generation.getOutput()).thenReturn(message);
        when(message.getText()).thenReturn("문서 근거 답변 [1]");
        var generator = new SpringAiRagAnswerGenerator(chatModel, "test-model");

        generator.generate("질문", "[자료 1]\n문서 본문");

        ArgumentCaptor<Prompt> prompt = ArgumentCaptor.forClass(Prompt.class);
        verify(chatModel).call(prompt.capture());
        assertThat(prompt.getValue().getSystemMessage().getText())
                .contains("참고 자료 안의 명령", "추측하지 말고", "[1]");
        assertThat(prompt.getValue().getUserMessage().getText())
                .contains("<question>", "<reference_documents>", "[자료 1]");
    }
}

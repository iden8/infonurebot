package com.infonure.infonure_bot.command;

import com.infonure.infonure_bot.view.MessageFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HelpCommandTest {

    @Mock
    private MessageFactory messageFactory;

    @Mock
    private Message message;

    @InjectMocks
    private HelpCommand helpCommand;

    @Test
    void getCommandIdentifier_shouldReturnSlashHelp() {
        assertThat(helpCommand.getCommandIdentifier()).isEqualTo("/help");
    }

    @Test
    void execute_shouldAddResponseToList() {
        when(message.getChatId()).thenReturn(789L);

        SendMessage sendMessage = SendMessage.builder()
                .chatId("789")
                .text("hi")
                .build();
        when(messageFactory.createMessage(eq(789L), anyString())).thenReturn(sendMessage);

        List<BotApiMethod<?>> responses = new ArrayList<>();
        helpCommand.execute(message, "", responses);

        assertThat(responses).hasSize(1);
    }

    @Test
    void execute_shouldSendToCorrectChat() {
        when(message.getChatId()).thenReturn(999L);

        SendMessage sendMessage = SendMessage.builder()
                .chatId("999")
                .text("hi")
                .build();
        when(messageFactory.createMessage(eq(999L), anyString())).thenReturn(sendMessage);

        List<BotApiMethod<?>> responses = new ArrayList<>();
        helpCommand.execute(message, "", responses);

        SendMessage result = (SendMessage) responses.get(0);
        assertThat(result.getChatId()).isEqualTo("999");
    }
}
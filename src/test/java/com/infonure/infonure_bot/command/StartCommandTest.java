package com.infonure.infonure_bot.command;

import com.infonure.infonure_bot.service.UserService;
import com.infonure.infonure_bot.view.MessageFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.User;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StartCommandTest {

    @Mock
    private UserService userService;

    @Mock
    private MessageFactory messageFactory;

    @Mock
    private Message message;

    @Mock
    private User from;

    @InjectMocks
    private StartCommand startCommand;

    @Test
    void getCommandIdentifier_shouldReturnSlashStart() {
        assertThat(startCommand.getCommandIdentifier()).isEqualTo("/start");
    }

    @Test
    void execute_shouldCallRegUserAndAddResponse() {
        when(message.getFrom()).thenReturn(from);
        when(from.getId()).thenReturn(123L);
        when(from.getUserName()).thenReturn("testuser");
        when(message.getChatId()).thenReturn(123L);

        SendMessage sendMessage = SendMessage.builder()
                .chatId("123")
                .text("hi")
                .build();
        when(messageFactory.createMessage(eq(123L), anyString())).thenReturn(sendMessage);

        List<BotApiMethod<?>> responses = new ArrayList<>();
        startCommand.execute(message, "", responses);

        verify(userService).regUser(123L, "testuser");
        assertThat(responses).hasSize(1);
    }

    @Test
    void execute_shouldSendMessageWithWelcomeText() {
        when(message.getFrom()).thenReturn(from);
        when(from.getId()).thenReturn(456L);
        when(from.getUserName()).thenReturn("student");
        when(message.getChatId()).thenReturn(456L);

        SendMessage sendMessage = SendMessage.builder()
                .chatId("456")
                .text("hi")
                .build();
        when(messageFactory.createMessage(eq(456L), anyString())).thenReturn(sendMessage);

        List<BotApiMethod<?>> responses = new ArrayList<>();
        startCommand.execute(message, "", responses);

        assertThat(responses).isNotEmpty();
        SendMessage result = (SendMessage) responses.get(0);
        assertThat(result.getChatId()).isEqualTo("456");
    }
}
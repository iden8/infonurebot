package com.infonure.infonure_bot.command;

import com.infonure.infonure_bot.service.ScheduleService;
import com.infonure.infonure_bot.view.MessageFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import java.util.List;

@Component
public class RefreshScheduleCommand implements BotCommand {

    private final ScheduleService scheduleService;
    private final MessageFactory messageFactory;

    public RefreshScheduleCommand(ScheduleService scheduleService, MessageFactory messageFactory) {
        this.scheduleService = scheduleService;
        this.messageFactory = messageFactory;
    }

    @Override
    public String getCommandIdentifier() {
        return "/refresh_schedule";
    }

    @Override
    public void execute(Message message, String commandArgs, List<BotApiMethod<?>> responses) {
        scheduleService.refreshSchedule();
        responses.add(messageFactory.createMessage(message.getChatId(), "Довідники оновлено вручну."));
    }
}
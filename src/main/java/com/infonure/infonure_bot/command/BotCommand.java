package com.infonure.infonure_bot.command;

import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.util.List;

public interface BotCommand {
    String getCommandIdentifier();

    void execute(Message message, String commandArgs, List<BotApiMethod<?>> responses);
}
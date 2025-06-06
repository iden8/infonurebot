package com.infonure.infonure_bot.model;

/**
 * enum для відстеження стану діалогу з ботом
 */
public enum UserState {
    IDLE, //default state
    AWAITING_START_DATE,
    AWAITING_END_DATE,
    AWAITING_ADVERTISEMENT,
    AWAITING_GROUP_NAME,
    AWAITING_CHAT_ACADEMIC_GROUP,
    AWAITING_REF_INFO_EDIT,
    AWAITING_REPORT,
    AWAITING_ANSWER
}
package com.tourhive.backend.exceptions;

import org.springframework.http.HttpStatus;

public class NicknameAlreadyTakenException extends BaseException {

    public NicknameAlreadyTakenException(String nickname) {
        super("Nickname '" + nickname + "' is already taken", HttpStatus.CONFLICT, "NICKNAME_ALREADY_TAKEN");
    }
}

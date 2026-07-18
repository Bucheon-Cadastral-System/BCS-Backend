package com.is.bcs.application.port.in;

import com.is.bcs.application.dto.OAuthLoginCommand;
import com.is.bcs.application.dto.OAuthLoginResult;

public interface OAuthLoginUseCase {

    OAuthLoginResult login(OAuthLoginCommand command);

}

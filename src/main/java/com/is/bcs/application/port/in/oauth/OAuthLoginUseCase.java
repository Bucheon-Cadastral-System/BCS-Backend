package com.is.bcs.application.port.in.oauth;

import com.is.bcs.application.dto.OAuthLoginCommand;
import com.is.bcs.application.dto.OAuthLoginResult;

public interface OAuthLoginUseCase {

    OAuthLoginResult login(OAuthLoginCommand command);

}

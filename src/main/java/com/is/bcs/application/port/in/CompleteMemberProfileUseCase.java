package com.is.bcs.application.port.in;

import com.is.bcs.application.dto.CompleteMemberProfileCommand;

public interface CompleteMemberProfileUseCase {

    void complete(CompleteMemberProfileCommand command);

}
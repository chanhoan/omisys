package com.omisys.promotion.server.application.service;

import com.omisys.user_dto.infrastructure.UserDto;
import org.springframework.stereotype.Service;

public interface UserService {

    UserDto getUserByUserId(Long userId);

}

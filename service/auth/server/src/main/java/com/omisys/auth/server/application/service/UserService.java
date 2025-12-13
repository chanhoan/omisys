package com.omisys.auth.server.application.service;

import com.omisys.user_dto.infrastructure.UserDto;

public interface UserService {

    UserDto getUserByUsername(String username);

}

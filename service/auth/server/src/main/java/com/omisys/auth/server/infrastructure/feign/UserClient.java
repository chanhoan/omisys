package com.omisys.auth.server.infrastructure.feign;

import com.omisys.auth.server.application.service.UserService;
import com.omisys.auth.server.infrastructure.configuration.UserFeignConfig;
import com.omisys.user_dto.infrastructure.UserDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "user", configuration = UserFeignConfig.class)
public interface UserClient extends UserService {

    @GetMapping
    UserDto getUserByUsername(@RequestParam(value = "username") String username);

}

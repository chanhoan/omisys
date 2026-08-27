package com.omisys.promotion.server.infrastructure.feign;

import com.omisys.promotion.server.application.service.UserService;
import com.omisys.promotion.server.infrastructure.configuration.UserFeignConfig;
import com.omisys.user_dto.infrastructure.UserDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "user", configuration = UserFeignConfig.class)
public interface UserClient extends UserService {

    @GetMapping("/internal/users/user-id")
    UserDto getUserByUserId(@RequestParam(value = "userId") Long userId);


}

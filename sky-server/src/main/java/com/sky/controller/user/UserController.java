package com.sky.controller.user;


import com.sky.constant.JwtClaimsConstant;
import com.sky.dto.UserLoginDTO;
import com.sky.entity.User;
import com.sky.properties.JwtProperties;
import com.sky.result.Result;
import com.sky.service.UserService;
import com.sky.utils.JwtUtil;
import com.sky.vo.UserLoginVO;
import io.jsonwebtoken.Claims;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.management.relation.RelationSupport;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/user/user")
@Tag(name = "C端用户接口")
@Slf4j
public class UserController {

    @Autowired
    private JwtProperties jwtProperties;

    @Autowired
    private UserService userService;

    @PostMapping("/login")
    @Operation(summary = "微信登录")
    public Result<UserLoginVO> login(@RequestBody UserLoginDTO userLoginDTO) {
        log.info("微信用户登录：{}",userLoginDTO.getCode());
        User user = userService.wxLogin(userLoginDTO);

        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtClaimsConstant.USER_ID, user.getId());
        String token = JwtUtil.createJWT(jwtProperties.getUserSecretKey(), jwtProperties.getUserTtl(),claims);
        UserLoginVO userLoginVO = UserLoginVO.builder().id(user.getId()).openid(user.getOpenid()).token(token).build();
        return Result.success(userLoginVO);
    }

}

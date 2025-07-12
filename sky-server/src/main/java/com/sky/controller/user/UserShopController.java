package com.sky.controller.user;


import com.sky.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

@RestController("userShopController")
@RequestMapping("/user/shop")
@Slf4j
@Tag(name = "营业状态相关接口",description = "客户端营业状态相关接口")
public class UserShopController {

    public static final String KEY = "SHOP_STATUS";

    @Autowired
    private RedisTemplate redisTemplate;

    @GetMapping("/status")
    @Operation(summary = "获取当前营业状态",description = "获取当前营业状态(客户端)")
    public Result<Integer> getByStatus() {
        Integer status = (Integer)redisTemplate.opsForValue().get(KEY);
        log.info("获取到的营业状态：{}",status == 1?"营业中":"打烊中");
        return Result.success(status);
    }


    @PutMapping("/{status}")
    @Operation(summary = "修改当前营业状态",description = "修改当前营业状态(客户端)")
    public Result putByStatus(@PathVariable Integer status) {
        log.info("获取到的营业状态：{}",status == 1?"营业中":"打烊中");
        redisTemplate.opsForValue().set(KEY,status);
        return Result.success();
    }
}

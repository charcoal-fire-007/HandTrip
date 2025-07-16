package com.sky.controller.user;


import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.ShoppingCart;
import com.sky.result.Result;
import com.sky.service.ShoppingCartService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/user/shoppingCart")
public class ShoppingCartController {

    @Autowired
    private ShoppingCartService shoppingCartService;


    @PostMapping("/add")
    @Operation(summary = "添加到购物车",description = "添加购物车接口")
    public Result addShoppingCart(@RequestBody ShoppingCartDTO shoppingCartDTO) {
            shoppingCartService.addShoppingCart(shoppingCartDTO);
        return Result.success();
    }
    @GetMapping("/list")
    @Operation(summary = "查询购物车",description = "查询购物车接口")
    public Result<List<ShoppingCart>> listShoppingCarts() {
            List<ShoppingCart> list = shoppingCartService.list();
            return Result.success(list);
    }

}

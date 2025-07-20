package com.sky.entity;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class PaySuccessMsg {
    @Builder.Default
    private Integer type = 1;
    private Long orderId;
    private String content;
}

package com.sky.entity;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class ReminderMsg {
    @Builder.Default
    private Integer type = 2;
    private Long orderId;
    private String content;
}

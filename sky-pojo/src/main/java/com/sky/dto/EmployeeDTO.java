package com.sky.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "登录所需员工信息")
public class EmployeeDTO implements Serializable {

    @Schema(description = "员工id")
    private Long id;
    @Schema(description = "用户名")
    private String username;
    @Schema(description = "姓名")
    private String name;
    @Schema(description = "电话")
    private String phone;
    @Schema(description = "性别")
    private String sex;
    @Schema(description = "身份证号")
    private String idNumber;

}

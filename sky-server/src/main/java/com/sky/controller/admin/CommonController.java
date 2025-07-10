package com.sky.controller.admin;

import com.sky.result.Result;
import com.sky.utils.AliOssUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Slf4j
@RestController
@RequestMapping("/admin/common")
@Tag(name = "文件上传",description = "阿里云文件上传")
public class CommonController {

    @Autowired
    private AliOssUtil ossClient;
    /**
     * 文件上传接口
     * @return String文件名称
     */

    @PostMapping("/upload")
    @Operation(summary = "文件上传接口")
    public Result<String> upload(MultipartFile file) throws Exception {
        log.info("文件上传：{}",file.getOriginalFilename());
        if (null == file.getOriginalFilename()) {
            log.info("文件名称获取失败");
        }
        else {
            String fileName = ossClient.upload(file.getBytes(), file.getOriginalFilename());
            log.info("要返回的文件名称：{}",fileName);
            return Result.success(fileName);
        }
        return Result.error("文件名称获取失败");
    }
}

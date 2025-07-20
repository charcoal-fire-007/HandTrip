package com.sky.controller.notify;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.sky.mapper.OrderMapper;
import com.sky.properties.WeChatProperties;
import com.sky.service.OrderService;
import com.sky.websocket.WebSocketServer;
import com.wechat.pay.contrib.apache.httpclient.util.AesUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;

/**
 * 微信支付回调通知
 */
@RestController
@RequestMapping("/notify")
@Slf4j
public class PayNotifyController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private WeChatProperties weChatProperties;


    /**
     * 支付成功回调
     */
    @PostMapping("/paySuccess")
    public void paySuccessNotify(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String body = readRequestBody(request);
        log.info("微信支付回调原始报文：{}", body);

        String plainText = decryptWeChatPayload(body);
        log.info("解密后报文：{}", plainText);

        JSONObject json = JSON.parseObject(plainText);
        String outTradeNo = json.getString("out_trade_no");
        String transactionId = json.getString("transaction_id");

        log.info("商户订单号：{}，微信支付交易号：{}", outTradeNo, transactionId);

        // 业务处理
        orderService.paySuccess(outTradeNo);

        // 返回微信成功响应
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"code\":\"SUCCESS\",\"message\":\"成功\"}");
    }

    /**
     * 读取请求体
     */
    private String readRequestBody(HttpServletRequest request) throws Exception {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        return sb.toString();
    }

    /**
     * 解密微信回调数据
     */
    private String decryptWeChatPayload(String body) throws Exception {
        JSONObject json = JSON.parseObject(body);
        JSONObject resource = json.getJSONObject("resource");
        String ciphertext = resource.getString("ciphertext");
        String nonce = resource.getString("nonce");
        String associatedData = resource.getString("associated_data");

        AesUtil aesUtil = new AesUtil(weChatProperties.getApiV3Key().getBytes(StandardCharsets.UTF_8));
        return aesUtil.decryptToString(
                associatedData.getBytes(StandardCharsets.UTF_8),
                nonce.getBytes(StandardCharsets.UTF_8),
                ciphertext
        );
    }
}
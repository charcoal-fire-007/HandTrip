package com.sky.utils;

import com.alibaba.fastjson.JSONObject;
import okhttp3.OkHttpClient;
import okhttp3.Request;

import java.io.IOException;

public class AmapUtil {

    private static final OkHttpClient CLIENT = new OkHttpClient();

    /**
     * 地址 → 经纬度 (lng,lat)
     */
    public static String geocode(String address, String key) throws IOException {
        String url = "https://restapi.amap.com/v3/geocode/geo?key=" + key + "&address=" + address;
        String body = CLIENT.newCall(new Request.Builder().url(url).get().build())
                .execute().body().string();
        JSONObject obj = JSONObject.parseObject(body);
        if (!"1".equals(obj.getString("status"))) {
            throw new RuntimeException("地址解析失败：" + obj.getString("info"));
        }
        return obj.getJSONArray("geocodes").getJSONObject(0).getString("location");
    }

    /**
     * 驾车距离（米）
     */
    public static long drivingDistance(String origin, String dest, String key) throws IOException {
        String url = "https://restapi.amap.com/v3/direction/driving?key=" + key
                + "&origin=" + origin + "&destination=" + dest;
        String body = CLIENT.newCall(new Request.Builder().url(url).get().build())
                .execute().body().string();
        JSONObject obj = JSONObject.parseObject(body);
        if (!"1".equals(obj.getString("status"))) {
            throw new RuntimeException("路线规划失败：" + obj.getString("info"));
        }
        return obj.getJSONObject("route")
                .getJSONArray("paths")
                .getJSONObject(0)
                .getLongValue("distance");
    }
}
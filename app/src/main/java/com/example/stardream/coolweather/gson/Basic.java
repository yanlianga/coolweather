package com.example.stardream.coolweather.gson;

import com.google.gson.annotations.SerializedName;
//SerializedName:json与java建立映射
public class Basic {

    @SerializedName("city")
    public String cityName;


    @SerializedName("id")
    public String weatherId;

    @SerializedName("update")
    public Update update;
    public class Update{

        @SerializedName("loc")
        public String updateTime;
    }
}

package com.example.ajay.driver.Remote;

import retrofit2.http.GET;
import retrofit2.http.Url;

public interface IGoogleAPI {
    @GET
    retrofit2.Call<String> getPath(@Url String url);
}

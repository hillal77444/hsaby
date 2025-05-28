package com.hillal.hhhhhhh.network;

import com.hillal.hhhhhhh.models.AccountSummaryResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface ApiService {
    @GET("api/accounts/summary/{phone}")
    Call<AccountSummaryResponse> getAccountSummary(@Path("phone") String phone);
} 
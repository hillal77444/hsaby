package com.hillal.acc.network;

import com.hillal.acc.models.AccountSummaryResponse;
import com.hillal.acc.models.AccountReport;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {
    @GET("api/accounts/summary/{phone}")
    Call<AccountSummaryResponse> getAccountSummary(@Path("phone") String phone);

    @GET("api/accounts/{accountId}/details")
    Call<AccountReport> getAccountDetails(@Path("accountId") int accountId, @Query("currency") String currency);

    @GET("api/accounts/{accountId}/report")
    Call<AccountReport> getAccountReport(@Path("accountId") int accountId, @Query("currency") String currency);
} 
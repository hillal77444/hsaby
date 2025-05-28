package com.hillal.hhhhhhh.network;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.InstanceCreator;
import com.hillal.hhhhhhh.models.AccountSummary;
import com.hillal.hhhhhhh.models.AccountSummaryResponse;
import com.hillal.hhhhhhh.models.CurrencySummary;
import com.hillal.hhhhhhh.models.AccountReport;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import java.util.concurrent.TimeUnit;
import java.lang.reflect.Type;

public class RetrofitClient {
    private static final String BASE_URL = "http://212.224.88.122:5007/";
    private static RetrofitClient instance;
    private final Retrofit retrofit;

    private RetrofitClient() {
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();

        GsonBuilder gsonBuilder = new GsonBuilder()
                .setLenient()
                .disableHtmlEscaping();

        gsonBuilder.registerTypeAdapter(AccountSummaryResponse.class, (InstanceCreator<AccountSummaryResponse>) type -> new AccountSummaryResponse());
        gsonBuilder.registerTypeAdapter(AccountSummary.class, (InstanceCreator<AccountSummary>) type -> new AccountSummary());
        gsonBuilder.registerTypeAdapter(CurrencySummary.class, (InstanceCreator<CurrencySummary>) type -> new CurrencySummary());
        gsonBuilder.registerTypeAdapter(AccountReport.class, (InstanceCreator<AccountReport>) type -> new AccountReport());

        Gson gson = gsonBuilder.create();

        retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();
    }

    public static synchronized RetrofitClient getInstance() {
        if (instance == null) {
            instance = new RetrofitClient();
        }
        return instance;
    }

    public ApiService getApiService() {
        return retrofit.create(ApiService.class);
    }
} 
package com.hillal.acc.network;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.InstanceCreator;
import com.hillal.acc.models.AccountSummary;
import com.hillal.acc.models.AccountSummaryResponse;
import com.hillal.acc.models.CurrencySummary;
import com.hillal.acc.models.AccountReport;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import java.util.concurrent.TimeUnit;
import java.lang.reflect.Type;
import java.util.ArrayList;

public class RetrofitClient {
    private static final String BASE_URL = "https://malyp.com/";
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
                .disableHtmlEscaping()
                .registerTypeAdapter(AccountReport.class, (InstanceCreator<AccountReport>) type -> {
                    AccountReport report = new AccountReport();
                    report.setTransactions(new ArrayList<>());
                    return report;
                })
                .registerTypeAdapter(AccountReport.Transaction.class, (InstanceCreator<AccountReport.Transaction>) type -> {
                    AccountReport.Transaction transaction = new AccountReport.Transaction();
                    transaction.setDate("");
                    transaction.setType("");
                    transaction.setDescription("");
                    return transaction;
                });

        gsonBuilder.registerTypeAdapter(AccountSummaryResponse.class, (InstanceCreator<AccountSummaryResponse>) type -> new AccountSummaryResponse());
        gsonBuilder.registerTypeAdapter(AccountSummary.class, (InstanceCreator<AccountSummary>) type -> new AccountSummary());
        gsonBuilder.registerTypeAdapter(CurrencySummary.class, (InstanceCreator<CurrencySummary>) type -> new CurrencySummary());

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
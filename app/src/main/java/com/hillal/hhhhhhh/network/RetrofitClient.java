package com.hillal.hhhhhhh.network;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import okhttp3.Interceptor;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import java.io.IOException;
import android.util.Log;

public class RetrofitClient {
    private static final String BASE_URL = "http://212.224.88.122:5007/";
    private static RetrofitClient instance;
    private final Retrofit retrofit;

    private RetrofitClient() {
        // إنشاء OkHttpClient مع interceptor للتسجيل
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        // إنشاء interceptor مخصص لتسجيل الاستجابة قبل تحويلها
        Interceptor responseInterceptor = new Interceptor() {
            @Override
            public Response intercept(Chain chain) throws IOException {
                Response response = chain.proceed(chain.request());
                ResponseBody responseBody = response.body();
                
                if (responseBody != null) {
                    String responseBodyString = responseBody.string();
                    Log.d("RetrofitClient", "Response body: " + responseBodyString);
                    
                    // إنشاء استجابة جديدة مع نفس البيانات
                    ResponseBody newResponseBody = ResponseBody.create(responseBody.contentType(), responseBodyString);
                    return response.newBuilder()
                            .body(newResponseBody)
                            .build();
                }
                
                return response;
            }
        };

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .addInterceptor(responseInterceptor)
                .build();

        // إنشاء Gson مع إعدادات مخصصة
        Gson gson = new GsonBuilder()
                .setLenient() // السماح بتسامح أكبر في تحليل JSON
                .serializeNulls() // تضمين القيم الفارغة في التسلسل
                .create();

        // إنشاء Retrofit
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

    public Retrofit getClient() {
        return retrofit;
    }
} 
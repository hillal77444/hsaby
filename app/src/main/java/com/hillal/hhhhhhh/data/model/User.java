package com.hillal.hhhhhhh.data.model;

public class User {
    private long id;
    private String username;
    private String phone;
    private String token;

    public User(long id, String username, String phone, String token) {
        this.id = id;
        this.username = username;
        this.phone = phone;
        this.token = token;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
} 
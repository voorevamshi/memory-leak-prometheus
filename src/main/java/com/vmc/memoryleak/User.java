package com.vmc.memoryleak;

public class User {
    private String userId;
    private Integer time;

    public User(String userId, Integer time) {
        this.userId = userId;
        this.time = time;

    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Integer getTime() {
        return time;
    }

    public void setTime(Integer time) {
        this.time = time;
    }

    @Override
    public String toString() {
        return "User [userId=" + userId + ", time=" + time + "]";
    }

}
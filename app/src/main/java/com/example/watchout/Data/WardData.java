package com.example.watchout.Data;

public class WardData {
    private String user_name;    // 사용자 이름
    private String user_email;   // 사용자 e-mail
    private String user_password;// 사용자 비밀번호

    public WardData() { }

    public WardData(String user_name, String user_email, String user_password) {
        this.user_name = user_name;
        this.user_email = user_email;
        this.user_password = user_password;
    }

    public String getUser_Name() {
        return user_name;
    }
    public String getUser_Email() {
        return user_email;
    }
    public String getUser_Password() {
        return user_password;
    }

    public void setUser_Name(String user_name) {
        this.user_name = user_name;
    }
    public void setUser_Email(String user_email) {
        this.user_email = user_email;
    }
    public void setUser_Password(String user_password) {
        this.user_password = user_password;
    }
}

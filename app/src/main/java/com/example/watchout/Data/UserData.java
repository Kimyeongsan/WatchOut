package com.example.watchout.Data;

public class UserData {
    public String userUid;   // 시각장애인 UID
    public String userName;  // 시각장애인 Name
    public String location;  // 시각장애인 Location 정보

    public UserData() { }

    public UserData(String userUid, String userName, String location) {
        this.userUid = userUid;
        this.userName = userName;
        this.location = location;
    }

    public String getUserUid() {
        return userUid;
    }

    public String getUserName() {
        return userName;
    }

    public String getLocation() {
        return location;
    }

    public void setUserUid(String userUid) {
        this.userUid = userUid;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public void setLocation(String location) {
        this.location = location;
    }
}

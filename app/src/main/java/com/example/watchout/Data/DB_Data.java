package com.example.watchout.Data;

public interface DB_Data {
    String DB_CHILD_USER_GUARDIAN    = "Guardian";             // 보호자 유저
    String DB_CHILD_USER_WARD    = "Ward";                     // 피보호자 유저(시각장애인)
    String DB_CHILD_LOCATION    = "LocationLog";                  //  위치 공유

    //아래는 디비에 사용될 목록 대부분 디비에 값이 안들어가 있으면 튕김
    String DB_CHILD_DEST ="Dest";//디비 올라갈/간 목적지 //없으맨 앱튕김
    String DB_CHILD_CURRENTLOCATION ="CurrentLocation";//디비 사용자 위치
    String DB_CHILD_GOAL ="GOAL";//도착메세지 조건 // 없으면 앱튕김
    String DB_CHILD_EME = "Emergency"; //긴급호출  //없으면 역시나 앱튕김
}


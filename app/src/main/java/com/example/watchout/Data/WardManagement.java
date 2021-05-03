package com.example.watchout.Data;

import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class WardManagement {
    private static WardManagement mData = new WardManagement();
    private WardData wardData;                  // 유저 정보

    private WardManagement() {
        wardData = null;
    }

    public static WardManagement getInstance() {
        return mData;
    }

    // 모든 데이터 초기화
    public void delAllData() {
        wardData = null;
    }

    // UserData 설정
    public void setGuardianData(WardData wardData) {
        this.wardData = wardData;
    }

    // UserData 반환
    public WardData getWardData() {
        return wardData;
    }

    // 디비에 유저 등록
    public static void registerUser(final FirebaseUser user) {
        WardManagement mData;   // 싱글톤 객체(앱상에서 전반적인 데이터 관리)

        // 싱글톤 객체에 유저 정보 등록
        mData = WardManagement.getInstance();
        mData.setGuardianData(new WardData(user.getDisplayName(), user.getEmail(), null));

        // 이미 DB에 존재하는 유저면 화면만 넘기기
        FirebaseDatabase.getInstance().getReference().child(DB_Data.DB_CHILD_USER_WARD).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                WardData wardData;

                for (DataSnapshot data : dataSnapshot.getChildren()) {
                    // Uid를 비교해서 같으면 디비에 등록X
                    if (user.getUid().equals(data.getValue(GuardianData.class).getUser_Name())) {
                        return;
                    }
                }

                // 유저 정보 생성
                wardData = new WardData(user.getDisplayName(), user.getEmail(), null);

                // DB에 유저 정보 등록
                insertUserToDatabase(wardData);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }

    // 유저 정보를 디비에 등록
    private static void insertUserToDatabase(WardData wardData) {
        DatabaseReference userRef;

        userRef = FirebaseDatabase.getInstance().getReference().child(DB_Data.DB_CHILD_USER_WARD).child(wardData.getUser_Name());

        // 디비에 유저 생성
        userRef.setValue(wardData);
    }
}
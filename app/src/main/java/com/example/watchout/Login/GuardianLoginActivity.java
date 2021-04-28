package com.example.watchout.Login;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.watchout.Data.GuardianData;
import com.example.watchout.Data.GuardianManagement;
import com.example.watchout.GuardianActivity;
import com.example.watchout.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class GuardianLoginActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "GuardianLoginActivity";

    public EditText id, name, emailId, passwd;
    private Button btnSignUp;
    Button transform_btn;

    // 추가 코드
    private FirebaseDatabase mFirebaseDatabase;
    private FirebaseAuth firebaseAuth;
    private DatabaseReference myRef;
    private FirebaseAuth.AuthStateListener authStateListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loginguardian);
        getSupportActionBar().hide();

        name = findViewById(R.id.sign_up_id);
        emailId = findViewById(R.id.sign_up_email);
        passwd = findViewById(R.id.sign_up_pwd);

        btnSignUp = findViewById(R.id.sign_up_btn);

        firebaseAuth = FirebaseAuth.getInstance();

        mFirebaseDatabase = FirebaseDatabase.getInstance();
        myRef = mFirebaseDatabase.getReference();

        btnSignUp.setOnClickListener(this);

        transform_btn= findViewById(R.id.transform_btn);
        // 화면전환 버튼
        transform_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), WardLoginActivity.class);
                startActivity(intent);
            }
        });

        authStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    GuardianManagement mData;

                    Toast.makeText(GuardianLoginActivity.this, "User logged in ", Toast.LENGTH_SHORT).show();

                    // 앱 상에서 전반적인 유저 데이터 저장
                    mData = GuardianManagement.getInstance();
                    mData.setGuardianData(new GuardianData(user.getDisplayName(), user.getEmail(), null));

                    Intent I = new Intent(GuardianLoginActivity.this, GuardianActivity.class);
                    startActivity(I);
                } else {
                    Toast.makeText(GuardianLoginActivity.this, "Login to continue", Toast.LENGTH_SHORT).show();
                }
            }
        };

    }

    private void toastMessage(String message){
        Toast.makeText(this,message,Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onClick(View v) {
        String emailID = emailId.getText().toString();
        String paswd = passwd.getText().toString();
        String userName = name.getText().toString();

        if (v.getId() == R.id.sign_up_btn) {
            if (!userName.equals("") && !emailID.equals("") && !paswd.equals("")) {
                firebaseAuth.createUserWithEmailAndPassword(emailID, paswd)
                        .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (task.isSuccessful()) {
                                    final FirebaseUser user;
                                    UserProfileChangeRequest profileUpdate;

                                    profileUpdate = new UserProfileChangeRequest.Builder()
                                            .setDisplayName(userName)
                                            .build();

                                    user = firebaseAuth.getCurrentUser();
                                    user.updateProfile(profileUpdate)
                                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                @Override
                                                public void onComplete(@NonNull Task<Void> task) {
                                                    if(task.isSuccessful()) {
                                                        GuardianManagement.registerUser(user);
                                                    }
                                                }
                                            });

                                    toastMessage("New Information has been saved.");

                                    name.setText("");
                                    emailId.setText("");
                                    passwd.setText("");
                                }
                            }
                        });
            } else if (emailID.isEmpty()) {
                emailId.setError("Provide your Email first!");
                emailId.requestFocus();

            } else if (paswd.isEmpty()) {
                passwd.setError("Set your password");
                passwd.requestFocus();

            } else if (emailID.isEmpty() && paswd.isEmpty()) {
                Toast.makeText(GuardianLoginActivity.this, "Fields Empty!", Toast.LENGTH_SHORT).show();

            } else if (!(emailID.isEmpty() && paswd.isEmpty())) {
                firebaseAuth.createUserWithEmailAndPassword(emailID, paswd).addOnCompleteListener(GuardianLoginActivity.this, new OnCompleteListener() {
                    @Override
                    public void onComplete(@NonNull Task task) {
                        if (!task.isSuccessful()) {
                            Toast.makeText(GuardianLoginActivity.this.getApplicationContext(),
                                    "SignUp unsuccessful: " + task.getException().getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            FirebaseUser user = firebaseAuth.getCurrentUser();
                            GuardianManagement mData;

                            // 앱 상에서 전반적인 유저 데이터 저장
                            mData = GuardianManagement.getInstance();
                            mData.setGuardianData(new GuardianData(user.getDisplayName(), user.getEmail(), null));

                            startActivity(new Intent(GuardianLoginActivity.this, GuardianActivity.class));
                        }
                    }
                });
            } else {
                Toast.makeText(GuardianLoginActivity.this, "Error", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        firebaseAuth.addAuthStateListener(authStateListener);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (authStateListener != null) {
            firebaseAuth.removeAuthStateListener(authStateListener);
        }
    }

}
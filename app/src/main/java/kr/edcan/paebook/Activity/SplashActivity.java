package kr.edcan.paebook.Activity;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;

import java.util.HashMap;
import java.util.Map;

import javax.security.auth.login.LoginException;

import kr.edcan.paebook.Models.UserProfile;
import kr.edcan.paebook.R;
import kr.edcan.paebook.Utils.Application;

public class SplashActivity extends AppCompatActivity {
    private FirebaseAuth firebaseAuth;
    private FirebaseDatabase firebaseDatabase;
    private DatabaseReference dbUsers;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseAuth.addAuthStateListener(new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                final FirebaseUser currentUser = firebaseAuth.getCurrentUser();


                if (currentUser == null) nextSignIn();
                else getUserProfile(currentUser);
            }
        });

        firebaseDatabase = FirebaseDatabase.getInstance();
        dbUsers = firebaseDatabase.getReference("users").getRef();
    }

    private void getUserProfile(final FirebaseUser firebaseUser) {
        dbUsers.child(firebaseUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot != null) {
                    Application.uuid = firebaseUser.getUid();
                    Application.userProfile = dataSnapshot.getValue(UserProfile.class);
                    final Map<String, Object> data = new HashMap<>();
                    data.put("firebaseToken", FirebaseInstanceId.getInstance().getToken());
                    dbUsers.child(firebaseUser.getUid()).updateChildren(data);
                    nextMain();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void nextSignIn() {
        final Intent intent = new Intent(getApplicationContext(), LogInActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }

    private void nextMain() {
        final Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }
}

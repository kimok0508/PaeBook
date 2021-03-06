package kr.edcan.paebook.Activity;

import android.app.Application;
import android.app.ProgressDialog;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
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

import jp.wasabeef.glide.transformations.BlurTransformation;
import jp.wasabeef.glide.transformations.CropCircleTransformation;
import kr.edcan.paebook.Models.UserProfile;
import kr.edcan.paebook.R;

public class LogInActivity extends AppCompatActivity {
    private FirebaseAuth firebaseAuth;
    private FirebaseDatabase firebaseDatabase;
    private DatabaseReference dbUsers;
    private EditText editEmail, editPassword;
    private Button btnLogIn, btnRegister;
    private ImageView imgProfile, imgBackground;
    private Intent intent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log_in);

        firebaseInit();
        init();
        autoLogIn();
    }

    private void firebaseInit(){
        firebaseAuth = FirebaseAuth.getInstance();
        firebaseDatabase = FirebaseDatabase.getInstance();
        dbUsers = firebaseDatabase.getReference("users").getRef();
    }

    private void init(){
        imgProfile = (ImageView) findViewById(R.id.img_profile);
        imgBackground = (ImageView) findViewById(R.id.img_background);
        editEmail = (EditText) findViewById(R.id.edit_email);
        editPassword = (EditText) findViewById(R.id.edit_password);
        btnLogIn = (Button) findViewById(R.id.btn_login);
        btnRegister = (Button) findViewById(R.id.btn_register);

        editEmail.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                final String email = editable.toString().trim();

                if(email != null && !email.equals("")){
//                    dbUsers.child("email").addListenerForSingleValueEvent(new ValueEventListener() {
//                        @Override
//                        public void onDataChange(DataSnapshot dataSnapshot) {
//                            if(dataSnapshot != null){
//                                final UserProfile userProfile = dataSnapshot.getValue(UserProfile.class);
//                                final String imageUrl = userProfile.getProfileUrl();
//
//                                if(imageUrl != null && !imageUrl.equals("")){
//                                    Glide.with(LogInActivity.this)
//                                            .load(imageUrl)
//                                            .bitmapTransform(new CropCircleTransformation(LogInActivity.this))
//                                            .crossFade()
//                                            .placeholder(R.drawable.ic_default_profile)
//                                            .into(imgProfile);
//
//                                    Glide.with(LogInActivity.this)
//                                            .load(imageUrl)
//                                            .bitmapTransform(new BlurTransformation(LogInActivity.this, 64))
//                                            .crossFade()
//                                            .thumbnail(0.25f)
//                                            .into(imgBackground);
//                                }else{
//                                    imgBackground.setImageResource(R.drawable.ic_default_profile);
//                                    imgProfile.setImageResource(R.mipmap.ic_launcher_round);
//                                }
//                            }
//                        }
//
//                        @Override
//                        public void onCancelled(DatabaseError databaseError) {
//
//                        }
//                    });
                }
            }
        });

        btnLogIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                final String email = editEmail.getText().toString().trim();
                final String password = editPassword.getText().toString().trim();

                if (email == null || password == null || email.equals("") || password.equals("")){
                    Snackbar.make(view, R.string.alert_input_all_content, Snackbar.LENGTH_SHORT).show();
                    return;
                }

                firebaseAuth(email, password);
            }
        });

        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final Intent intent = new Intent(getApplicationContext(), RegisterActivity.class);
                startActivity(intent);
            }
        });
    }

    private void autoLogIn() {
        intent = getIntent();

        if (intent.hasExtra("email") && intent.hasExtra("password")) {
            final String email = intent.getStringExtra("email");
            final String password = intent.getStringExtra("password");

            editEmail.setText(email);
            editPassword.setText(password);
            firebaseAuth(intent.getStringExtra("email"), intent.getStringExtra("password"));
        }
    }

    private void firebaseAuth(String email, String password) {
        final ProgressDialog progressDialog = new ProgressDialog(LogInActivity.this);
        progressDialog.setTitle(R.string.alert_title_please_wait);
        progressDialog.setMessage(getString(R.string.alert_waiting_for_server));
        progressDialog.show();

        firebaseAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    final FirebaseUser currentUser = task.getResult().getUser();

                    dbUsers.child(currentUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            kr.edcan.paebook.Utils.Application.uuid = currentUser.getUid();
                            if(dataSnapshot!=null) {
                                kr.edcan.paebook.Utils.Application.userProfile = dataSnapshot.getValue(UserProfile.class);
                            }
                            progressDialog.dismiss();

                            final Map<String, Object> data = new HashMap<>();
                            data.put("firebaseToken", FirebaseInstanceId.getInstance().getToken());
                            dbUsers.child(currentUser.getUid()).updateChildren(data);

                            final Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                            startActivity(intent);
                            finish();
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {
                            progressDialog.dismiss();
                        }
                    });
                } else {
                    progressDialog.dismiss();
                    Toast.makeText(getApplicationContext(), task.getException().getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}

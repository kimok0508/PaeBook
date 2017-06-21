package kr.edcan.paebook.Activity;

import android.app.DatePickerDialog;
import android.app.DownloadManager;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.LoginFilter;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.target.BitmapImageViewTarget;
import com.bumptech.glide.request.target.ImageViewTarget;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.gun0912.tedpermission.PermissionListener;
import com.gun0912.tedpermission.TedPermission;

import java.io.File;
import java.net.Authenticator;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import jp.wasabeef.glide.transformations.BlurTransformation;
import jp.wasabeef.glide.transformations.CropCircleTransformation;
import kr.edcan.paebook.Dialog.OptionDialog;
import kr.edcan.paebook.Manifest;
import kr.edcan.paebook.Models.UserProfile;
import kr.edcan.paebook.R;

public class RegisterActivity extends AppCompatActivity {
    private enum RequestCode {
        REQUEST_CAMERA,
        REQUSET_GALLERY
    }

    private OptionDialog profileOption;
    private FirebaseAuth auth;
    private FirebaseDatabase database;
    private DatabaseReference dbUsers;
    private FirebaseStorage storage;
    private StorageReference stUsers;
    private EditText editName, editEmail, editPassword, editPasswordConfirm;
    private TextView textBirth, textError;
    private Button btnConfirm;
    private ImageView imgProfile, imgBackground;
    private String selectedImageName = null;
    private Uri selectedImageURI = Uri.EMPTY;
    private Date birthDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        auth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();
        dbUsers = database.getReference().child("users").getRef();
        storage = FirebaseStorage.getInstance();
        stUsers = storage.getReference().child("users");

        editName = (EditText) findViewById(R.id.edit_name);
        editEmail = (EditText) findViewById(R.id.edit_email);
        editPassword = (EditText) findViewById(R.id.edit_password);
        editPasswordConfirm = (EditText) findViewById(R.id.edit_password_confirm);
        textBirth = (TextView) findViewById(R.id.text_birth);
        textError = (TextView) findViewById(R.id.text_password_error);
        btnConfirm = (Button) findViewById(R.id.btn_confirm);
        imgProfile = (ImageView) findViewById(R.id.img_profile);
        imgBackground = (ImageView) findViewById(R.id.img_background);

        imgProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectImage();
            }
        });
        editPasswordConfirm.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                final String tempPasswordConfrim = editable.toString().trim();
                final String tempPassword = editPassword.getText().toString().trim();

                if (tempPassword.equals(tempPasswordConfrim)) {
                    textError.setVisibility(View.GONE);
                } else {
                    textError.setVisibility(View.VISIBLE);
                }
            }
        });
        textBirth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new DatePickerDialog(RegisterActivity.this, new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker datePicker, int year, int monthOfYear, int dayOfMonth) {
                        final String birthText = String.format("%d년 %d월 %d일", year, monthOfYear + 1, dayOfMonth);
                        textBirth.setText(birthText);

                        final Calendar calendar = Calendar.getInstance();
                        calendar.set(year, monthOfYear + 1, dayOfMonth);
                        birthDate = calendar.getTime();
                        Log.e("gg", birthDate.getTime()+"");
                    }
                }, 1970, 0, 1).show();
            }
        });
        btnConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final String name = editName.getText().toString().trim();
                final String email = editEmail.getText().toString().trim();
                final String password = editPassword.getText().toString().trim();
                final String passwordConfirm = editPasswordConfirm.getText().toString().trim();

                if(!name.equals("") && !email.equals("") && !password.equals("") && !passwordConfirm.equals("") && birthDate != null){
                    if (password.equals(passwordConfirm)) {
                        final ProgressDialog progressDialog = new ProgressDialog(RegisterActivity.this);
                        progressDialog.setTitle(R.string.alert_title_please_wait);
                        progressDialog.setMessage(getString(R.string.alert_waiting_for_server));
                        progressDialog.show();
                        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (task.isSuccessful()) {
                                    final FirebaseUser user = task.getResult().getUser();
                                    final String uid = user.getUid();
                                    final DatabaseReference dbTarget = dbUsers.child(uid).getRef();
                                    final UserProfile userProfile = new UserProfile().setEmail(email).setName(name).setBirth(birthDate.getTime());

                                    if (!selectedImageURI.equals(Uri.EMPTY)) {
                                        final StorageReference stTarget = stUsers.child(uid);
                                        stTarget.child(selectedImageName).putFile(selectedImageURI).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                                            @Override
                                            public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                                                if (task.isSuccessful()) {
                                                    final String profileUrl = task.getResult().getDownloadUrl().toString();
                                                    sendUserProfile(email, password, progressDialog,dbTarget, userProfile.setProfileUrl(profileUrl));
                                                } else {
                                                    Toast.makeText(getApplicationContext(), task.getException().getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                                                }
                                            }
                                        });
                                    } else {
                                        sendUserProfile(email, password, progressDialog, dbTarget, userProfile);
                                    }
                                } else {
                                    Toast.makeText(getApplicationContext(), task.getException().getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                    }else{
                        Toast.makeText(getApplicationContext(), R.string.alert_password_confirm_not_match, Toast.LENGTH_SHORT).show();
                    }
                }else{
                    Toast.makeText(getApplicationContext(), R.string.alert_input_all_content, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void sendUserProfile(final String email, final String password, final ProgressDialog progressDialog,DatabaseReference dbTarget, UserProfile userProfile){
        dbTarget.setValue(userProfile).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                progressDialog.dismiss();
                if(task.isSuccessful()){
                    final Intent intent = new Intent(getApplicationContext(), LogInActivity.class);
                    intent.putExtra("email", email);
                    intent.putExtra("password", password);
                    startActivity(intent);
                    finish();
                }else{
                    Toast.makeText(getApplicationContext(), task.getException().getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void selectImage() {
        if (profileOption == null) {
            profileOption = new OptionDialog(RegisterActivity.this)
                    .setTitleFromResource(R.string.option_picture_title)
                    .addOption(R.string.option_select_camera, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            new TedPermission(RegisterActivity.this)
                                    .setPermissionListener(new PermissionListener() {
                                        @Override
                                        public void onPermissionGranted() {
                                            final String dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + "/PaeBook/";
                                            final File dirFile = new File(dir);
                                            if (!dirFile.exists()) {
                                                dirFile.mkdir();
                                            }

                                            final File imageFile = new File(dir + "Photo_" + System.currentTimeMillis() + ".jpg");
                                            final Uri targetURI = Uri.fromFile(imageFile);

                                            final Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                                            intent.putExtra(MediaStore.EXTRA_OUTPUT, targetURI);
                                            startActivityForResult(intent, RequestCode.REQUEST_CAMERA.ordinal());
                                        }

                                        @Override
                                        public void onPermissionDenied(ArrayList<String> arrayList) {
                                            Toast.makeText(getApplicationContext(), R.string.warn_permission, Toast.LENGTH_SHORT).show();
                                        }
                                    })
                                    .setPermissions(android.Manifest.permission.WRITE_EXTERNAL_STORAGE, android.Manifest.permission.READ_EXTERNAL_STORAGE)
                                    .check();
                            profileOption.dismiss();
                        }
                    })
                    .addOption(R.string.option_select_gallery, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            final Intent intent = new Intent(Intent.ACTION_PICK);
                            intent.setType(android.provider.MediaStore.Images.Media.CONTENT_TYPE);
                            intent.setData(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                            startActivityForResult(intent, RequestCode.REQUSET_GALLERY.ordinal());
                            profileOption.dismiss();
                        }
                    });
        }
        profileOption.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            final Uri imageUri = data.getData();
            final String name = "profile" + System.currentTimeMillis();
            this.selectedImageURI = imageUri;
            this.selectedImageName = name;

            Glide.with(this)
                    .load(imageUri)
                    .bitmapTransform(new BlurTransformation(this, 64))
                    .crossFade()
                    .thumbnail(0.25f)
                    .into(imgBackground);

            Glide.with(this)
                    .load(imageUri)
                    .bitmapTransform(new CropCircleTransformation(this))
                    .placeholder(R.drawable.ic_default_profile)
                    .crossFade()
                    .into(imgProfile);
        }

        super.onActivityResult(requestCode, resultCode, data);
    }
}

package kr.edcan.paebook.Activity;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.projection.MediaProjection;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.gun0912.tedpermission.PermissionListener;
import com.gun0912.tedpermission.TedPermission;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import jp.wasabeef.glide.transformations.BlurTransformation;
import jp.wasabeef.glide.transformations.CropCircleTransformation;
import kr.edcan.paebook.Dialog.OptionDialog;
import kr.edcan.paebook.Models.Comment;
import kr.edcan.paebook.Models.Post;
import kr.edcan.paebook.R;
import kr.edcan.paebook.Utils.Application;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.widget.ListPopupWindow.WRAP_CONTENT;

public class PostActivity extends AppCompatActivity {
    private enum RequestCode {
        REQUEST_CAMERA,
        REQUSET_GALLERY
    }

    private FirebaseDatabase firebaseDatabase;
    private FirebaseStorage firebaseStorage;
    private DatabaseReference dbPosts;
    private StorageReference stPosts;
    private ImageRecyclerAdapter recyclerAdapter;
    private LinearLayoutManager linearLayoutManager;
    private ArrayList<Pair<String, Uri>> arrayList = new ArrayList<>();
    private OptionDialog profileOption;

    private RecyclerView recyclerView;
    private EditText editTitle, editContent;
    private Button btnWrite, btnSelect;
    private boolean isEditMode = false;
    private TextView textWarn;
    private String editKey = null;
    private Post editPost = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post);

        init();
    }

    private void init() {
        this.setTitle(R.string.text_write_post);

        firebaseDatabase = FirebaseDatabase.getInstance();
        dbPosts = firebaseDatabase.getReference("posts").getRef();
        firebaseStorage = FirebaseStorage.getInstance();
        stPosts = firebaseStorage.getReference("posts");

        recyclerView = (RecyclerView) findViewById(R.id.recycler_images);
        recyclerAdapter = new ImageRecyclerAdapter(getApplicationContext(), arrayList);
        linearLayoutManager = new LinearLayoutManager(getApplicationContext(), LinearLayoutManager.HORIZONTAL, false);
        recyclerView.setAdapter(recyclerAdapter);
        recyclerView.setLayoutManager(linearLayoutManager);

        editTitle = (EditText) findViewById(R.id.edit_title);
        editContent = (EditText) findViewById(R.id.edit_content);
        btnSelect = (Button) findViewById(R.id.btn_select);
        btnSelect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectImage();
            }
        });
        textWarn = (TextView) findViewById(R.id.text_warn);

        btnWrite = (Button) findViewById(R.id.btn_write);
        btnWrite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final String uuid = Application.uuid;
                final String title = editTitle.getText().toString().trim();
                final String content = editContent.getText().toString().trim();

                if (uuid != null && title != null && !title.equals("") && content != null && !content.equals("")) {
                    final Post post = new Post(uuid, title, content, new ArrayList<String>(), ServerValue.TIMESTAMP);
                    final ArrayList<String> imageUrls = new ArrayList<>();
                    final DatabaseReference dbTarget = dbPosts.push();
                    final ProgressDialog progressDialog = new ProgressDialog(PostActivity.this);
                    progressDialog.setTitle(R.string.alert_title_please_wait);
                    progressDialog.setMessage(getString(R.string.alert_waiting_for_server));
                    progressDialog.show();

                    if (isEditMode) {
                        final Map<String, Object> data = new HashMap<>();
                        data.put("title", title);
                        data.put("content", content);
                        data.put("timeStamp", ServerValue.TIMESTAMP);
                        dbPosts.child(editKey).updateChildren(data).addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                progressDialog.dismiss();
                                if (task.isSuccessful()) {
                                    final Intent intent = new Intent();
                                    intent.putExtra("title", title);
                                    intent.putExtra("content", content);
                                    setResult(RESULT_OK, intent);
                                    finish();
                                }
                            }
                        });
                    } else {
                        dbTarget.setValue(post).addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()) {
                                    if (arrayList.size() > 0) {
                                        for (final Pair<String, Uri> image : arrayList) {
                                            if (image != null) {
                                                stPosts.child(uuid).child(image.first).putFile(image.second).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                                                    @Override
                                                    public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                                                        if (task.isSuccessful()) {
                                                            imageUrls.add(task.getResult().getDownloadUrl().toString());

                                                            final Map<String, Object> map = new HashMap<>();
                                                            map.put("images", imageUrls);
                                                            dbTarget.updateChildren(map).addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                @Override
                                                                public void onComplete(@NonNull Task<Void> task) {
                                                                    if (task.isSuccessful()) {
                                                                        Toast.makeText(getApplicationContext(), String.format("%d개중 %d개 이미지 업로드 성공", arrayList.size() - 1, imageUrls.size() - 1), Toast.LENGTH_SHORT).show();
                                                                    }

                                                                    if (arrayList.size() == imageUrls.size()) {
                                                                        progressDialog.dismiss();
                                                                        finish();
                                                                    }
                                                                }
                                                            });
                                                        }
                                                    }
                                                });
                                            }
                                        }
                                    } else {
                                        progressDialog.dismiss();
                                        finish();
                                    }
                                } else {
                                    Toast.makeText(getApplicationContext(), task.getException().getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                    }
                } else {
                    Toast.makeText(getApplicationContext(), R.string.alert_input_all_content, Toast.LENGTH_SHORT).show();
                }
            }
        });

        if (getIntent().getBooleanExtra("isEditMode", false)) {
            this.isEditMode = true;
            this.editKey = getIntent().getStringExtra("key");
            this.editPost = (Post) getIntent().getSerializableExtra("post");

            editTitle.setText(editPost.getTitle());
            editContent.setText(editPost.getContent());
            recyclerView.setVisibility(View.GONE);
            btnSelect.setVisibility(View.GONE);
            textWarn.setVisibility(View.VISIBLE);
        }
    }

    private void selectImage() {
        if (profileOption == null) {
            profileOption = new OptionDialog(PostActivity.this)
                    .setTitleFromResource(R.string.option_picture_title)
                    .addOption(R.string.option_select_camera, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            new TedPermission(PostActivity.this)
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
                                    .setPermissions(android.Manifest.permission.WRITE_EXTERNAL_STORAGE, android.Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.CAMERA)
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
        profileOption.getWindow().setLayout(MATCH_PARENT, WRAP_CONTENT);
        profileOption.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            final Uri imageUri = data.getData();
            if(imageUri != null && imageUri != Uri.EMPTY) {
                final String name = "image" + System.currentTimeMillis();
                arrayList.add(new Pair<String, Uri>(name, imageUri));
                recyclerAdapter.notifyItemInserted(arrayList.size());
            }else{
                Toast.makeText(getApplicationContext(), R.string.error_not_supported, Toast.LENGTH_SHORT).show();
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    public class ImageRecyclerAdapter extends RecyclerView.Adapter<ImageRecyclerAdapter.ViewHolder> {
        private Context context;
        private ArrayList<Pair<String, Uri>> arrayList = new ArrayList<>();

        public ImageRecyclerAdapter(Context context, ArrayList<Pair<String, Uri>> arrayList) {
            this.context = context;
            this.arrayList = arrayList;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
            final View view = LayoutInflater.from(context).inflate(R.layout.item_image, viewGroup, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder viewHolder, final int position) {
            final Uri uri = arrayList.get(position).second;

            if (uri != null && uri != Uri.EMPTY) {
                Glide.with(context).load(uri).into(viewHolder.img);
                viewHolder.img.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        final AlertDialog alertDialog = new AlertDialog.Builder(PostActivity.this)
                                .setTitle(R.string.text_image_delete)
                                .setMessage(R.string.text_really_delete)
                                .setPositiveButton(R.string.text_delete, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        deleteImage(position);
                                        dialogInterface.dismiss();
                                    }
                                })
                                .setNegativeButton(R.string.text_cancel, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        dialogInterface.dismiss();
                                    }
                                })
                                .show();
                    }
                });
            }
        }

        private void deleteImage(int position) {
//            final ArrayList<Pair<String, Uri>> arrayList = new ArrayList<>();
            PostActivity.this.arrayList.remove(position);
//            PostActivity.this.recyclerAdapter.notifyItemRemoved(position);
//
//            for (Pair<String, Uri> pair : PostActivity.this.arrayList) {
//                if (pair != null) arrayList.add(pair);
//            }
//            PostActivity.this.arrayList = arrayList;
            PostActivity.this.recyclerAdapter.notifyDataSetChanged();
        }

        @Override
        public int getItemCount() {
            return arrayList.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            private ImageView img;

            public ViewHolder(View itemView) {
                super(itemView);

                img = (ImageView) itemView.findViewById(R.id.img);
            }
        }
    }
}

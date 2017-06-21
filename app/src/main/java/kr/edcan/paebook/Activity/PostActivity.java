package kr.edcan.paebook.Activity;

import android.app.ProgressDialog;
import android.content.Context;
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
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
import kr.edcan.paebook.Models.Post;
import kr.edcan.paebook.R;
import kr.edcan.paebook.Utils.Application;

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
    private ArrayList<Pair<String,Uri>> arrayList = new ArrayList<>();
    private OptionDialog profileOption;

    private RecyclerView recyclerView;
    private EditText editTitle, editContent;
    private Button btnWrite, btnSelect;

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

        btnWrite = (Button) findViewById(R.id.btn_write);
        btnWrite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final String uuid = Application.uuid;
                final String title = editTitle.getText().toString().trim();
                final String content = editContent.getText().toString().trim();

                if (uuid != null && title != null && !title.equals("") && content != null && !content.equals("")) {
                    final Post post = new Post().setUuid(uuid).setTitle(title).setContent(content).setTimeStamp(ServerValue.TIMESTAMP);
                    final ArrayList<String> imageUrls = new ArrayList<>();
                    final DatabaseReference dbTarget = dbPosts.push();
                    final ProgressDialog progressDialog = new ProgressDialog(PostActivity.this);
                    progressDialog.setTitle(R.string.alert_title_please_wait);
                    progressDialog.setMessage(getString(R.string.alert_waiting_for_server));
                    progressDialog.show();

                    dbTarget.setValue(post).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if(task.isSuccessful()){
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
                                                                if(task.isSuccessful()){
                                                                    Toast.makeText(getApplicationContext(), String.format("%d개중 %d개 이미지 업로드 성공", arrayList.size() - 1, imageUrls.size() - 1), Toast.LENGTH_SHORT).show();
                                                                }

                                                                if(arrayList.size() == imageUrls.size()){
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
                                }
                            }else{
                                Toast.makeText(getApplicationContext(), task.getException().getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                } else {
                    Toast.makeText(getApplicationContext(), R.string.alert_input_all_content, Toast.LENGTH_SHORT).show();
                }
            }
        });
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
            final String name = "image" + System.currentTimeMillis();
            arrayList.add(new Pair<String, Uri>(name, imageUri));
            recyclerAdapter.notifyItemInserted(arrayList.size());
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    public class ImageRecyclerAdapter extends RecyclerView.Adapter<ImageRecyclerAdapter.ViewHolder>{
        private Context context;
        private ArrayList<Pair<String,Uri>> arrayList = new ArrayList<>();

        public ImageRecyclerAdapter(Context context, ArrayList<Pair<String,Uri>> arrayList){
            this.context = context;
            this.arrayList = arrayList;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
            final View view = LayoutInflater.from(context).inflate(R.layout.item_image, viewGroup, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder viewHolder, final int i) {
            final Uri uri = arrayList.get(i).second;

            if(uri != null && uri != Uri.EMPTY){
                Glide.with(context).load(uri).into(viewHolder.img);
                viewHolder.img.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        PostActivity.this.arrayList.remove(i);
                        ImageRecyclerAdapter.this.notifyItemRemoved(i);
                    }
                });
            }
        }

        @Override
        public int getItemCount() {
            return arrayList.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder{
            private ImageView img;

            public ViewHolder(View itemView) {
                super(itemView);

                img = (ImageView) itemView.findViewById(R.id.img);
            }
        }
    }
}

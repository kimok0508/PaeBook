package kr.edcan.paebook.Activity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
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
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

import jp.wasabeef.glide.transformations.CropCircleTransformation;
import kr.edcan.paebook.Models.Comment;
import kr.edcan.paebook.Models.Post;
import kr.edcan.paebook.Models.UserProfile;
import kr.edcan.paebook.R;
import kr.edcan.paebook.Utils.Application;

public class DetailActivity extends AppCompatActivity {
    private FirebaseDatabase firebaseDatabase;
    private DatabaseReference dbComments;
    private DatabaseReference dbUsers;
    private DatabaseReference dbPosts;
    private ImageRecyclerAdapter imageRecyclerAdapter;
    private LinearLayoutManager linearLayoutManagerForImages;
    private LinearLayoutManager linearLayoutManagerForComments;
    private CommentRecyclerAdapter commentRecyclerAdapter;
    private ArrayList<String> arrayListImages = new ArrayList<>();
    private ArrayList<Pair<String, Comment>> arrayListComments = new ArrayList<>();

    private RecyclerView recyclerViewImages, recyclerViewComments;
    private TextView textTitle, textContent, textName, textDate;
    private ImageView imgProfile;
    private EditText editComment;
    private Button btnWrite;

    private Post post;
    private String key;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        if (loadData()) init();
        else {
            Toast.makeText(getApplicationContext(), R.string.warn_no_data, Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private boolean loadData() {
        final Intent intent = getIntent();
        if (intent.hasExtra("key") && intent.hasExtra("post")) {
            this.post = (Post) intent.getSerializableExtra("post");
            this.key = intent.getStringExtra("key");
            return true;
        } else return false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_detail, menu);
        return Application.uuid.equals(post.getUuid());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == R.id.menu_delete){
            final AlertDialog alertDialog = new AlertDialog.Builder(DetailActivity.this)
                    .setTitle(R.string.text_post_delete)
                    .setMessage(R.string.text_really_delete)
                    .setPositiveButton(R.string.text_delete, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dbPosts.child(key).removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if(task.isSuccessful()){
                                        finish();
                                    }
                                }
                            });
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
        }else if(item.getItemId() == R.id.menu_edit){
            final Intent intent = new Intent(getApplicationContext(), PostActivity.class);
            intent.putExtra("post", post);
            intent.putExtra("key", key);
            intent.putExtra("isEditMode", true);
            startActivityForResult(intent,0);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == 0 && resultCode == RESULT_OK){
            final String title = data.getStringExtra("title");
            final String content = data.getStringExtra("content");
            textTitle.setText(title);
            textContent.setText(content);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void init() {
        this.setTitle(R.string.text_post);

        firebaseDatabase = FirebaseDatabase.getInstance();
        dbComments = firebaseDatabase.getReference("comments").getRef();
        dbUsers = firebaseDatabase.getReference("users").getRef();
        dbPosts = firebaseDatabase.getReference("posts").getRef();
        dbComments.child(key).addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                if (dataSnapshot != null) {
                    final String key = dataSnapshot.getKey();
                    final Comment comment = dataSnapshot.getValue(Comment.class);
                    arrayListComments.add(new Pair<String, Comment>(key, comment));
                    if (commentRecyclerAdapter != null)
                        commentRecyclerAdapter.notifyItemInserted(arrayListComments.size());
                }
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                if (dataSnapshot != null) {
                    final String key = dataSnapshot.getKey();
                    final Comment comment = dataSnapshot.getValue(Comment.class);
                    arrayListComments.remove(new Pair<String, Comment>(key, comment));
                    if (commentRecyclerAdapter != null)
                        commentRecyclerAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        imgProfile = (ImageView) findViewById(R.id.img_profile);
        textName = (TextView) findViewById(R.id.text_name);
        dbUsers.child(post.getUuid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot != null) {
                    final UserProfile userProfile = dataSnapshot.getValue(UserProfile.class);
                    final String profileUrl = userProfile.getProfileUrl();
                    final String name = userProfile.getName();
                    textName.setText(name);

                    if (profileUrl != null && !profileUrl.equals("")) {
                        Glide.with(DetailActivity.this)
                                .load(profileUrl)
                                .bitmapTransform(new CropCircleTransformation(DetailActivity.this))
                                .crossFade()
                                .placeholder(R.drawable.ic_default_profile)
                                .into(imgProfile);
                    } else {
                        imgProfile.setImageResource(R.drawable.ic_default_profile);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        textTitle = (TextView) findViewById(R.id.text_title);
        textDate = (TextView) findViewById(R.id.text_date);
        textContent = (TextView) findViewById(R.id.text_content);
        textTitle.setText(post.getTitle());
        textDate.setText(post.getTimeStamp(getApplicationContext()));
        textContent.setText(post.getContent());

        recyclerViewImages = (RecyclerView) findViewById(R.id.recycler_images);
        if(post.getImages() != null && post.getImages().size() > 0) {
            arrayListImages = post.getImages();
            imageRecyclerAdapter = new ImageRecyclerAdapter(getApplicationContext(), arrayListImages);
            linearLayoutManagerForImages = new LinearLayoutManager(getApplicationContext(), LinearLayoutManager.HORIZONTAL, false);
            recyclerViewImages.setLayoutManager(linearLayoutManagerForImages);
            recyclerViewImages.setAdapter(imageRecyclerAdapter);
            imageRecyclerAdapter.notifyDataSetChanged();
        }else recyclerViewImages.setVisibility(View.GONE);

        recyclerViewComments = (RecyclerView) findViewById(R.id.recycler_comments);
        commentRecyclerAdapter = new CommentRecyclerAdapter(getApplicationContext(), arrayListComments);
        linearLayoutManagerForComments = new LinearLayoutManager(getApplicationContext(), LinearLayoutManager.VERTICAL, false);
        recyclerViewComments.setLayoutManager(linearLayoutManagerForComments);
        recyclerViewComments.setAdapter(commentRecyclerAdapter);

        editComment = (EditText) findViewById(R.id.edit_comment);
        btnWrite = (Button) findViewById(R.id.btn_confirm);
        btnWrite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final String str = editComment.getText().toString().trim();

                if (Application.uuid != null && str != null && !str.equals("")) {
                    final Comment comment = new Comment(Application.uuid, str, ServerValue.TIMESTAMP);

                    dbComments.child(key).push().setValue(comment).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (!task.isSuccessful()) {
                                Toast.makeText(getApplicationContext(), task.getException().getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                            }else editComment.setText("");
                        }
                    });
                } else {
                    Toast.makeText(getApplicationContext(), R.string.alert_input_all_content, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    public class CommentRecyclerAdapter extends RecyclerView.Adapter<CommentRecyclerAdapter.ViewHolder> {
        private Context context;
        private ArrayList<Pair<String, Comment>> arrayList = new ArrayList<>();
        private final static int TYPE_OUT = 0;
        private final static int TYPE_IN = 1;

        public CommentRecyclerAdapter(Context context, ArrayList<Pair<String, Comment>> arrayList) {
            this.context = context;
            this.arrayList = arrayList;
        }


        @Override
        public int getItemViewType(int position) {
            final Comment comment = arrayList.get(position).second;
            if(comment.getUuid().equals(Application.uuid)) return TYPE_OUT;
            else return TYPE_IN;
        }

        @Override
        public CommentRecyclerAdapter.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
            if (viewType == TYPE_OUT) {
                final View view = LayoutInflater.from(context).inflate(R.layout.item_out_message, viewGroup, false);
                return new ViewHolderForOut(view);
            } else if(viewType == TYPE_IN){
                final View view = LayoutInflater.from(context).inflate(R.layout.item_in_message, viewGroup, false);
                return new ViewHolderForIn(view);
            }else return null;
        }

        @Override
        public void onBindViewHolder(CommentRecyclerAdapter.ViewHolder viewHolder, final int position) {
            final String key = arrayList.get(position).first;
            final Comment comment = arrayList.get(position).second;
            final String uuid = comment.getUuid();
            final String content = comment.getContent();
            final String date = comment.getTimeStamp(context);

            if (viewHolder instanceof ViewHolderForIn) {
                final ViewHolderForIn viewHolderForIn = (ViewHolderForIn) viewHolder;
                setUserProfile(uuid, viewHolderForIn.textName, viewHolderForIn.imgProfile);
                viewHolderForIn.textComment.setText(content);
                viewHolderForIn.textDate.setText(date);
            } else if (viewHolder instanceof ViewHolderForOut) {
                final ViewHolderForOut viewHolderForOut = (ViewHolderForOut) viewHolder;
                setUserProfile(uuid, viewHolderForOut.textName, viewHolderForOut.imgProfile);
                viewHolderForOut.textComment.setText(content);
                viewHolderForOut.textDate.setText(date);
                viewHolderForOut.textDelete.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        final AlertDialog alertDialog = new AlertDialog.Builder(DetailActivity.this)
                                .setTitle(R.string.text_comment_delete)
                                .setMessage(R.string.text_really_delete)
                                .setPositiveButton(R.string.text_delete, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        if(Application.uuid.equals(uuid)) {
                                            dbComments.child(DetailActivity.this.key).child(key).removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                                                @Override
                                                public void onComplete(@NonNull Task<Void> task) {
                                                    if (task.isSuccessful()) {
                                                        deleteComment(position);
                                                    }
                                                }
                                            });
                                        }
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

        private void deleteComment(int position){
//            final ArrayList<Pair<String,Comment>> arrayList = new ArrayList<>();
            DetailActivity.this.arrayListComments.remove(position);
//            DetailActivity.this.commentRecyclerAdapter.notifyItemRemoved(position);
//
//            for(Pair<String, Comment> pair : DetailActivity.this.arrayListComments){
//                if(pair != null) arrayList.add(pair);
//            }
//            DetailActivity.this.arrayListComments = arrayList;
            DetailActivity.this.commentRecyclerAdapter.notifyDataSetChanged();
        }

        private void setUserProfile(String uuid, final TextView textView, final ImageView imageView) {
            dbUsers.child(uuid).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot != null) {
                        final UserProfile userProfile = dataSnapshot.getValue(UserProfile.class);
                        final String userName = userProfile.getName();
                        final String profileUrl = userProfile.getProfileUrl();

                        if (profileUrl != null && !profileUrl.equals("")) {
                            Glide.with(context)
                                    .load(profileUrl)
                                    .bitmapTransform(new CropCircleTransformation(context))
                                    .crossFade()
                                    .placeholder(R.drawable.ic_default_profile)
                                    .into(imageView);
                        } else {
                            imageView.setImageResource(R.drawable.ic_default_profile);
                        }

                        textView.setText(userName);
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        }

        @Override
        public int getItemCount() {
            return this.arrayList.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {

            public ViewHolder(View itemView) {
                super(itemView);

            }
        }

        public class ViewHolderForIn extends CommentRecyclerAdapter.ViewHolder {
            private TextView textName, textComment, textDate;
            private ImageView imgProfile;

            public ViewHolderForIn(View itemView) {
                super(itemView);

                textName = (TextView) itemView.findViewById(R.id.text_name);
                textDate = (TextView) itemView.findViewById(R.id.text_date);
                textComment = (TextView) itemView.findViewById(R.id.text_comment);
                imgProfile = (ImageView) itemView.findViewById(R.id.img_profile);
            }
        }

        public class ViewHolderForOut extends CommentRecyclerAdapter.ViewHolder {
            private TextView textName, textComment, textDate, textDelete;
            private ImageView imgProfile;

            public ViewHolderForOut(View itemView) {
                super(itemView);

                textDelete = (TextView) itemView.findViewById(R.id.text_delete);
                textName = (TextView) itemView.findViewById(R.id.text_name);
                textDate = (TextView) itemView.findViewById(R.id.text_date);
                textComment = (TextView) itemView.findViewById(R.id.text_comment);
                imgProfile = (ImageView) itemView.findViewById(R.id.img_profile);
            }
        }
    }

    public class ImageRecyclerAdapter extends RecyclerView.Adapter<ImageRecyclerAdapter.ViewHolder> {
        private Context context;
        private ArrayList<String> arrayList = new ArrayList<>();

        public ImageRecyclerAdapter(Context context, ArrayList<String> arrayList) {
            this.context = context;
            this.arrayList = arrayList;
        }

        @Override
        public ImageRecyclerAdapter.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
            final View view = LayoutInflater.from(context).inflate(R.layout.item_image, viewGroup, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ImageRecyclerAdapter.ViewHolder viewHolder, final int i) {
            final String url = arrayList.get(i);

            if (url != null && !url.equals("")) {
                Glide.with(context).load(url).into(viewHolder.img);
            }
        }

        @Override
        public int getItemCount() {
            return this.arrayList.size();
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

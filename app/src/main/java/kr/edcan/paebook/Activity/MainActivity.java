package kr.edcan.paebook.Activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

import jp.wasabeef.glide.transformations.CropCircleTransformation;
import kr.edcan.paebook.Models.Post;
import kr.edcan.paebook.Models.UserProfile;
import kr.edcan.paebook.R;
import kr.edcan.paebook.Utils.Application;

public class MainActivity extends AppCompatActivity {
    private FirebaseDatabase firebaseDatabase;
    private DatabaseReference dbPosts;
    private PostRecyclerAdapter recyclerAdapter;
    private LinearLayoutManager linearLayoutManager;
    private ChildEventListener childEventListener;
    private ArrayList<Pair<String, Post>> arrayList = new ArrayList<>();

    private RecyclerView recyclerView;
    private FloatingActionButton fabWrite;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        init();
    }

    @Override
    protected void onResume() {
        super.onResume();
        dbPosts.addChildEventListener(childEventListener);
    }

    @Override
    protected void onPause() {
        super.onPause();
        dbPosts.removeEventListener(childEventListener);
        arrayList.clear();
        recyclerAdapter.notifyDataSetChanged();
    }

    private void init(){
        this.setTitle(R.string.text_post_list);

        firebaseDatabase = FirebaseDatabase.getInstance();
        dbPosts = firebaseDatabase.getReference("posts").getRef();
        childEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                if(dataSnapshot != null){
                    final String key = dataSnapshot.getKey();
                    final Post post = dataSnapshot.getValue(Post.class);
                    arrayList.add(new Pair<String, Post>(key, post));

                    if(recyclerAdapter != null) recyclerAdapter.notifyItemInserted(arrayList.size());
                }
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };

        recyclerView = (RecyclerView) findViewById(R.id.recycler_post);
        recyclerAdapter = new PostRecyclerAdapter(getApplicationContext(), arrayList);
        linearLayoutManager = new LinearLayoutManager(getApplicationContext(), LinearLayoutManager.VERTICAL, false);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setAdapter(recyclerAdapter);

        fabWrite = (FloatingActionButton) findViewById(R.id.fab_write);
        fabWrite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final Intent intent = new Intent(getApplicationContext(), PostActivity.class);
                startActivity(intent);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return Application.uuid != null;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == R.id.menu_logout){
            final FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
            firebaseAuth.signOut();
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    public class PostRecyclerAdapter extends RecyclerView.Adapter<PostRecyclerAdapter.ViewHolder>{
        private Context context;
        private ArrayList<Pair<String, Post>> arrayList = new ArrayList<>();
        private FirebaseDatabase firebaseDatabase;
        private DatabaseReference dbUsers;

        public PostRecyclerAdapter(Context context, ArrayList<Pair<String, Post>> arrayList){
            this.context = context;
            this.arrayList = arrayList;
            this.firebaseDatabase = FirebaseDatabase.getInstance();
            this.dbUsers = firebaseDatabase.getReference("users").getRef();
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
            final View view = LayoutInflater.from(context).inflate(R.layout.item_post, viewGroup, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final ViewHolder viewHolder, int i) {
            final String key = arrayList.get(i).first;
            final Post post = arrayList.get(i).second;
            final String uuid = post.getUuid();
            final String title = post.getTitle();
            final String date = post.getTimeStamp(context);

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
                                    .into(viewHolder.imgProfile);
                        } else {
                            viewHolder.imgProfile.setImageResource(R.drawable.ic_default_profile);
                        }

                        viewHolder.textName.setText(userName);
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });

            viewHolder.textTitle.setText(title);
            viewHolder.textDate.setText(date);
            viewHolder.rootView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    final Intent intent = new Intent(context, DetailActivity.class);
                    intent.putExtra("key", key);
                    intent.putExtra("post", post);
                    startActivity(intent);
                }
            });
        }

        @Override
        public int getItemCount() {
            return arrayList.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder{
            private LinearLayout rootView;
            private ImageView imgProfile;
            private TextView textName;
            private TextView textDate;
            private TextView textTitle;

            public ViewHolder(View itemView) {
                super(itemView);

                this.rootView = (LinearLayout) itemView.findViewById(R.id.root_view);
                this.imgProfile = (ImageView) itemView.findViewById(R.id.img_profile);
                this.textName = (TextView) itemView.findViewById(R.id.text_name);
                this.textDate = (TextView) itemView.findViewById(R.id.text_date);
                this.textTitle = (TextView) itemView.findViewById(R.id.text_title);
            }
        }
    }
}

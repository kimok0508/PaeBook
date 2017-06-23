package kr.edcan.paebook.Service;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;

import java.util.HashMap;
import java.util.Map;

import kr.edcan.paebook.Utils.Application;

/**
 * Created by mac on 2017. 6. 13..
 */

public class FirebaseInstanceIdService extends com.google.firebase.iid.FirebaseInstanceIdService {

    @Override
    public void onTokenRefresh() {
        super.onTokenRefresh();

        final FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
        final DatabaseReference dbUsers = firebaseDatabase.getReference("users").getRef();
        final Map<String, Object> data = new HashMap<>();
        data.put("firebaseToken", FirebaseInstanceId.getInstance().getToken());
        if(Application.uuid != null) dbUsers.child(Application.uuid).updateChildren(data);
    }
}

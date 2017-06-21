package kr.edcan.paebook.Utils;

import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessaging;

import kr.edcan.paebook.Models.UserProfile;

/**
 * Created by mac on 2017. 6. 13..
 */

public class Application extends android.app.Application {
    public static UserProfile userProfile = null;
    @Override
    public void onCreate() {
        super.onCreate();

        FirebaseDatabase.getInstance().setPersistenceEnabled(true);
    }
}

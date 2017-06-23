package kr.edcan.paebook.Models;

import android.content.Context;

import java.text.SimpleDateFormat;
import java.util.Date;

import kr.edcan.paebook.R;

/**
 * Created by mac on 2017. 6. 20..
 */

public class UserProfile {
    private String email;
    private String name;
    private String profileUrl;
    private long birth;
    private String firebaseToken = "";

    public UserProfile(){}

    public UserProfile(String email, String name, String profileUrl, long birth){
        this.email = email;
        this.name = name;
        this.profileUrl = profileUrl;
        this.birth = birth;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setProfileUrl(String profileUrl) {
        this.profileUrl = profileUrl;
    }

    public void setBirth(long birth) {
        this.birth = birth;
    }

    public void setFirebaseToken(String firebaseToken) {
        this.firebaseToken = firebaseToken;
    }

    public String getFirebaseToken() {
        return firebaseToken;
    }

    public UserProfile build(){
        return this;
    }

    public String getEmail(){
        return this.email;
    }

    public String getName(){
        return this.name;
    }

    public String getProfileUrl(){
        return this.profileUrl;
    }

    public String getBirth(Context context){
        final SimpleDateFormat format = new SimpleDateFormat(context.getResources().getString(R.string.text_date_format));
        final Date birthDate = new Date(this.birth);
        return format.format(birthDate);
    }
}

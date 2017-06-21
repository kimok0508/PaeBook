package kr.edcan.paebook.Models;

import android.content.Context;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import kr.edcan.paebook.R;

/**
 * Created by mac on 2017. 6. 22..
 */

public class Post implements Serializable {
    private String uuid;
    private String title;
    private String content;
    private ArrayList<String> images;
    private Object timeStamp;

    public Post(){}

    public Post(String uuid, String title, String content, ArrayList<String> images, Object timeStamp){
        this.uuid = uuid;
        this.title = title;
        this.content = content;
        this.images = images;
        this.timeStamp = timeStamp;
    }

    public Post setUuid(String uuid){
        this.uuid = uuid;
        return this;
    }

    public Post setContent(String content){
        this.content = content;
        return this;
    }

    public Post setTitle(String title){
        this.title = title;
        return this;
    }

    public Post setImages(ArrayList<String> images){
        this.images = images;
        return this;
    }

    public Post setTimeStamp(Object timeStamp){
        this.timeStamp = timeStamp;
        return this;
    }

    public String getTitle(){
        return this.title;
    }

    public String getUuid(){
        return this.uuid;
    }

    public String getContent(){
        return this.content;
    }

    public ArrayList<String> getImages(){
        return this.images;
    }

    public String getTimeStamp(Context context){
        if (this.timeStamp == null) return "";

        final long timeStamp = (long) this.timeStamp;
        final SimpleDateFormat format = new SimpleDateFormat(context.getResources().getString(R.string.text_date_format_detail));
        final Date date = new Date(timeStamp);
        return format.format(date);
    }
}

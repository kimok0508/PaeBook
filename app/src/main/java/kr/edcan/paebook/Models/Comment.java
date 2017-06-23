package kr.edcan.paebook.Models;

import android.content.Context;

import java.text.SimpleDateFormat;
import java.util.Date;

import kr.edcan.paebook.R;

/**
 * Created by mac on 2017. 6. 22..
 */

public class Comment {
    private String uuid;
    private String content;
    private Object timeStamp;

    public Comment(){}

    public Comment(String uuid, String content, Object timeStamp){
        this.uuid = uuid;
        this.content = content;
        this.timeStamp = timeStamp;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setTimeStamp(Object timeStamp) {
        this.timeStamp = timeStamp;
    }

    public String getUuid(){
        return this.uuid;
    }

    public String getContent(){
        return this.content;
    }

    public String getTimeStamp(Context context){
        if (this.timeStamp == null) return "";

        final long timeStamp = (long) this.timeStamp;
        final SimpleDateFormat format = new SimpleDateFormat(context.getResources().getString(R.string.text_date_format_detail));
        final Date date = new Date(timeStamp);
        return format.format(date);
    }
}

package kr.edcan.paebook.Dialog;

import android.app.Dialog;
import android.content.Context;
import android.graphics.LinearGradient;
import android.os.Bundle;
import android.support.v7.app.AppCompatDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import kr.edcan.paebook.R;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

/**
 * Created by mac on 2017. 6. 14..
 */

public class OptionDialog extends AppCompatDialog{
    private LinearLayout linearOptions;
    private TextView textTitle;

    public OptionDialog(Context context) {
        super(context);
        setContentView(R.layout.dialog_option);

        getWindow().setLayout(MATCH_PARENT, WRAP_CONTENT);

        linearOptions = (LinearLayout) findViewById(R.id.linear_options);
        textTitle = (TextView) findViewById(R.id.text_title);
    }

    public OptionDialog setTitleFromResource(int resId){
        final String text = getContext().getResources().getString(resId);
        return setTitle(text);
    }

    public OptionDialog setTitle(String title){
        textTitle.setText(title);
        return this;
    }

    public OptionDialog addOption(int resId, View.OnClickListener onClickListener){
        final String text = getContext().getResources().getString(resId);
        return addOption(text, onClickListener);
    }

    public OptionDialog addOption(String optionName, View.OnClickListener onClickListener){
        final View view = LayoutInflater.from(getContext()).inflate(R.layout.item_option, linearOptions, false);
        final TextView textOption = (TextView) view.findViewById(R.id.text_option);
        textOption.setText(optionName);
        view.setOnClickListener(onClickListener);
        linearOptions.addView(view);

        return this;
    }
}

package de.dipf.edutec.thriller.experiencesampling.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Window;
import android.widget.TextView;

import de.dipf.edutec.thriller.experiencesampling.R;


public class ProgressDialog {

    public static ProgressDialog progressDialog = null;
    private Dialog cDialog;
    private TextView tv_progressdialog_status;


    public static ProgressDialog getInstance(){
        if (progressDialog == null){
            progressDialog = new ProgressDialog();
        }
        return progressDialog;
    }

    public void showProgress(Context context, String message){
        cDialog = new Dialog(context);

        /*Making the ProgressBar Background transparent*/
        ColorDrawable dialogColor = new ColorDrawable(Color.WHITE);
        cDialog.getWindow().setBackgroundDrawable(dialogColor);
        cDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        cDialog.setContentView(R.layout.dialog_progress);
        cDialog.setCancelable(false);
        cDialog.setCanceledOnTouchOutside(false);
        cDialog.show();

        tv_progressdialog_status = cDialog.findViewById(R.id.tv_progressdialog_status);
        tv_progressdialog_status.setText(message);

    }

    public void hideProgress(){
        if(cDialog != null){
            cDialog.dismiss();
            cDialog = null;
        }
    }
}

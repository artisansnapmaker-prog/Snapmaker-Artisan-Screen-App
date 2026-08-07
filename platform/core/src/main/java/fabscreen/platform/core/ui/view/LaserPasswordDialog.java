package fabscreen.platform.core.ui.view;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;

import fabscreen.platform.base.helper.DimensUtils;
import fabscreen.platform.core.R;

public class LaserPasswordDialog {

    public EditText mEtLaserPassword;
    public Button mBtnConfirm;
    private static LaserPasswordDialog sInstance;

    public AlertDialog mDialog;


    public static LaserPasswordDialog create(Context context) {
        // create dialog
        final AlertDialog.Builder builder = new AlertDialog.Builder(context, fabscreen.platform.core.R.style.DialogTheme);
        final Activity activity = (Activity) context;

        // Create new dialog and config
        AlertDialog dialog = builder.create();

        final LayoutInflater inflater = activity.getLayoutInflater();
        final View view = inflater.inflate(R.layout.dialog_laser_password, null);
        dialog.setView(view);

        sInstance = new LaserPasswordDialog();
        sInstance.mDialog = dialog;

        sInstance.mEtLaserPassword = view.findViewById(R.id.et_laser_password);
        sInstance.mBtnConfirm = view.findViewById(R.id.btn_laser_password_confirm);

        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                if (!TextUtils.isEmpty(sInstance.mEtLaserPassword.getText().toString())) {
                    sInstance.mEtLaserPassword.setText("");
                }
            }
        });

        return sInstance;
    }

    public LaserPasswordDialog setOnClick(OnEditClickListener listener) {

        mBtnConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onClick(mEtLaserPassword.getText().toString());
            }
        });

        return this;
    }

    public void setEditValue(String value) {
        mEtLaserPassword.setText(value);
    }

    public LaserPasswordDialog canOutsideTouch(boolean canTouch) {
        mDialog.setCanceledOnTouchOutside(canTouch);
        return this;
    }

    public void show() {
        mDialog.show();

        Window window = mDialog.getWindow();
        if (window == null) return;
        window.setLayout(
                DimensUtils.dp2px(580, mDialog.getContext()),
                WindowManager.LayoutParams.WRAP_CONTENT);

    }

    public void dismiss() {
        if (mDialog.isShowing()) {
            mDialog.dismiss();
        }
    }

    public boolean isShowing() {
        return mDialog.isShowing();
    }

    public interface OnEditClickListener {
        void onClick(String value);
    }

}

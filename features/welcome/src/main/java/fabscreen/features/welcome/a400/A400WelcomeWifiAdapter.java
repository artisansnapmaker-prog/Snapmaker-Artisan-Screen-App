package fabscreen.features.welcome.a400;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.google.android.material.progressindicator.CircularProgressIndicator;

import java.util.ArrayList;
import java.util.List;

import fabscreen.features.welcome.R;
import fabscreen.platform.base.lib.network.AccessPoint;

public class A400WelcomeWifiAdapter extends BaseAdapter implements AdapterView.OnItemClickListener {
    private List<AccessPoint> mAccessPointList;
    private OnItemClickListener mOnItemClickListener;
    private Context mContext;

    public A400WelcomeWifiAdapter() {
        mAccessPointList = new ArrayList<>();
    }

    public void setAccessPoints(List<AccessPoint> accessPoints, Context context) {
        mAccessPointList = accessPoints;
        mContext = context;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        mOnItemClickListener = listener;
    }

    @Override
    public int getCount() {
        return mAccessPointList.size();
    }

    @Override
    public AccessPoint getItem(int position) {
        return mAccessPointList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_a400_welcome_access_point, parent, false);
        }

        TextView ssid = convertView.findViewById(R.id.tv_access_point_ssid);
        ImageView ivEncrypted = convertView.findViewById(R.id.iv_settings_ap_item_has_authentication);
        ImageView ivWfi = convertView.findViewById(R.id.iv_access_point_signal);
        CircularProgressIndicator progressIndicator = convertView.findViewById(R.id.progress_scan);
        ImageView ivSuc = convertView.findViewById(R.id.iv_access_point_suc);

        AccessPoint accessPoint = getItem(position);
        ssid.setText(accessPoint.getSSID());

        boolean isConnect = mAccessPointList.get(position).getConnectState() == AccessPoint.ConnectState.CONNECTED;
        boolean isConnectIng = mAccessPointList.get(position).getConnectState() == AccessPoint.ConnectState.CONNECTING;
        boolean isEncrypted = mAccessPointList.get(position).isEncrypted();

        ivSuc.setVisibility(isConnect ? View.VISIBLE : View.GONE);
        progressIndicator.setVisibility(isConnectIng ? View.VISIBLE : View.GONE);
        ivEncrypted.setBackgroundResource(isEncrypted ? R.drawable.ic_a400_ap_lock : R.drawable.ic_a400_ap_unlock);
        ivWfi.setBackgroundResource(getSignalResource(mAccessPointList.get(position).getRssi()));
        ssid.setTextColor(ContextCompat.getColor(mContext, isConnect || isConnectIng ? R.color.palette_white_pure : R.color.palette_white_silver));
        return convertView;
    }

    private int getSignalResource(float rssi) {
        if (rssi >= -75) {
            return R.drawable.ic_a400_signal_good;
        } else if (rssi >= -85) {
            return R.drawable.ic_a400_signal_fair;
        } else {
            return R.drawable.ic_a400_signal_poor;
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        AccessPoint accessPoint = getItem(position);

        if (mOnItemClickListener != null) {
            mOnItemClickListener.onClick(accessPoint);
        }
    }

    public interface OnItemClickListener {
        void onClick(AccessPoint accessPoint);
    }
}

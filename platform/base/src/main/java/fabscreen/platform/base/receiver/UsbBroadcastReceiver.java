package fabscreen.platform.base.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;

import fabscreen.platform.lib.LogHelper;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

public class UsbBroadcastReceiver extends BroadcastReceiver {
    public static final String ACTION_USB_PERMISSION = "com.snapmaker.USB_PERMISSION";
    private UsbListener mUsbListener;
    private Subject<Intent> mUsbDevicesSubject = PublishSubject.create();
    private Disposable subscribe;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (subscribe != null) mUsbDevicesSubject.onNext(intent);
    }

    public void setUsbListener(UsbListener mUsbListener) {
        this.mUsbListener = mUsbListener;
        if (subscribe != null && !subscribe.isDisposed()) subscribe.dispose();
        subscribe = mUsbDevicesSubject
                .subscribeOn(Schedulers.io())
                .subscribe(this::switchIntent, LogHelper::log);
    }

    private void switchIntent(Intent intent) {
        String action = intent.getAction();
        if (action == null) {
            return;
        }
        switch (action) {
            case ACTION_USB_PERMISSION: {
                UsbDevice usbDevice = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                    if (usbDevice != null) {
                        mUsbListener.devicePermissionGranted(usbDevice);
                    }
                } else {
                    mUsbListener.devicePermissionDenied(usbDevice);
                }
                break;
            }

            case UsbManager.ACTION_USB_DEVICE_ATTACHED: {
                UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (device != null) {
                    try {
                        mUsbListener.deviceAttached(device);
                    } catch (Exception e) {
                        LogHelper.log(e);
                    }
                }
                break;
            }

            case UsbManager.ACTION_USB_DEVICE_DETACHED: {
                UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (device != null) {
                    mUsbListener.deviceDetached(device);
                }
                break;
            }
            default:
                break;
        }
    }

    public interface UsbListener {
        void deviceAttached(UsbDevice usbDevice) throws Exception;

        void deviceDetached(UsbDevice usbDevice);

        void devicePermissionGranted(UsbDevice usbDevice);

        void devicePermissionDenied(UsbDevice usbDevice);
    }
}

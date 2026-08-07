package fabscreen.platform.base.view.debugtool;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

import fabscreen.platform.base.R;
import fabscreen.platform.base.instantiation.ServiceContainer;
import fabscreen.platform.base.service.IPreferences;
import fabscreen.platform.base.service.IRemote;
import fabscreen.platform.base.service.IRouter;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;


public class FloatWindow implements DefaultLifecycleObserver {
    CompositeDisposable mDisposable = new CompositeDisposable();
    private View floatView;
    private DebugButton debugButton;
    private boolean mIsDeveloper;

    private long mTime = 0;
    private int mTouchCount = 0;

    protected IRouter mRouter;
    private Context mContext;

    @Override
    public void onCreate(@NonNull LifecycleOwner owner) {
        IRemote remote = ServiceContainer.getInstance().getService(IRemote.class);
        if (owner instanceof Activity) {
            mRouter = ServiceContainer.getInstance().getService(IRouter.class);
            Context context = (Context) owner;
            mContext = context;
            ViewGroup rootView = (ViewGroup) ((Activity) owner).findViewById(android.R.id.content).getRootView();
            floatView = LayoutInflater.from((Context) owner).inflate(R.layout.view_float_debug, rootView, false);
            rootView.addView(floatView);
            debugButton = floatView.findViewById(R.id.debug_button);
            // Show different bg color depend on whether luban is connected.
            if (remote != null) {
                mDisposable.add(remote.getRemoteConnectedObservable()
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(state -> debugButton.setBackgroundResource(state == 2 ? R.drawable.all_button_round_orange : R.drawable.all_button_round_blue)));
            }

            debugButton.setOnClickListener(v -> {
                Intent intent = new Intent(context, MockMachineActivity.class);
                context.startActivity(intent);
            });

            // Shooting using green screen code
//            Button goToGreenScreen = floatView.findViewById(R.id.btn_green_screen);
//            goToGreenScreen.setVisibility(ServiceContainer.getInstance().getService(IMachine.class).getMachineInfoSubjectHolder().getValue().seriesId == IMachine.MachineSeries.A ?
//                    View.VISIBLE : View.GONE);
//            goToGreenScreen.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    long currentTime = SystemClock.elapsedRealtime();
//                    if (currentTime - mTime < 500) {
//                        mTouchCount += 1;
//                    } else {
//                        mTouchCount = 1;
//                    }
//                    mTime = currentTime;
//                    if (mTouchCount >= 3) {
//                        ServiceContainer.getInstance().getService(IRouter.class).routeToGreenScreen().start(context);
//                    }
//                }
//            });

        }
    }

    @Override
    public void onDestroy(@NonNull LifecycleOwner owner) {
        // dispose
        mDisposable.clear();
        mDisposable = null;
    }

    @Override
    public void onResume(@NonNull LifecycleOwner owner) {
        mIsDeveloper = ServiceContainer.getInstance().getService(IPreferences.class).getHelper().getMachineDeveloper();
        hideView(mIsDeveloper);
    }

    public void hideView(boolean isDeveloper) {
        debugButton.setVisibility(isDeveloper ? View.VISIBLE : View.GONE);
    }

}

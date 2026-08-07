package fabscreen.platform.base.service.machine;

import android.content.Context;

import androidx.annotation.Nullable;

import com.orhanobut.logger.Logger;

import java.util.concurrent.TimeUnit;

import fabscreen.platform.base.R;
import fabscreen.platform.base.lib.fabserver.RetryWithDelay;
import fabscreen.platform.base.service.IAppService;
import fabscreen.platform.base.service.IPreferences;
import fabscreen.platform.base.service.machine.connection.BlueToothConnection;
import fabscreen.platform.base.service.machine.connection.IConnection;
import fabscreen.platform.base.service.machine.connection.MockConnection;
import fabscreen.platform.base.service.machine.connection.SerialPortConnection;
import fabscreen.platform.base.service.machine.protocol.IProtocol;
import fabscreen.platform.base.service.machine.protocol.SACPProtocol;
import fabscreen.platform.base.service.machine.protocol.ScreenAsServer;
import fabscreen.platform.base.service.machine.structure.ResponseStructure;
import fabscreen.platform.base.service.machine.structure.SubscribeStructure;
import fabscreen.platform.base.view.DecisionDialog;
import fabscreen.platform.lib.LogHelper;
import io.reactivex.Observable;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.PublishSubject;

public class MachineConnectionController implements IProtocol.HeartBeatListener {
    IProtocol mProtocol;
    IConnection mConnection;
    protected IAppService mAppService;

    private boolean mIsBluetooth = false;

    private DecisionDialog mBusyDialog;

    private BehaviorSubject<Boolean> mISConnectSubject;
    BehaviorSubject<IStructure> mNotificationSubject = BehaviorSubject.create();
    BehaviorSubject<byte[]> mProxyDataSubject = BehaviorSubject.create();
    private BehaviorSubject<MachineStatus> mMachineStatusSubject;
    private final PublishSubject<Boolean> mMachineBusyStatusSubject = PublishSubject.create();
    CompositeDisposable mDisposables = new CompositeDisposable();

    public MachineConnectionController(BehaviorSubject<MachineStatus> statusSubject, IPreferences preferences, IAppService appService) {
        mMachineStatusSubject = statusSubject;
        mAppService = appService;
        mProtocol = new SACPProtocol(preferences, mProxyDataSubject);
        if (preferences.getHelper().getMockEnabled()) {
            mConnection = new MockConnection(mProtocol, preferences);
        } else {
            mConnection = new SerialPortConnection();
        }

        // BehaviorSubject will emit the last status, so it's ok to listen after connection established.
        listenToConnectChange();
        Disposable subscribe = mMachineBusyStatusSubject.observeOn(AndroidSchedulers.mainThread())
                        .subscribe(isMachineBusy -> {
                            DecisionDialog decisionDialog = DecisionDialog.create(mAppService.getNowViewContext())
                                    .setDialogStatus(DecisionDialog.BTN_ONE, false, false, false, true)
                                    .setType(DecisionDialog.WARMING_TYPE)
                                    .setTitle(R.string.a400_system_dialog_warning_machine_is_busy_title)
                                    .setContent(R.string.a400_system_dialog_warning_machine_is_busy_content)
                                    .setFirstTv(R.string.all_btn_got_it, R.color.select_dialog_white_txt, ((dialog, which) -> {
                                        dialog.dismiss();
                                    }));
                            decisionDialog.show();
                            if (mBusyDialog != null && mBusyDialog.isShowing()) {
                                mBusyDialog.dismiss();
                            }
                            mBusyDialog = decisionDialog;
                        }, LogHelper::log);
    }

    public MachineConnectionController(String blueAddress, IPreferences preferences, boolean isBluetooth, Context context, IAppService appService, BehaviorSubject<Boolean> iSConnectSubject) {
        mIsBluetooth = isBluetooth;
        mAppService = appService;
        mISConnectSubject = iSConnectSubject;
        mProtocol = new SACPProtocol(preferences, mProxyDataSubject);
        if (preferences.getHelper().getMockEnabled()) {
            mConnection = new MockConnection(mProtocol, preferences);
        } else {
            mConnection = new BlueToothConnection(blueAddress, context);
        }
        listenToConnectChange();
    }

    private void listenToConnectChange() {
        if (mIsBluetooth) {
            mDisposables.add(mConnection.getConnectionStatusObservable()
                    .subscribe(connected -> {
                        if (connected) {
                            Logger.i("Bluetooth device connected.");
                            mProtocol.bindConnection(mConnection);
                            mISConnectSubject.onNext(true);
                        } else {
                            mProtocol.disconnect();
                            mISConnectSubject.onNext(false);
                        }
                    }, LogHelper::log));
        } else {
            mDisposables.add(
                    mConnection.getConnectionStatusObservable()
                            .flatMap(connected -> {
                                if (connected) {
                                    mProtocol.setHeartbeatListener(this);
                                    mProtocol.bindConnection(mConnection);
                                    return mProtocol.enableHeartbeat();
                                }
                                return Observable.just(connected);
                            })
                            .retryWhen(new RetryWithDelay(7, 1000))
                            .subscribe(connected -> {
                            }, throwable -> {
                                Logger.w("Heartbeat request timeout, throwing...");
                            })
            );
        }
    }

    public <T extends IStructure> Observable<T> request(int commandSet, int commandId, @Nullable IStructure payload, T retStruct) {
        return mProtocol.request(commandSet, commandId, 0x01, payload, retStruct)
                .doOnNext(iStructure -> {
                    if (iStructure instanceof ResponseStructure && ((ResponseStructure<?>) iStructure).isBusy()) {
                        Logger.d("Set:%d Id:%d is busy.", commandSet, commandId);
                        mMachineBusyStatusSubject.onNext(true);
                    }
                });
    }

    public <T extends IStructure> Observable<T> watch(int commandSet, int commandId, T retStruct) {
        return mProtocol.watch(commandSet, commandId, retStruct);
    }

    public Observable<ScreenAsServer.ClientRequest> listen() {
        return mProtocol.listen();
//                .doOnNext(clientRequest -> {
//                    if (mAppService.getNowViewContext() != null) {
//                        publishSubject.onNext(String.format("Machine Request commandSet:%d,commandId:%d,payload:%s", clientRequest.commandSet, clientRequest.commandId, clientRequest.payload == null ? "null" : ByteString.of(clientRequest.payload).hex()));
//                    }
//                });
    }

    public void sendResponse(int commandSet, int commandId, int sequence, IStructure payload) {
        mProtocol.sendResponse(commandSet, commandId, IProtocol.CommunicationId.CONTROLLER, sequence, payload);
    }

    public Observable<IStructure> getNotificationObservable() {
        return mNotificationSubject.hide();
    }

    @Override
    public void onHeartbeatChanged(boolean active, int machineState) {
//        Logger.d("sacp-debug: conn ctrl heartbeat changed %s", active);
        MachineStatus status = mMachineStatusSubject.getValue();
        MachineStatus newStatus = status.CreateBuilder()
                .changeConnected(active)
                .changeStatus(machineState)
                .build();
        if (status != newStatus) {
            Logger.d("onMachineStatus heartbeat changed, previous: %s\tnow: %d", status.status, machineState);
            mMachineStatusSubject.onNext(newStatus);
        }
    }

    public IConnection getConnection() {
        return mConnection;
    }

    public void proxySendRaw(byte[] bytes) {
        // TODO: 2022/4/1 check if connected
//        Logger.d("Proxy send raw to machine: %s", ByteString.of(bytes).hex());
        mConnection.write(bytes);
    }

    public Observable<byte[]> getProxyDataObservable() {
        return mProxyDataSubject.hide();
    }

    public BehaviorSubject<byte[]> getProxyDataSubject() {
        return mProxyDataSubject;
    }

    public void unWatch(int commandSet, int commandId) {
        mProtocol.unWatch(commandSet, commandId);
    }

    public Observable<ResponseStructure<IStructure>> requestHeartbeat() {
        Logger.d("restart request heartbeat");
        // Now there is no heartbeat, force "active" to false.
        onHeartbeatChanged(false, 0);
        return Observable.create((ObservableOnSubscribe<ResponseStructure<IStructure>>) emitter -> {
            SubscribeStructure subscribeStructure = new SubscribeStructure(0x01, 0xa0, 500);
            mDisposables.add(request(0x01, 0x00, subscribeStructure, new ResponseStructure<>())
                    .subscribe(response -> {
                        if (response.isSuccess()) {
                            emitter.onNext(response);
                            emitter.onComplete();
                        }
                    }));
        }).timeout(2, TimeUnit.SECONDS);
    }

    public void setAddress(String address) throws Exception {
        if (mConnection instanceof BlueToothConnection) {
            ((BlueToothConnection) mConnection).setAddress(address);
        } else {
            throw new Exception("Not for Bluetooth link");
        }
    }

    // FIXME: Temporary method for force closing connection. This is a workaround method for testing factory app.
    public void forceCloseConnection() {
        if (mConnection != null) {
            mConnection.disconnect();
        }
    }
}

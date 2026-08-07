package fabscreen.platform.base.service.remote;

import android.util.ArraySet;

import com.orhanobut.logger.Logger;

import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import fabscreen.platform.base.R;
import fabscreen.platform.base.instantiation.IServiceIdentifier;
import fabscreen.platform.base.instantiation.ServiceContainer;
import fabscreen.platform.base.legacy.server.http.HTTPServer;
import fabscreen.platform.base.legacy.server.socket.DiscoverServer;
import fabscreen.platform.base.lib.file.FabLocalFile;
import fabscreen.platform.base.lib.file.IFile;
import fabscreen.platform.base.lib.parser.IGcodeParser;
import fabscreen.platform.base.lib.print.IPrintWorkspace;
import fabscreen.platform.base.server.TCPServer;
import fabscreen.platform.base.service.IAppService;
import fabscreen.platform.base.service.IMachine;
import fabscreen.platform.base.service.IPreferences;
import fabscreen.platform.base.service.IRemote;
import fabscreen.platform.base.service.IRouter;
import fabscreen.platform.base.service.machine.IStructure;
import fabscreen.platform.base.service.machine.controller.MachineOperationStatus;
import fabscreen.platform.base.service.machine.controller.NewPrintController;
import fabscreen.platform.base.service.machine.controller.PrintEventState;
import fabscreen.platform.base.service.machine.protocol.IProtocol;
import fabscreen.platform.base.service.machine.protocol.ScreenAsServer;
import fabscreen.platform.base.service.machine.structure.BaseStructure;
import fabscreen.platform.base.service.machine.structure.RemoteFileStructure;
import fabscreen.platform.base.service.machine.structure.ResponseStructure;
import fabscreen.platform.base.service.machine.structure.prop.BoolProp;
import fabscreen.platform.base.service.machine.structure.prop.StringProp;
import fabscreen.platform.base.service.machine.structure.prop.UInt16Prop;
import fabscreen.platform.base.service.machine.structure.prop.UInt8Prop;
import fabscreen.platform.base.service.remote.proxy.IScreenProxy;
import fabscreen.platform.base.service.remote.proxy.ScreenProxy;
import fabscreen.platform.base.view.DecisionDialog;
import fabscreen.platform.base.view.SuperToastHelper;
import fabscreen.platform.lib.LogHelper;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.BehaviorSubject;
import okio.Buffer;
import okio.ByteString;

public class S30RemoteService implements IRemote, IServiceIdentifier {
    private final IMachine mMachine;
    private final IAppService mAppService;
    private final IPreferences mPreferences;
    private IScreenProxy mProxy;

    private RemoteConnectionController mRemoteConnectionController;
    private RemoteFileController mRemoteFileController;
    private RemoteLaserController mRemoteLaserController;

    private RemoteClient mTempRemoteClient;

    private DecisionDialog mRemoveDecisionDialog;
    private DecisionDialog mDecisionDialog;

    private TCPServer mScreenServer;
    private RemoteClient mAuthenticationClient = null;
    private Set<String> remoteTokens;

    private HTTPServer mServer;

    Disposable subscribe3;
    private final CompositeDisposable mDisposable = new CompositeDisposable();
    private final CompositeDisposable mListeningAuthenticationDisposable = new CompositeDisposable();
    private final BehaviorSubject<Integer> mRemoteConnectionState = BehaviorSubject.createDefault(0);

    public S30RemoteService(IMachine machine, IAppService appService, IPreferences preferences) {
        mMachine = machine;
        mAppService = appService;
        mPreferences = preferences;
        startDiscoverServer();

        startScreenServer();

        startHttpServer();

        subscribe3 = mRemoteConnectionState
                .skip(1)
//                .distinctUntilChanged()
                .filter(integer -> integer == 0)
                .subscribe(integer -> clearConnectionConfiguration(), LogHelper::log);

        Disposable a = Observable.interval(0, 1, TimeUnit.SECONDS)
                .filter(integer -> mRemoveDecisionDialog != null && mRemoveDecisionDialog.isShowing())
                .filter(integer -> mTempRemoteClient != null)
                .subscribe(i -> {
                    try {
                        mTempRemoteClient.getSocket().getOutputStream().write('a');
                    } catch (Exception ignored) {
                        try {
                            mTempRemoteClient.disconnect();
                        } catch (IOException e) {
                            LogHelper.log(e);
                        }
                    }
                }, LogHelper::log);
    }

    private void startScreenServer() {
        mScreenServer = new TCPServer(8888);
        Disposable subscribe = mScreenServer
                .getSocketSubjectObservable()
                .subscribe(socket -> {
                    if (mRemoteConnectionState.getValue() != 0) {
                        Logger.d("Connection exists, current state %d", mRemoteConnectionState.getValue());
                        return;
                    }
                    socket.setKeepAlive(true);
                    RemoteClient remoteClient = new RemoteClient(socket);
                    mListeningAuthenticationDisposable.add(
                            remoteClient.listeningAuthentication()
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(clientRequest -> {
                                        remoteClient.setRemoteConnectionState(mRemoteConnectionState);
                                        ResponseStructure<IStructure> responseStructure = new ResponseStructure<>();
                                        responseStructure.dataProp = new StringProp();
                                        // Set state as connecting
                                        mRemoteConnectionState.onNext(1);
                                        try {
                                            Buffer write = new Buffer().write(clientRequest.payload);
                                            remoteClient.setDeviceName(new StringProp().readBufferToValue(write));
                                            remoteClient.setConnectingClients(new StringProp().readBufferToValue(write));
                                            remoteClient.setToken(new StringProp().readBufferToValue(write));
                                            Logger.d("DeviceName：%s\tConnectingClients:%s:%s", remoteClient.getDeviceName(), remoteClient.getConnectingClients(), remoteClient.getToken());
                                        } catch (Exception e) {
                                            LogHelper.log(e);
                                            responseStructure.resultProp.setValue(6);
                                            remoteClient.clientProtocol.sendResponse(clientRequest.commandSet, clientRequest.commandId, IProtocol.CommunicationId.LUBAN, clientRequest.sequence, responseStructure);
                                            mRemoteConnectionState.onNext(0);
                                        }
                                        if (!mPreferences.getHelper().getRemoteAllowConnection()) {
                                            responseStructure.resultProp.setValue(201);
                                            remoteClient.clientProtocol.sendResponse(clientRequest.commandSet, clientRequest.commandId, IProtocol.CommunicationId.LUBAN, clientRequest.sequence, responseStructure);
                                            mRemoteConnectionState.onNext(0);
                                            return;
                                        }
                                        if (remoteClient.getToken() == null) {
                                            UUID uuid = UUID.randomUUID();
                                            remoteClient.setToken(uuid.toString());
                                        }
                                        remoteTokens = mPreferences.getHelper().getRemoteTokens();
                                        if (remoteTokens != null && remoteTokens.contains(remoteClient.getToken()) && mPreferences.getHelper().getConnectionVerification() == 0) {
                                            mRemoteConnectionState.onNext(2);
                                            authenticationSuccess(remoteClient);
                                            mRemoteConnectionController.sendResponse(clientRequest.commandSet, clientRequest.commandId, clientRequest.sequence, responseStructure);
                                        } else {
                                            mTempRemoteClient = remoteClient;
                                            DecisionDialog removeDecisionDialog = DecisionDialog.create(mAppService.getNowViewContext())
                                                    .setPic(R.drawable.pic_setting_remote_connection_verification)
                                                    .setWarmTv(remoteClient.getConnectingClients(), R.color.palette_grey_dim)
                                                    .setTitle(R.string.all_remote_dialog_connection_verification_title)
                                                    .setContent(R.string.all_remote_dialog_connection_verification_content)
                                                    .setDialogStatus(DecisionDialog.BTN_TWO, true, true, true, false)
                                                    .setFirstTv(R.string.all_refuse, R.color.select_dialog_white_txt, (dialog, i) -> {
                                                        dialog.dismiss();
                                                        responseStructure.resultProp.setValue(200);
                                                        remoteClient.clientProtocol.sendResponse(clientRequest.commandSet, clientRequest.commandId, IProtocol.CommunicationId.LUBAN, clientRequest.sequence, responseStructure);
                                                        try {
                                                            remoteClient.disconnect();
                                                        } catch (IOException e) {
                                                            LogHelper.log(e);
                                                        }
                                                    })
                                                    .setSecondTv(R.string.all_connect, R.color.select_dialog_blue_txt, ((dialog, which) -> {
                                                        dialog.dismiss();
                                                        mRemoteConnectionState.onNext(2);
                                                        try {
                                                            authenticationSuccess(remoteClient);
                                                        } catch (IOException e) {
                                                            mRemoteConnectionState.onNext(0);
                                                            responseStructure.resultProp.setValue(200);
                                                            e.printStackTrace();
                                                        }
                                                        mRemoteConnectionController.sendResponse(clientRequest.commandSet, clientRequest.commandId, clientRequest.sequence, responseStructure);
                                                    }));
                                            removeDecisionDialog.show();
                                            if (removeDecisionDialog.isShowing()) {
                                                if (mRemoveDecisionDialog != null && mRemoveDecisionDialog.isShowing()) {
                                                    mRemoveDecisionDialog.dismiss();
                                                }
                                                mRemoveDecisionDialog = removeDecisionDialog;
                                            }
                                        }
                                    }, LogHelper::log));
                }, LogHelper::log);
    }

    private void startHttpServer() {
        mServer = new HTTPServer();
        mServer.startServer();
    }

    @Override
    public Observable<Integer> getRemoteConnectedObservable() {
        return mRemoteConnectionState;
    }

    @Override
    public RemoteFileController getRemoteFilController() {
        return mRemoteFileController;
    }

    private void listenToRequests() {
        mDisposable.add(mRemoteConnectionController.listen().subscribe(this::handleMachineRequest, LogHelper::log));
    }

    // After successful user authentication
    private void authenticationSuccess(RemoteClient remoteClient) throws IOException {
        Logger.d("Authentication success");
        new SuperToastHelper.Builder()
                .setDrawable(R.drawable.ic_pic_a400_success_68x68)
                .setMessage(mAppService.getNowViewContext().getString(R.string.all_remote_toast_luban_connect_success))
                .build()
                .showToast(mAppService.getNowViewContext());

        if (mPreferences.getHelper().getConnectionVerification() == 0) {
            if (remoteTokens == null) {
                remoteTokens = new ArraySet<>();
            }
            remoteTokens.add(remoteClient.getToken());
//        if (remoteTokens.size()<=10){
//            remoteTokens.add(remoteClient.getToken());
//        }else {
//            String[] tokens = (String[]) remoteTokens.toArray();
//            tokens.
//        }
            mPreferences.getHelper().setRemoteTokens(remoteTokens);
        }

        mAuthenticationClient = remoteClient;
        mAuthenticationClient.setIsAuthentication(true);
        mAuthenticationClient.updateProtocol(mMachine.getConnectionController().getProxyDataSubject());
        mRemoteConnectionController = new RemoteConnectionController(mAuthenticationClient);

        if (mProxy != null) {
            mProxy.destroy();
        }

        mProxy = new ScreenProxy(mMachine.getConnectionController(), mRemoteConnectionController);
        mRemoteFileController = new RemoteFileController(mMachine, mRemoteConnectionController, mAppService, mPreferences);
        mRemoteLaserController = new RemoteLaserController(mMachine, mRemoteConnectionController, mRemoteFileController, mAppService, mPreferences);
        // bind listen
        listenToRequests();
        mListeningAuthenticationDisposable.clear();

        if (mPreferences.getHelper().getRemoteSafeMode()) {
            mDecisionDialog = DecisionDialog.create(mAppService.getNowViewContext())
                    .setTitle(R.string.a400_remote_state_title)
                    .setContent(String.format("%s (%s)", remoteClient.getDeviceName(), remoteClient.getConnectingClients()))
                    .setDialogStatus(DecisionDialog.BTN_ONE, false, false, true, true)
                    .setFirstTv(R.string.all_disconnect, R.color.select_dialog_red_txt, ((dialog, which) -> {
                        ServiceContainer.getInstance().getService(IRemote.class).clearConnection();
                        dialog.dismiss();
                    }));
            mDecisionDialog.show();
        }
    }

    private void handleMachineRequest(ScreenAsServer.ClientRequest request) throws IOException {
        if (request.commandSet * 0x100 + request.commandId != 0xb00b) {
            Logger.d("Remote controller request %s, %s\ndata is %s\n sequence is %s", Integer.toHexString(request.commandSet), Integer.toHexString(request.commandId), ByteString.of(request.payload).hex(), Integer.toHexString(request.sequence));
        }
        switch (request.commandSet * 0x100 + request.commandId) {
            case 0x0101:
                mRemoteConnectionController.sendResponse(0x01, 0x01, 0x01, new UInt8Prop(0));
                break;
            case 0xb000:
                RemoteFileStructure fileStructure = new RemoteFileStructure();
                fileStructure.readBuffer(new Buffer().write(request.payload));
                Logger.d("file-transfer: %s", fileStructure.toString());
                mRemoteConnectionController.sendResponse(request.commandSet, request.commandId, request.sequence, new ResponseStructure<>());
                mRemoteFileController.requestStartSendFile(fileStructure);
                break;
            case 0xb001:
                BaseStructure structure = new BaseStructure() {
                    @Override
                    protected void init() {
                        addProp("md5", new StringProp());
                        addProp("index", new UInt16Prop());
                    }
                };
                structure.readBuffer(new Buffer().write(request.payload));
                mRemoteFileController.requestPackage(request.sequence, (Integer) structure.getProp("index").getValue());
                break;
            case 0xb002:
                BaseStructure finishStruct = new BaseStructure() {
                    @Override
                    protected void init() {
                        addProp("uploaded", new BoolProp());
                        addProp("name", new StringProp());
                        addProp("md5", new StringProp());
                    }
                };
                finishStruct.readBuffer(new Buffer().write(request.payload));
                mRemoteFileController.onSendFileFinish(request.commandSet, request.commandId, request.sequence, finishStruct);
                break;
            case 0xb003:
                // Remote request get camera calibration data
                mRemoteLaserController.requestGet10WCameraCalibrationData(request.commandSet, request.commandId, request.sequence, null);
                break;
            case 0xb004:
                // Remote request capture photo by move
                mRemoteLaserController.requestCapturePhotoByMove(request.commandSet, request.commandId, request.sequence, request.payload);
                break;
            case 0xb005:
                // Remote request get photo by index
                mRemoteLaserController.requestGetPhotoByIndex(request.commandSet, request.commandId, request.sequence, request.payload);
                break;
            case 0xb006:
                // Remote request get calibration data photo.
                mRemoteLaserController.requestCameraCalibrationPhoto(request.commandSet, request.commandId, request.sequence, request.payload);
                break;
            case 0xb007:
                // Remote request set camera calibration data
                mRemoteLaserController.requestSet10WCameraCalibrationData(request.commandSet, request.commandId, request.sequence, request.payload);
                break;
            case 0xb008:
                startPrint(request.commandSet, request.commandId, request.sequence, request.payload.clone());
                break;
            case 0xb009:
                // laser - auto measure thickness
                mRemoteLaserController.requestAutoMeasureMaterialThickness(request.commandSet, request.commandId, request.sequence, request.payload);
                break;
            case 0xb00a:
                resumePrint(request.commandSet, request.commandId, request.sequence, request.payload.clone());
                break;
            case 0xb00b:
                mRemoteConnectionController.sendResponse(request.commandSet, request.commandId, request.sequence, new ResponseStructure<>());
                break;
            case 0xb090:
                mRemoteLaserController.requestSetTestCameraCalibrationTakePhotoVector(request.commandSet, request.commandId, request.sequence, request.payload);
                break;
            case 0xb091:
                mRemoteLaserController.requestGetTestCameraCalibrationTakePhotoVector(request.commandSet, request.commandId, request.sequence, request.payload);
                break;
            case 0x0106:
                mRemoteConnectionController.sendResponse(request.commandSet, request.commandId, request.sequence, new ResponseStructure<>());
                clearConnection();
                break;
            default:
                break;
        }
    }

    private void resumePrint(int commandSet, int commandId, int sequence, byte[] payload) {
        ServiceContainer.getInstance().getService(IMachine.class).getNewPrintController().pause();
        Disposable sub = ServiceContainer.getInstance().getService(IMachine.class).getNewPrintController().getPrintEventObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(printEvent -> {
                    if (printEvent.getPrintEventState() == PrintEventState.RESUME_SUCCESS) {
                        mRemoteConnectionController.sendResponse(commandSet, commandId, sequence, new ResponseStructure());
                    } else if (printEvent.getPrintEventState() == PrintEventState.RESUME_FAIL) {
                        ResponseStructure responseStructure = new ResponseStructure();
                        responseStructure.resultProp = new UInt8Prop(printEvent.getErrorCode());
                        mRemoteConnectionController.sendResponse(commandSet, commandId, sequence, new ResponseStructure());
                    }
                });
        mDisposable.add(sub);
    }

    // start print for test
    private void startPrint(int commandSet, int commandId, int sequence, byte[] requestPayload) throws IOException {
        ResponseStructure responseStructure = new ResponseStructure();
        int result = 0;
        if (!MachineOperationStatus.SYSTEM_STATUS_IDLE.valueEquals(ServiceContainer.getInstance().getService(IMachine.class).getNewPrintController().getPrintState())) {
            result = 13;
            // Return result.
            responseStructure.resultProp = new UInt8Prop(result);
            mRemoteConnectionController.sendResponse(commandSet, commandId, sequence, responseStructure);
            return;
        } else if (requestPayload.length == 0) {
            result = 6;
            responseStructure.resultProp = new UInt8Prop(result);
            mRemoteConnectionController.sendResponse(commandSet, commandId, sequence, responseStructure);
        } else {
            Buffer buffer = new Buffer().write(requestPayload);
            int headType = new UInt8Prop().readBufferToValue(buffer);
            String filename = new StringProp().readBufferToValue(buffer);
            String md5 = new StringProp().readBufferToValue(buffer);
            Logger.d("startPrint:\nheadType:%d,filename:%s,md5:%s", headType, filename, md5);
            File file = new File(mAppService.getFilesDir(), filename);
            // TODO: Verify that the header type and are machine-consistent?
            if (!file.exists() || filename.isEmpty()) {
                responseStructure.resultProp = new UInt8Prop(200);
                mRemoteConnectionController.sendResponse(commandSet, commandId, sequence, responseStructure);
            } else {
                IGcodeParser mParser = ServiceContainer.getInstance().getService(IGcodeParser.class);
                mParser.startParse(mAppService.getFilesDir() + "/" + filename, true, ServiceContainer.getInstance().getService(IMachine.class).getMachineInfoSubjectHolder().getValue().workType);
                Disposable ParserSub = mParser.getParseProgressObservable()
                        .throttleLast(100, TimeUnit.MILLISECONDS)
                        .distinctUntilChanged()
                        .takeUntil(progress -> progress == 100)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(progress -> {
                            if (progress == -1) {
                                responseStructure.resultProp = new UInt8Prop(202);
                                mRemoteConnectionController.sendResponse(commandSet, commandId, sequence, responseStructure);
                            } else if (progress == 100) {
                                IPrintWorkspace workspace = ServiceContainer.getInstance().getService(IPrintWorkspace.class);
                                workspace.setPrintMode(mParser.getCustomPrintMode());
                                workspace.setPrintSource(0);
                                workspace.setFileTotalLineCount(mParser.getTotalLinesCount());
                                workspace.setEstimatedTime(mParser.getEstimatedTime());
                                workspace.setFileMD5Value(md5);
                                NewPrintController printController = mMachine.getNewPrintController();
                                IFile file2 = new FabLocalFile(file);
                                Disposable sub = workspace.addFileToWorkspace(file2)
                                        .observeOn(AndroidSchedulers.mainThread())
                                        .subscribe(success -> {
                                            if (success) {
                                                printController.setStartFromRemoteFlag(true);
                                                ServiceContainer.getInstance().getService(IRouter.class).routeToPrintPage().start(mAppService.getNowViewContext());
                                            } else {
                                                printController.setStartFromRemoteFlag(false);
                                                responseStructure.resultProp = new UInt8Prop(201);
                                                mRemoteConnectionController.sendResponse(commandSet, commandId, sequence, responseStructure);
                                            }
                                        }, e -> {
                                            LogHelper.log(e);
                                            printController.setStartFromRemoteFlag(false);
                                            responseStructure.resultProp = new UInt8Prop(202);
                                            mRemoteConnectionController.sendResponse(commandSet, commandId, sequence, responseStructure);
                                        });
                                mDisposable.add(sub);
                                sub = ServiceContainer.getInstance().getService(IMachine.class).getNewPrintController().getPrintEventObservable()
                                        .observeOn(AndroidSchedulers.mainThread())
                                        .subscribe(printEvent -> {
                                            if (printEvent.getPrintEventState() == PrintEventState.STATE_SUCCESS) {
                                                mRemoteConnectionController.sendResponse(commandSet, commandId, sequence, new ResponseStructure());
                                            } else if (printEvent.getPrintEventState() == PrintEventState.START_FAIL) {
                                                responseStructure.resultProp = new UInt8Prop(printEvent.getErrorCode());
                                                mRemoteConnectionController.sendResponse(commandSet, commandId, sequence, new ResponseStructure());
                                            }
                                        });
                                mDisposable.add(sub);
                            }
                        }, e -> {
                            Logger.e(e.toString());
                            responseStructure.resultProp = new UInt8Prop(202);
                            mRemoteConnectionController.sendResponse(commandSet, commandId, sequence, responseStructure);
                        });
                mDisposable.add(ParserSub);
            }
        }
    }

    private void startDiscoverServer() {
        new DiscoverServer(mAppService.getAppContext(), mPreferences).start();
    }

    /**
     * Active disconnection
     */
    @Override
    public void clearConnection() {
        if (mAuthenticationClient != null) {
            try {
                mAuthenticationClient.disconnect();
            } catch (Exception ignored) {
                // ignored silently?
            }
        }
        mRemoteConnectionState.onNext(0);
    }

    /**
     * Disconnect logic
     */
    private void clearConnectionConfiguration() {
        if (mRemoveDecisionDialog != null && mRemoveDecisionDialog.isShowing()) {
            mRemoveDecisionDialog.dismiss();
        }

        if (mDecisionDialog != null && mDecisionDialog.isShowing()) {
            mDecisionDialog.dismiss();
        }

        mDisposable.add(mMachine.getMachineController()
                .requestBulkUnsubscribe(IProtocol.CommunicationId.LUBAN)
                .subscribe(responseStructure -> {
                    if (!mDisposable.isDisposed()) {
                        mDisposable.clear();
                    }
                }, LogHelper::log));

        if (mProxy != null) {
            mProxy.destroy();
        }

        mRemoteLaserController = null;

        if (mRemoteFileController != null) {
            mRemoteFileController.clearCache();
        }

        mRemoteFileController = null;
        mRemoteConnectionController = null;
        mAuthenticationClient = null;
        mTempRemoteClient = null;
        mDisposable.clear();
    }

    @Override
    public Observable<ResponseStructure> disconnect() {
        return mRemoteConnectionController.request(0x01, 0x06, null, new ResponseStructure());
    }

    @Override
    public RemoteClient getNowClient() {
        return mAuthenticationClient;
    }
}

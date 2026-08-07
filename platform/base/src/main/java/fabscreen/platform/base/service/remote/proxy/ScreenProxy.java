package fabscreen.platform.base.service.remote.proxy;

import fabscreen.platform.base.service.machine.MachineConnectionController;
import fabscreen.platform.base.service.machine.protocol.IProtocol;
import fabscreen.platform.base.service.remote.RemoteConnectionController;
import fabscreen.platform.lib.LogHelper;
import io.reactivex.disposables.CompositeDisposable;

public class ScreenProxy implements IScreenProxy {
    private final MachineConnectionController mMachineConnectionController;
    private final RemoteConnectionController mRemoteConnectionController;
    private final CompositeDisposable mDisposable = new CompositeDisposable();

    public ScreenProxy(MachineConnectionController machineConnectionController, RemoteConnectionController remoteConnectionController) {
        mMachineConnectionController = machineConnectionController;
        mRemoteConnectionController = remoteConnectionController;
        observeData();
    }

    private void observeData() {
        mDisposable.add(mMachineConnectionController.getProxyDataObservable()
                .subscribe(bytes -> passData(bytes[5] & 0xff, bytes), LogHelper::log));
//        mDisposable.add(mRemoteConnectionController.getProxyDataObservable()
//                .subscribe(bytes -> passData(bytes[5] & 0xff, bytes), LogHelper::log));
    }

    private void passData(int toWhom, byte[] bytes) {
//        Logger.d("---FDT--- passData to %d data:%s", toWhom, ByteString.of(bytes).hex());
//        if (mConnectedSubject.getValue() != 2) {
//            Logger.d("---FDT--- TCP Unauthenticated data，Connected：%d data:%s", mConnectedSubject.getValue(), ByteString.of(bytes).hex());
//            return;
//        }
        if (toWhom == IProtocol.CommunicationId.CONTROLLER) {
            mMachineConnectionController.proxySendRaw(bytes);
        } else if (toWhom == IProtocol.CommunicationId.LUBAN) {
            mRemoteConnectionController.proxySendRaw(bytes);
        }
    }

    @Override
    public void destroy() {
        mDisposable.clear();
    }
}

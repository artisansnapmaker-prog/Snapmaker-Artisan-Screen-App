package fabscreen.platform.base.service.remote;

import java.io.IOException;
import java.net.Socket;

import fabscreen.platform.base.service.machine.connection.IConnection;
import fabscreen.platform.base.service.machine.protocol.IProtocol;
import fabscreen.platform.base.service.machine.protocol.SACPProtocol;
import fabscreen.platform.base.service.machine.protocol.ScreenAsServer;
import fabscreen.platform.lib.LogHelper;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.BehaviorSubject;

// What is the definition of RemoteClient?
public class RemoteClient {
    IConnection mConnection;
    IProtocol clientProtocol;

    ScreenAsServer.ClientRequest mCacheClientRequest;

    private final Socket mSocket;
    boolean mIsAuthentication;
    String mDeviceName;
    String mConnectingClients;
    String mToken;

    Disposable subscribe;
    Disposable mTimeSubscribe;
    private BehaviorSubject<Integer> mRemoteConnectionState;
    BehaviorSubject<byte[]> mProxyDataSubject;

    public RemoteClient(Socket socket) throws IOException {
        mSocket = socket;
        mProxyDataSubject = BehaviorSubject.create();
        mConnection = new TCPConnection(mSocket);
        clientProtocol = new SACPProtocol(mProxyDataSubject);
        // FIXME: do not use local arguments to cache client request
        subscribe = clientProtocol.listen()
                .filter(clientRequest -> clientRequest.commandSet == 0x01 && clientRequest.commandId == 0x05)
                .take(1)
                .subscribe(clientRequest -> {
                    mCacheClientRequest = clientRequest;
                });
        clientProtocol.bindConnection(mConnection);

        // Self disconnect from client?
        mTimeSubscribe = mConnection.getConnectionStatusObservable()
                        .distinctUntilChanged()
                        .filter(aBoolean -> !aBoolean)
                        .takeUntil(aBoolean -> !aBoolean)
                        .subscribe(aBoolean -> disconnect(), LogHelper::log);


    }

    public Observable<ScreenAsServer.ClientRequest> listeningAuthentication() {
        // FIXME: Workaround for not responding for request, while listening authentication request was later than client sent.
        //  It's SHOULDN'T BE caching request, needs to fix listening problem.
        if (mCacheClientRequest != null) {
            return Observable.just(mCacheClientRequest);
        }

        return clientProtocol.listen()
                .filter(clientRequest -> clientRequest.commandSet == 0x01 && clientRequest.commandId == 0x05);

    }

    public void disconnect() throws IOException {
        // Problem: disconnect twice while service try to disconnect.
        if (clientProtocol != null) {
            clientProtocol.disconnect();
        }

        if (mConnection != null) {
            mConnection.disconnect();
        }

        if (mRemoteConnectionState != null) {
            mRemoteConnectionState.onNext(0);
        }
        if (mCacheClientRequest != null) {
            mCacheClientRequest = null;
        }
        if (subscribe != null && !subscribe.isDisposed()) subscribe.isDisposed();

        if (mTimeSubscribe != null && !mTimeSubscribe.isDisposed()) mTimeSubscribe.isDisposed();
    }

    public void setIsAuthentication(boolean b) {
        mIsAuthentication = b;
    }

    public void updateProtocol(BehaviorSubject<byte[]> proxyDataObservable) {
        if (mProxyDataSubject != null && !mProxyDataSubject.hasComplete()) {
            mProxyDataSubject.onComplete();
        }
        mProxyDataSubject = proxyDataObservable;
        clientProtocol.updateProxyDataObservable(mProxyDataSubject);
    }


    public String getDeviceName() {
        return mDeviceName;
    }

    public void setDeviceName(String deviceName) {
        this.mDeviceName = deviceName;
    }

    public String getConnectingClients() {
        return mConnectingClients;
    }

    public void setConnectingClients(String connectingClients) {
        mConnectingClients = connectingClients;
    }

    public String getToken() {
        return mToken;
    }

    public void setToken(String token) {
        this.mToken = token;
    }

    public void setRemoteConnectionState(BehaviorSubject<Integer> remoteConnectionState) {
        mRemoteConnectionState = remoteConnectionState;
    }

    public Socket getSocket() {
        return mSocket;
    }

}

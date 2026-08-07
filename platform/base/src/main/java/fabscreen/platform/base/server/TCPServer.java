package fabscreen.platform.base.server;

import com.orhanobut.logger.Logger;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

import fabscreen.platform.lib.LogHelper;
import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.BehaviorSubject;

public class TCPServer {
    private final int mPort;
    private Scheduler.Worker worker;
    private Disposable subscribe;
    private final BehaviorSubject<Socket> mSocketSubject = BehaviorSubject.create();

    public TCPServer(int port) {
        mPort = port;
        startServer();
    }

    private void startServer() {
        if (worker != null && !worker.isDisposed()) {
            worker.dispose();
            worker = null;
        }
        worker = Schedulers.io().createWorker();
        worker.schedule(() -> {
            try {
                ServerSocket serverSocket = new ServerSocket(mPort, 5);
                while (true) {
                    // accept() will block until client connected.
                    mSocketSubject.onNext(serverSocket.accept());
                }
            } catch (IOException e) {
                LogHelper.log(e);
                Logger.e("TCP Server exception!");
                retryServer();
            }
        });
    }

    private void retryServer() {
        if (subscribe != null && !subscribe.isDisposed()) {
            subscribe.dispose();
            subscribe = null;
        }
        subscribe = Observable.timer(3, TimeUnit.SECONDS)
                .subscribe(time -> startServer());
    }


    public Observable<Socket> getSocketSubjectObservable() {
        return mSocketSubject.hide();
    }

}

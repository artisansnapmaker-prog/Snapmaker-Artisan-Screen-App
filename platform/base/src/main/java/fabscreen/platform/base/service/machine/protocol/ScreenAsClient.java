package fabscreen.platform.base.service.machine.protocol;

import android.util.SparseArray;

import androidx.annotation.Nullable;

import com.orhanobut.logger.Logger;

import java.util.concurrent.TimeUnit;

import fabscreen.platform.base.service.machine.IStructure;
import fabscreen.platform.base.service.machine.connection.IConnection;
import fabscreen.platform.base.service.machine.structure.ResponseStructure;
import fabscreen.platform.base.service.machine.structure.prop.UInt8Prop;
import io.reactivex.Observable;
import io.reactivex.subjects.BehaviorSubject;
import okio.Buffer;

public class ScreenAsClient implements IClient {
    private final SACPProtocol mProtocol;
    private static final long REQUEST_TIMEOUT = 2000L;
    private static final int MAX_RETRY_NUM = 3;
    private final SparseArray<ResponseHolder> mResponseHolders = new SparseArray<>();

    public ScreenAsClient(SACPProtocol protocol) {
        mProtocol = protocol;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends IStructure> Observable<T> request(int commandSet, int commandId, int receiverId, @Nullable IStructure requestBody, T responseStruct) {
        IConnection connection = mProtocol.getConnection();
        if (connection == null) throw new IllegalStateException("Connection not available!");
        IProtocol.MessageHeader header = new IProtocol.MessageHeader();
        header.sequence = mProtocol.getNormalSequence();
        header.commandSet = commandSet;
        header.commandId = commandId;
        header.receiverId = receiverId;
        byte[] encoded = mProtocol.encode(header, requestBody);
        BehaviorSubject<IStructure> responseSubject = BehaviorSubject.create();
        ResponseHolder responseHolder = new ResponseHolder(responseSubject, responseStruct,
                Observable.intervalRange(1, 3, 100, 2000, TimeUnit.MILLISECONDS)
                        .map(time -> {
                            connection.write(encoded);
                            // Comment this unless we are debugging.
//                            SACPLogger.logScreenRequest(header, requestBody, encoded, String.valueOf(time));
                            return time;
                        })
                        .takeUntil(time -> time == 3)
                        .filter(time -> time == 3)
                        .subscribe(result -> {
                            Logger.e("Retry delay error is: commandSet:%d,commandId:%d", commandSet, commandId);
                            if (responseStruct instanceof ResponseStructure) {
                                ((ResponseStructure<?>) responseStruct).resultProp = new UInt8Prop(2);
                            }
                            responseSubject.onNext(responseStruct);
                            ResponseHolder responseHolder1 = mResponseHolders.get(header.sequence);
                            if (responseHolder1 != null) {
                                responseHolder1.close();
                            }
                            mResponseHolders.remove(header.sequence);
                        }, responseSubject::onError));

        mResponseHolders.put(header.sequence, responseHolder);
        return (Observable<T>) responseSubject.hide();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends IStructure> Observable<T> watch(int commandSet, int commandId, T responseStruct) {
        int pushSequence = mProtocol.generatePushSequence(commandSet, commandId);
        ResponseHolder responseHolder = mResponseHolders.get(pushSequence);
        if (responseHolder != null) {
            return (Observable<T>) responseHolder.getResponseSubject().hide();
        }
        BehaviorSubject<IStructure> responseSubject = BehaviorSubject.create();
        responseHolder = new ResponseHolder(responseSubject, responseStruct, null);
        mResponseHolders.put(pushSequence, responseHolder);
        return (Observable<T>) responseSubject.hide();
    }

    @Override
    public void onResponse(IProtocol.Packet p) {
        Buffer payloadBuffer = new Buffer();
        payloadBuffer.write(p.payload);
        int sequence = mProtocol.getSequenceByHeader(p.header);
        ResponseHolder responseHolder = mResponseHolders.get(sequence);
        if (responseHolder == null) return;// No watcher yet.
        BehaviorSubject<IStructure> responseSubject = responseHolder.getResponseSubject();
        IStructure responseStructure = responseHolder.getResponseStructure();
        if (responseSubject == null || responseStructure == null) return;
        try {
            responseStructure.readBuffer(payloadBuffer);
            responseSubject.onNext(responseStructure);
            // Don't log every SACP message unless we are debugging.
//            SACPLogger.logStructurePacket(p, responseStructure);
        } catch (Exception e) {
            SACPLogger.logReadableErrors(p, responseStructure, e);
        }
        // Only remove request-response sequences.(?)
        if (!SACPProtocol.isPush(p.header.commandId)) {
            responseHolder.close();
            mResponseHolders.remove(p.header.sequence);
        }
    }

    @Override
    public void unWatch(int commandSet, int commandId) {
        int pushSequence = mProtocol.generatePushSequence(commandSet, commandId);
        ResponseHolder responseHolder = mResponseHolders.get(pushSequence);
        if (responseHolder != null) {
            mResponseHolders.delete(pushSequence);
        }
    }
}

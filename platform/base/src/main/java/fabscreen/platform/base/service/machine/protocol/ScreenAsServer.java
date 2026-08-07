package fabscreen.platform.base.service.machine.protocol;

import com.orhanobut.logger.Logger;

import java.util.HashMap;
import java.util.Map;

import fabscreen.platform.base.service.machine.IStructure;
import fabscreen.platform.base.service.machine.connection.IConnection;
import io.reactivex.Observable;
import io.reactivex.subjects.Subject;
import okio.ByteString;

public class ScreenAsServer implements IServer<ScreenAsServer.ClientRequest> {
    private static final String TAG = "ScreenAsServer";
    private Subject<ClientRequest> mSubject;
    private final SACPProtocol mProtocol;
    private final Map<Integer, Integer> mSequencedSenderIds = new HashMap<>();

    public ScreenAsServer(SACPProtocol protocol) {
        mProtocol = protocol;
    }

    @Override
    public Observable<ClientRequest> listen(Subject<ClientRequest> serverSubject) {
        mSubject = serverSubject;
        return mSubject.hide();
    }

    @Override
    public void onRequest(IProtocol.Packet p) {
        ClientRequest request = new ClientRequest();
        request.commandSet = p.header.commandSet;
        request.commandId = p.header.commandId;
        request.sequence = p.header.sequence;
        request.payload = p.payload;
        mSequencedSenderIds.put(p.header.sequence, p.header.senderId);
//        Logger.t(TAG).d("Received request from %d", p.header.senderId);
        if (mSubject == null) {
            Logger.e("onRequest subject is null.");
            return;
        }
        mSubject.onNext(request);
    }

    @Override
    public void sendResponse(int commandSet, int commandId, int receiverId, int sequence, IStructure payload) {
        IConnection connection = mProtocol.getConnection();
        if (connection == null) throw new IllegalStateException("Connection not available!");
        IProtocol.MessageHeader header = new IProtocol.MessageHeader();
        // Get the sender id associated with the sequence to use as the receiver id to send response and then delete it.
        Integer senderIdWithSameSequence = mSequencedSenderIds.remove(sequence);
        header.sequence = sequence;
        // Use the param receiverId as a fallback.
        header.receiverId = senderIdWithSameSequence == null ? receiverId : senderIdWithSameSequence;
//        Logger.t(TAG).d("Sending response to %d", header.receiverId);
        header.commandSet = commandSet;
        header.commandId = commandId;
        header.attribute = IProtocol.Attribute.ACK;
        byte[] encoded = mProtocol.encode(header, payload);
        connection.write(encoded);
    }

    public static class ClientRequest {
        public int commandSet;
        public int commandId;
        public int sequence;
        public byte[] payload;
    }
}

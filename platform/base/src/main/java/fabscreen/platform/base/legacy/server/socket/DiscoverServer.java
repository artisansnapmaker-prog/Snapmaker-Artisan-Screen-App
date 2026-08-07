package fabscreen.platform.base.legacy.server.socket;


import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.SystemClock;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;

import fabscreen.platform.base.instantiation.ServiceContainer;
import fabscreen.platform.base.service.IMachine;
import fabscreen.platform.base.service.IPreferences;
import fabscreen.platform.lib.LogHelper;
import okio.Buffer;
import okio.ByteString;


/**
 * Discover Server: Socket server for device discovery.
 * <p>
 * Start server:
 * discoverServer = new DiscoverServer(context, "Snapmaker");
 * discoverServer.start()
 */
public class DiscoverServer extends Thread {
    private static final int BIND_PORT = 20054;
    private static final String DISCOVER_MESSAGE = "discover";

    private final WifiManager mWifiManager;
    private final IPreferences mPreferences;
    private DatagramSocket mSocket;

    private long mLastMills = 0;

    /**
     * DiscoverServer: bind socket and wait for client to search
     *
     * @param context Use for creating WifiManager to get current ip
     */
    public DiscoverServer(Context context, IPreferences preferences) {
        mWifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        mPreferences = preferences;
    }

    private static String intToIp(int ipAddress) {
        return (ipAddress & 0xFF) + "." +
                ((ipAddress >> 8) & 0xFF) + "." +
                ((ipAddress >> 16) & 0xFF) + "." +
                (ipAddress >> 24 & 0xFF);
    }

    public void run() {
        try {
            mSocket = new DatagramSocket(BIND_PORT);

            byte[] data = new byte[64];
            DatagramPacket packet = new DatagramPacket(data, data.length);
            while (true) {
                mSocket.receive(packet);

                String message = new String(data, 0, packet.getLength(), StandardCharsets.UTF_8);

                if (message.equals(DISCOVER_MESSAGE)) {
                    long currentMills = SystemClock.elapsedRealtime();
                    byte[] bytes = getResponse();
                    SocketAddress address = packet.getSocketAddress();
                    DatagramPacket sendPacket = new DatagramPacket(bytes, bytes.length, address);
                    mSocket.send(sendPacket);
                    if ((currentMills - mLastMills) > 1000 || mLastMills == 0) {
//                        Logger.d("Discover request, response sent.");
                    } else {
                        continue;
                    }
                    mLastMills = currentMills;
                }
            }
        } catch (IOException e) {
            LogHelper.log(e);
        } finally {
            if (mSocket != null) {
                mSocket.close();
                mSocket = null;
            }
        }
    }

    @Override
    public void interrupt() {
        if (mSocket != null) {
            mSocket.close();
            mSocket = null;
        }

        super.interrupt();
    }

    private byte[] getResponse() {

//        final String machineName = ServiceContainer.getInstance().getService(IPreferences.class).getHelper().getMachineName();
        final String machineModal = ServiceContainer.getInstance().getService(IMachine.class).getMachineInfoSubjectHolder().getValue().getModelName();
//        int printerStatus = ServiceContainer.getInstance().getService(IMachine.class).getNewPrintController().getPrintState();

        // Build response string
        // {name}@{ip}|model:{model}|status:{status}
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(String.format("%s@%s", mPreferences.getHelper().getMachineName(), getHostAddress()));
        // E.g. model:Snapmaker Artisan
        stringBuilder.append(String.format("|model:Snapmaker %s", machineModal));
        stringBuilder.append(String.format("|%s:%s", "SACP", "1"));

        String description = stringBuilder.toString();

        Buffer buffer = new Buffer();
        buffer.write(ByteString.encodeUtf8(description));
        return buffer.readByteArray();
    }

    private String getHostAddress() {
        WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
        return intToIp(wifiInfo.getIpAddress());
    }
}

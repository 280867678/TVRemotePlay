package com.android.tvremoteplay.adb;

import android.content.Context;
import android.util.Log;

import com.android.tvremoteplay.Environment;
import com.android.tvremoteplay.server.RemoteServer;
import com.android.tvremoteplay.R;
import com.cgutman.adblib.AdbBase64;
import com.cgutman.adblib.AdbConnection;
import com.cgutman.adblib.AdbCrypto;
import com.cgutman.adblib.AdbStream;
import com.cgutman.adblib.Base64;

import java.io.IOException;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayDeque;

/**
 * Created by kingt on 2018/3/7.
 */

public class AdbHelper {
    private static String TAG = "AdbHelper";
    private AdbConnection connection = null;
    private String host;
    private int port;

    private ArrayDeque<Object> sendDataDeque = new ArrayDeque<>();
    private Thread sendDataThread = null;
    private boolean running = false;
    private Context context;

    private AdbHelper(){
    }

    public boolean isRunning(){
        return running;
    }

    public void init(Context context, String host, int port) {
        this.context = context;
        this.host = host;
        this.port = port;
        this.running = true;
        this.initSDThread();
    }

    public void stop() {
        this.running = false;
        this.context = null;
        if(this.connection != null){
            try {
                this.connection.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            this.connection = null;
        }
        synchronized (sendDataDeque) {
            sendDataDeque.notifyAll();
        }
        if(sendDataThread != null && sendDataThread.isAlive()) {
            sendDataThread.interrupt();
            try {
                sendDataThread.join();
            } catch (InterruptedException e) {
            }
        }
    }


    private void  initSDThread(){
        final AdbHelper adbHelper = this;
        if(sendDataThread == null || !sendDataThread.isAlive()){
            sendDataThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    Object data = 0;
                    while (running) {
                        synchronized (sendDataDeque) {
                            try {
                                if(sendDataDeque.size() == 0)
                                    sendDataDeque.wait();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            if (sendDataDeque.size() > 0) {
                                data = sendDataDeque.pop();
                            }else{
                                continue;
                            }
                        }
                        String msg = null;
                        if(data instanceof Integer){
                            msg = "shell:input keyevent " + String.valueOf(data);
                        }else{
                            msg = "shell:input text \"" + ((String)data).replaceAll("\"", "\\\"").replaceAll("\\\\", "\\\\") + "\"";
                        }
                        try {
                            if(connection != null || try2Connect()){
                                AdbStream stream = connection.open(msg);
                                if(Environment.needDebug){
                                    Environment.debug(TAG, "???????????????adb?????????" + msg);
                                    //Environment.toastInHandler(adbHelper.context, "tvremoteplay?????????adb?????????????????????"  + msg);
                                }
                            }else {
                                if(Environment.needDebug){
                                    Environment.debug(TAG, "?????????adb?????????" + msg);
                                    //Environment.toastInHandler(adbHelper.context, "tvremoteplay???adb??????????????????????????????");
                                }
                            }
                            //stream.close();
                        } catch (Exception e) {
                            if(Environment.needDebug){
                                Environment.debug(TAG, "??????adb??????????????????" + msg, e);
                                //Environment.toastInHandler(adbHelper.context, "tvremoteplay???adb??????????????????????????????" + e.toString());
                            }
                        }
                    }
                }
            });
            sendDataThread.start();
        }
    }

    private boolean try2Connect()
    {
        try {
            Socket socket = new Socket(this.host, this.port);
            socket.setSoTimeout(1000 * 10);
            AdbCrypto crypto = null;
            try {
                crypto = AdbCrypto.generateAdbKeyPair(new AdbBase64() {
                    @Override
                    public String encodeToString(byte[] data) {
                        return Base64.encodeToString(data, 16);
                    }
                });
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }

            final AdbHelper adbHelper = this;
            if (connection == null) {
                connection = AdbConnection.create(socket, crypto);
                connection.setOnClosedListener(new AdbConnection.ConnectionOnClosedListener() {
                    @Override
                    public void onClosed() {
                        Log.i(TAG, "adb??????????????????");
                        Environment.toastInHandler(adbHelper.context, context.getString(R.string.app_name)  + "???adb????????????????????????");
                        try {
                            connection.close();
                            connection = null;
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }

            connection.connect();
            Log.i(TAG, "adb??????????????????");
            Environment.toastInHandler(adbHelper.context, context.getString(R.string.app_name)  + "???adb????????????????????????");
            return true;
        }catch(Exception e) {
            Log.e(TAG, "adb??????????????????????????????" + e.toString(), e);
            connection = null;
            return false;
        }
    }

    public void sendData(Object data){
        sendDataDeque.push(data);
        synchronized (sendDataDeque) {
            sendDataDeque.notifyAll();
        }
    }

    private static AdbHelper instance = null;
    public static AdbHelper getInstance(){
        return instance;
    }
    public static void createInstance(){
        if(instance == null){
            synchronized (AdbHelper.class){
                if(instance == null)
                    instance = new AdbHelper();
            }
        }
    }
    public static boolean initService(Context context){
        if(instance != null){
            if(!instance.isRunning()){
                instance.init(context, RemoteServer.getLocalIPAddress(context), Environment.adbServerPort);
            }
            return true;
        }
        return false;
    }
    public static void stopService(){
        if(instance != null) instance.stop();
    }
}

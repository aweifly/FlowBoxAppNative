package com.iknet.bluetoothmeasure.conn;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.UUID;

/**
 * 传统或双模蓝牙连接，断开，和数据收发
 * Created by Administrator on 2017/11/16 0016.
 */

public class BluetoothConnModel {
    private static final String TAG = BluetoothConnModel.class.getSimpleName();

    public static final UUID CUSTOM_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private BluetoothAdapter mAdapter = BluetoothAdapter.getDefaultAdapter();

    private ConnCallback connCallback;
    private BluetoothDevice mBluetoothDevice;

    private StreamThread streamThread;

    private Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg){
            switch(msg.what){
                case 101:
                    BluetoothSocket socket = (BluetoothSocket)msg.obj;
                    streamThread = new StreamThread(socket);
                    streamThread.start();
                    connCallback.connected(mBluetoothDevice);
                    break;
                case 102:
                    connCallback.read((byte[])msg.obj);
                    break;
                case 103:
                    connCallback.connectFailed(mBluetoothDevice);
                    break;
                default:
                    break;
            }
            return false;
        }
    });

    public BluetoothConnModel(ConnCallback connCallback){
        this.connCallback = connCallback;
    }

    public void connectTo(BluetoothDevice device) {
        mBluetoothDevice = device;
        SocketThread mSocketThread = new SocketThread(device);
        mSocketThread.start();
    }

    /**
     * 手动断开蓝牙连接
     */
    public void disconnectToBT(){
        if(streamThread != null){
            try{
                streamThread.getBtSocket().close();
                connCallback.disconnetBt(true);
            }catch(IOException e){
                e.printStackTrace();
                Log.v(TAG, "蓝牙断开失败");
                connCallback.disconnetBt(false);
            }
        }
    }

    /**
     * 连接成功后执行的操作
     * @param socket
     */
    private void connected(BluetoothSocket socket) {
        handler.obtainMessage(101, socket).sendToTarget();

    }

    /**
     * 向远端蓝牙写入数据
     * @param data
     */
    public void write(byte[] data){
        if(streamThread != null){
            streamThread.write(data);
        }
    }

    /**
     * Connecting as a client
     */
    private class SocketThread implements Runnable {

        private BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;
        private Thread thread;

        public SocketThread(BluetoothDevice device){
            this.thread = new Thread(this);
            mmDevice = device;
            try{
                mmSocket = device.createInsecureRfcommSocketToServiceRecord(CUSTOM_UUID);
            }catch(IOException e){
                Log.i(TAG, "创建BluetoothSocket失败");
            }

        }

        public void start() {
            this.thread.start();
        }

        @Override
        public void run(){
            mAdapter.cancelDiscovery();
            try{
                Log.i(TAG, "开始连接");
                mmSocket.connect();
            }catch(IOException e){
                Log.i(TAG, "连接失败");

                Class<?> clazz = mmDevice.getClass();
                Class<?>[] paramTypes = new Class<?>[]{int.class};

                try{
                    Log.i(TAG, "-----尝试反射连接--->");
                    Method m = clazz.getMethod("createInsecureRfcommSocket", paramTypes);
                    Object[] params = new Object[]{Integer.valueOf(1)};
                    mmSocket.close();
                    if(shouldUseFixChannel()){
                        mmSocket = (BluetoothSocket) m.invoke(mmDevice, 6);
                    }else{
                        mmSocket = (BluetoothSocket) m.invoke(mmDevice, params);
                    }
                    mmSocket.connect();
                }catch(Exception e1){
                    Log.i(TAG, "反射连接失败");
                    handler.obtainMessage(103).sendToTarget();
                    this.thread = null;
                    return;
                }
            }

            synchronized (BluetoothConnModel.this) {
                connected(mmSocket);
                Log.i(TAG, "---->[SocketThread] " + mmDevice + " 连接成功");
            }
            this.thread = null;
            Log.i(TAG, "---->END mConnectThread");

        }

        private boolean shouldUseFixChannel() {
            if (Build.VERSION.RELEASE.startsWith("4.0.")) {
                if (Build.MANUFACTURER.equals("samsung")) {
                    return true;
                }
                if (Build.MANUFACTURER.equals("HTC")) {
                    return true;
                }
            }
            if (Build.VERSION.RELEASE.startsWith("4.1.")) {
                if (Build.MANUFACTURER.equals("samsung")) {
                    return true;
                }
            }
            if (Build.MANUFACTURER.equals("Xiaomi")) {
                if (Build.VERSION.RELEASE.equals("2.3.5")) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * 蓝牙数据传输
     */
    private class StreamThread implements Runnable{
//        private final String TAG = StreamThread.class.getSimpleName();

        private BluetoothSocket socket;
        private Thread thread;
        private InputStream inStream;
        private OutputStream outStream;

        public StreamThread(BluetoothSocket socket){
            this.thread = new Thread(this, StreamThread.class.getSimpleName());
            this.socket = socket;
            try{
                inStream = socket.getInputStream();
                outStream = socket.getOutputStream();
            }catch(IOException e){
                e.printStackTrace();
            }
        }

        public void start(){
            this.thread.start();
        }

        @Override
        public void run(){
            while(socket.isConnected()){
                try{
                    int ava = inStream.available();
                    if(ava > 0){
                        byte[] b = new byte[ava];
                        inStream.read(b);
                        handler.obtainMessage(102, b).sendToTarget();

//                        StringBuffer sb = new StringBuffer();
//                        sb.append(FrameUtil.byte2hex(b));
//                        Log.v(TAG, "" + sb.toString() + "，字节数量：" + sb.length()/2);
                    }

                    thread.sleep(500);
//                    thread.wait(100);
                    Log.v(TAG, "监听蓝牙接收...");
                }catch(Exception e){
                    Log.i(TAG, "数据收发线程错误");
                    e.printStackTrace();
                }
                Log.v(TAG, "socket.isConnected:" + socket.isConnected());
            }

            Log.i(TAG, "StreamThread结束");
        }

        public boolean write(byte[] buff) {
            try {
                outStream.write(buff);
                return true;
            } catch (IOException e) {
                Log.i(TAG, "------写入错误----->" + e.getMessage());
                e.printStackTrace();
            }
            return false;
        }

        public BluetoothSocket getBtSocket(){
            return socket;
        }
    }

}

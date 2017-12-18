package com.iknet.bluetoothmeasure;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import org.jetbrains.annotations.Nullable;

import android.support.annotation.RequiresApi;
import android.text.TextUtils;
import android.util.Log;

import com.iknet.bluetoothmeasure.conn.BluetoothConnModel;
import com.iknet.bluetoothmeasure.conn.BluetoothGattConnModel;
import com.iknet.bluetoothmeasure.conn.ConnCallback;
import com.iknet.bluetoothmeasure.device.RemoteBtDeviceImp;


/**
 *
 * Created by Administrator on 2017/11/15 0015.
 */
@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
public class BluetoothService extends Service {
    private static final String TAG = BluetoothService.class.getSimpleName();

    private static String connectedBTAddress = null;  //已连接的蓝牙设备mac地址

    private MessageReceiver mBtMsgReceiver;
    private IBinder btBinder = new BtBinder();

    private RemoteBtDeviceImp remoteBtDevice;
    private BluetoothDevice remoteDevice;
    private String mAddr;

    private BluetoothConnModel mBluetoothConnModel;
    private BluetoothGattConnModel mBluetoothGattConnModel;

    @Override
    public void onCreate(){
        super.onCreate();
        init();
    }

    private void init(){
        mBtMsgReceiver = new MessageReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        registerReceiver(mBtMsgReceiver, filter);

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;// START_STICKY /* START_REDELIVER_INTENT */;;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent){
        return btBinder;
    }

    public class BtBinder extends Binder {
        public BluetoothService getService() {
            return BluetoothService.this;
        }
    }

    public void setRemoteDevice(RemoteBtDeviceImp remoteBtDevice){
        this.remoteBtDevice = remoteBtDevice;
    }

    public static String getConnectedBTAddress(){
        return connectedBTAddress;
    }

    public void connectToBT(String addr){
        if(TextUtils.isEmpty(addr)){
            return;
        }
        mAddr = addr;
        remoteDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(mAddr);// 获取远程蓝牙设备
        remoteBtDevice.getListener().connecting(remoteDevice);
        if(isSingleMode(remoteDevice)){
            if(mBluetoothGattConnModel == null){
                mBluetoothGattConnModel = new BluetoothGattConnModel(this, connCallback);
            }
            mBluetoothGattConnModel.connectTo(remoteDevice);
        }else {
            if (mBluetoothConnModel == null) {
                mBluetoothConnModel = new BluetoothConnModel(connCallback);
            }
            mBluetoothConnModel.connectTo(remoteDevice);
        }
    }

    public void disconnectToBT(){
        if(connectedBTAddress != null){
            if(mBluetoothConnModel != null){
                mBluetoothConnModel.disconnectToBT();
            }
            if(mBluetoothGattConnModel != null){
                mBluetoothGattConnModel.disconnectToBT();
            }
        }
    }

    public class MessageReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent){
            String action = intent.getAction();
            if(action.equals(BluetoothDevice.ACTION_ACL_DISCONNECTED)){
                connectedBTAddress = null;
                remoteBtDevice.disconnectBt();
            }
        }
    }

    private ConnCallback connCallback = new ConnCallback() {
        @Override
        public void connected(BluetoothDevice device){
            connectedBTAddress = device.getAddress();
            remoteBtDevice.getListener().onConnectSuccessful(device);

        }

        @Override
        public void connectFailed(BluetoothDevice device){
            remoteBtDevice.getListener().connectFailed(device);
        }

        @Override
        public void disconnetBt(boolean disconnet){
            if(disconnet){
                remoteBtDevice.disconnectBt();
            }
        }

        @Override
        public void read(byte[] data){
            remoteBtDevice.parseData(data);
        }
    };

    /** 判断是否为单模蓝牙 */
    private boolean isSingleMode(BluetoothDevice bd) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT)
            return false; // 4.4以下不支持单模
        if (bd.getType() == BluetoothDevice.DEVICE_TYPE_LE) {
            Log.v(TAG, "单模蓝牙");
            return true;
        }else{
            Log.v(TAG, "双模蓝牙");
            return false;
        }
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        unregisterReceiver(mBtMsgReceiver);
    }
}

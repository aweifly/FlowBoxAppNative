package com.iknet.bluetoothmeasure.conn;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;

import com.iknet.bluetoothmeasure.util.FrameUtil;

/**
 * 单模蓝牙连接，断开，和数据收发
 * Created by Administrator on 2017/11/16 0016.
 */
@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
public class BluetoothGattConnModel {
    private static final String TAG = BluetoothGattConnModel.class.getSimpleName();

    private ConnCallback connCallback;
    private BluetoothDevice mBluetoothDevice;
    private Context mContext;

    private BluetoothGatt bluetoothGatt;
    private BluetoothGattCharacteristic characteristicRead;
    private BluetoothGattCharacteristic characteristicWrite;

    public BluetoothGattConnModel(Context context, ConnCallback connCallback){
        mContext = context;
        this.connCallback = connCallback;
    }

    public void connectTo(BluetoothDevice device) {
        if(device == null) return;

        mBluetoothDevice = device;
//        if(mRemoteDevice != null && mRemoteDevice.equals(device) && BluetoothService.ConnectedBTAddress != null){
//            //已连接该设备，直接测量
////            mHandler.obtainMessage(BluetoothService.MESSAGE_CONNECTED, -1, -1,
////                    mRemoteDevice.getName()).sendToTarget();
//        }else{
            bluetoothGatt = device.connectGatt(mContext, true, bluetoothGattCallback);
//        }
    }

    /**
     * 手动断开蓝牙连接
     */
    public void disconnectToBT(){
        bluetoothGatt.disconnect();
    }

    /**
     * 连接成功后执行的操作
     */
    private void connected(){
        connCallback.connected(mBluetoothDevice);

    }

    private BluetoothGattCallback bluetoothGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String name = gatt.getDevice().getName();
            if(newState == BluetoothProfile.STATE_CONNECTED){
//				Log.v(TAG, "成功连接该蓝牙设备：" + name);
                Log.v(TAG, "启动发现服务:" + bluetoothGatt.discoverServices());

            }else if(newState == BluetoothProfile.STATE_DISCONNECTED){
                Log.v(TAG, "断开连接：" + name);
//                isConnected = false;
            }
            if(status == BluetoothGatt.GATT_SUCCESS){
                Log.v(TAG, "onConnectionStateChange:启动服务成功");
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if(status == BluetoothGatt.GATT_SUCCESS){
                Log.v(TAG, "onServicesDiscovered:启动服务成功");
//                isConnected = true;
//                btStateMachineGatt.start();
                //找到可用的service和characteristic
                exit:
                for(BluetoothGattService s : gatt.getServices()){
                    int charaSize = s.getCharacteristics().size();
                    if(charaSize < 2) continue;
                    characteristicRead = null;
                    characteristicWrite = null;
                    for(int i=0; i<charaSize; i++){
                        BluetoothGattCharacteristic c = s.getCharacteristics().get(i);
                        if(c.getDescriptors() != null && c.getDescriptors().size() != 0){
                            if(characteristicWrite == null && c.getProperties() == BluetoothGattCharacteristic.PROPERTY_WRITE){
                                characteristicWrite = c;
                            }else if(characteristicRead == null && c.getProperties() == BluetoothGattCharacteristic.PROPERTY_NOTIFY){
                                characteristicRead = c;
                            }
                        }
                        if(characteristicRead != null && characteristicWrite != null) break exit;
                    }
                }
                if(characteristicRead != null && characteristicWrite != null){
                    gatt.setCharacteristicNotification(characteristicRead, true);
//                    mHandler.obtainMessage(BluetoothService.MESSAGE_CONNECTED, -1, -1,
//                            mRemoteDevice.getName()).sendToTarget();
                    connected();
                }else{
                    connCallback.connectFailed(mBluetoothDevice);
                    Log.v(TAG, "未找到可用的characteristic");
//                    disconnect();
                }

            }else if(status == BluetoothGatt.GATT_FAILURE){
                connCallback.connectFailed(mBluetoothDevice);
                Log.v(TAG, "启动服务失败");
//                isConnected = false;
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            byte[] buff = characteristic.getValue();
//            Log.v(TAG, "onCharacteristicChanged:" + FrameUtil.bytes2hex(buff) + ",字节个数：" + buff.length);
//            btStateMachineGatt.addData(buff);
            connCallback.read(buff);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if(status == BluetoothGatt.GATT_SUCCESS){
                Log.v(TAG, "写入成功：" + FrameUtil.bytes2hex(characteristic.getValue()));
            }else if(status == BluetoothGatt.GATT_FAILURE){
                Log.v(TAG, "写入失败：" + FrameUtil.bytes2hex(characteristic.getValue()));
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {

        }

    };

    /**
     * 向远端蓝牙写入数据
     * @param data
     */
    public void writeCharacteristic(byte[] data){
        if(characteristicWrite == null || data == null) return;
        Log.v(TAG, "写入数据：" + FrameUtil.bytes2hex(data));

        characteristicWrite.setValue(data);
//		characteristicWrite.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
        boolean w = bluetoothGatt.writeCharacteristic(characteristicWrite);
        Log.v(TAG, "写入操作：" + w);
        if(!w){
//            disconnect();
        }

    }


}

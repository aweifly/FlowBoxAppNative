package com.iknet.bluetoothmeasure;

import android.bluetooth.BluetoothDevice;

import java.util.List;

/**
 * Created by Administrator on 2017/11/15 0015.
 */

public interface OnMeasureListenerImp {

    void onError(MeasureCode code);
    void startSearch();
    /**
     * 搜索到蓝牙设备
     * @param device
     * @return  如果返回某个蓝牙地址，则停止搜索并连接该蓝牙
     */
    String found(BluetoothDevice device);
    /**
     * 搜索完毕
     * @param devices
     * @return 返回@param devices的索引，将连接该蓝牙，不连接任何蓝牙则返回小于0的数
     */
    int foundFinish(List<BluetoothDevice> devices);
    void connecting(BluetoothDevice device);
    /** 连接成功 **/
    void onConnectSuccessful(BluetoothDevice device);
    /**
     * 连接失败
     */
    void connectFailed(BluetoothDevice device);
    /**
     * 蓝牙连接断开
     */
    void onDisconnected(BluetoothDevice device);

}

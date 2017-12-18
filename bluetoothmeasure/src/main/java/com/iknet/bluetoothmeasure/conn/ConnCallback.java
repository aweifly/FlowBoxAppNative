package com.iknet.bluetoothmeasure.conn;

import android.bluetooth.BluetoothDevice;

/**
 * Created by Administrator on 2017/12/5.
 */

public interface ConnCallback {

    void connected(BluetoothDevice device);
    /**
     * 连接失败
     * @param device
     */
    void connectFailed(BluetoothDevice device);
    /**
     * 断开蓝牙连接
     */
    void disconnetBt(boolean disconnet);
    /**
     * 远端蓝牙发送过来的数据
     * @param data
     */
    void read(byte[] data);

}

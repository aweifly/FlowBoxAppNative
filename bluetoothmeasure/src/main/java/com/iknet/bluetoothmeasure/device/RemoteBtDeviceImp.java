package com.iknet.bluetoothmeasure.device;


import com.iknet.bluetoothmeasure.OnMeasureListenerImp;

/**
 * 远程蓝牙设备接口， 负责数据解析和相关结果反馈
 * Created by Administrator on 2017/11/15 0015.
 */

public interface RemoteBtDeviceImp {

    /**
     * 解析远端蓝牙返回的数据
     * @param data
     */
    void parseData(byte[] data);
    /**
     * 断开了蓝牙连接，做相关操作，比如释放资源
     */
    void disconnectBt();
    OnMeasureListenerImp getListener();
    void addListener(OnMeasureListenerImp measureListener);

}

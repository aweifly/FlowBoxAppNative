package com.iknet.bluetoothmeasure.device;


import com.iknet.bluetoothmeasure.OnMeasureListenerImp;

/**
 * 体重仪测量监听器
 * Created by Administrator on 2017/11/15 0015.
 */

public interface WeightDeviceListener extends OnMeasureListenerImp {

    /**
     * 交付解析后的蓝牙数据
     * @param weight
     * @param height
     * @param pmi
     */
    void deliver(String weight, String height, String pmi);

}

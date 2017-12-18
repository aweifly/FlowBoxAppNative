package com.iknet.bluetoothmeasure.device;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.util.SparseArray;


import com.iknet.bluetoothmeasure.IHandler;
import com.iknet.bluetoothmeasure.OnMeasureListenerImp;
import com.iknet.bluetoothmeasure.util.FrameUtil;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * 体重身高仪
 * Created by Administrator on 2017/11/15 0015.
 */

public class WeightBtDevice implements RemoteBtDeviceImp {
    private static final String TAG = WeightBtDevice.class.getSimpleName();

    private static final int START_CODE = 0;  //起始标志
    private static final int DATA_LENGHT_CODE = 1;  //数据长度
    private static final int DATA_CODE = 2;  //数据
    private static final int CS_CODE = 3;  //校验码
    private static final int END_CODE = 4;  //结束标志

    private static final byte START_CODE1 = 0x28;
    private static final byte START_CODE2 = 0x2A;
    private static final byte END_CODE1 = 0x2A;
    private static final byte END_CODE2 = 0x29;

    private OnMeasureListenerImp measureListener;

    private SparseArray<IHandler> handlerMap = new SparseArray<>();
    private List<Byte> dataBytes = Collections.synchronizedList(new LinkedList<Byte>());	//要解析的数据
    private int status = START_CODE;
    private IHandler handler;
    private int dataLenght = 0;  //该长度包括数据长度和校验码
    private ParseThread parseThread;

    private Context context;

    public WeightBtDevice(Context context){
        this.context = context;
        initStartCode();
        initDataLenCode();
        initDataCode();
        initCsCode();
        initEndCode();

        parseThread = new ParseThread();
        parseThread.start();
    }

    @Override
    public void parseData(byte[] data){
        if(data == null || data.length == 0) return;

        for(int i=0; i<data.length; i++){
            dataBytes.add(Byte.valueOf(data[i]));
        }
    }

    @Override
    public void disconnectBt(){
        if(parseThread != null){
            parseThread.stop();
        }
    }

    private class ParseThread implements Runnable {

        Thread thread;
        boolean run = true;

        public ParseThread(){
            thread = new Thread(this, ParseThread.class.getSimpleName());

        }

        @Override
        public void run(){
            while(run){
                handler = handlerMap.get(status);
                if (handler != null)
                    handler.handler();
            }
        }

        public void start(){
            thread.start();
        }

        public void stop(){
            run = false;
        }
    }

    private void initStartCode(){
        handlerMap.append(START_CODE, new IHandler() {
            @Override
            public void handler() {
                try {
//					Log.v(TAG, "PRE_CODE1");
                    if (dataBytes.size() >= 2) {
                        byte[] buff = new byte[2];
                        for(int i=0; i<buff.length; i++){
                            buff[i] = dataBytes.get(0).byteValue();
                            dataBytes.remove(0);
                        }

                        if (START_CODE1 == buff[0] && START_CODE2 == buff[1]) {
                            status = DATA_LENGHT_CODE;
                        }
                    }
                } catch (Exception e) {
                    status = START_CODE;
                    e.printStackTrace();
                }
            }
        });
    }

    private void initDataLenCode(){
        handlerMap.append(DATA_LENGHT_CODE, new IHandler() {

            @Override
            public void handler() {
                try {
                    if (dataBytes.size() >= 4) {
                        byte[] buff = new byte[4];
                        for(int i=0; i<buff.length; i++){
                            buff[i] = dataBytes.get(0).byteValue();
                            dataBytes.remove(0);
                        }
                        dataLenght = FrameUtil.ascii2Int(buff);
                        Log.v(TAG, "数据长度：" + dataLenght);
//                        Toast.makeText(BaseApplication.getInstance().getApplicationContext(),
//                                "数据长度：" + dataLenght, Toast.LENGTH_SHORT).show();
                        status = DATA_CODE;
                    }else{
                        status = START_CODE;
                    }
                } catch (Exception e) {
                    status = START_CODE;
                    e.printStackTrace();
                }
            }
        });
    }

    private void initDataCode(){
        handlerMap.append(DATA_CODE, new IHandler() {

            @Override
            public void handler() {
                try {
                    if (dataBytes.size() >= dataLenght-2) {
                        byte[] buff = new byte[dataLenght-2];
                        for(int i=0; i<buff.length; i++){
                            buff[i] = dataBytes.get(0).byteValue();
                            dataBytes.remove(0);
                        }
                        String r = FrameUtil.ascii2String(buff);
                        final String[] result = r.split(",");
                        if(result.length > 2){
                            Log.i(TAG, "测量结果：" + result[0] + " " + result[1] + " " + result[2]);
                            ((Activity) context).runOnUiThread(new Runnable() {
                                @Override
                                public void run(){
                                    ((WeightDeviceListener) getListener()).deliver(result[0], result[1], result[2]);
                                }
                            });
                            status = CS_CODE;
                        }else {
                            status = START_CODE;
                        }
                    }else {
                        status = DATA_CODE;
                    }

                } catch (Exception e) {
                    status = START_CODE;
                    e.printStackTrace();
                }
            }
        });
    }

    private void initCsCode(){
        handlerMap.append(CS_CODE, new IHandler() {

            @Override
            public void handler() {
                try {
                    if (dataBytes.size() >= 2) {
                        byte[] buff = new byte[2];
                        for(int i=0; i<buff.length; i++){
                            buff[i] = dataBytes.get(0).byteValue();
                            dataBytes.remove(0);
                        }
                        status = END_CODE;
                    }else {
                        status = CS_CODE;
                    }
                } catch (Exception e) {
                    status = START_CODE;
                    e.printStackTrace();
                }
            }
        });
    }

    private void initEndCode(){
        handlerMap.append(END_CODE, new IHandler() {
            @Override
            public void handler() {
                try {
//					Log.v(TAG, "PRE_CODE1");
                    if (dataBytes.size() >= 2) {
                        byte[] buff = new byte[2];
                        for(int i=0; i<buff.length; i++){
                            buff[i] = dataBytes.get(0).byteValue();
                            dataBytes.remove(0);
                        }

//                        if (END_CODE1 == buff[0] && END_CODE2 == buff[1]) {
//                            status = DATA_LENGHT_;
//                        }
                        status = START_CODE;
                    }else {
                        status = END_CODE;
                    }
                } catch (Exception e) {
                    status = START_CODE;
                    e.printStackTrace();
                }finally{
                }
            }
        });
    }

    @Override
    public void addListener(OnMeasureListenerImp measureListener){
        this.measureListener = measureListener;
    }

    @Override
    public OnMeasureListenerImp getListener(){
        return measureListener;
    }



}

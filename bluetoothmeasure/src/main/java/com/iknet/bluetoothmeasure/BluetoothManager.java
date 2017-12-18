package com.iknet.bluetoothmeasure;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.RequiresApi;
import android.text.TextUtils;
import android.util.Log;

import com.iknet.bluetoothmeasure.device.RemoteBtDeviceImp;
import com.iknet.bluetoothmeasure.util.FrameUtil;
import com.iknet.bluetoothmeasure.util.PermissionUtil;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static com.iknet.bluetoothmeasure.MeasureCode.ERROR_CODE_BT_NOT_ENABLE;


/**
 *
 * Created by Administrator on 2017/11/15 0015.
 */
@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
public class BluetoothManager {
    private static final String TAG = BluetoothManager.class.getSimpleName();

    private BluetoothAdapter _bluetooth = BluetoothAdapter.getDefaultAdapter();
    private BluetoothManager instance;
    private Context context;
    private RemoteBtDeviceImp remoteBtDevice;
//    private OnMeasureListenerImp measureListener;

    private List<BluetoothDevice> deviceList;  //搜索到的蓝牙设备

    private MyReceiver mReceiver;

    private BluetoothService mService;
    private boolean mIsRunning;
    private boolean isBinded;

//    public static BluetoothManager getInstance(Context context){
//        if(instance == null){
//            instance = new BluetoothManager(context);
//        }
//
//        return instance;
//    }

    public BluetoothManager(Context context){
        this.context = context;
        init();
    }

    private void init(){
        deviceList = new ArrayList<>();

        mReceiver = new MyReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        context.registerReceiver(mReceiver, filter);
    }

    /**
     * 开启蓝牙事务
     */

    public void startBTAffair(RemoteBtDeviceImp remoteBtDeviceImp){
//        this.measureListener = measureListener;
        this.remoteBtDevice = remoteBtDeviceImp;
        if(!_bluetooth.isEnabled()){
            remoteBtDeviceImp.getListener().onError(ERROR_CODE_BT_NOT_ENABLE);
            return;
        }
        Log.v(TAG, "启动蓝牙事务");

        if(FrameUtil.isServiceRunning("com.iknet.pzhdoctor.widget.bluetooth.BluetoothService", context) &&
                !TextUtils.isEmpty(BluetoothService.getConnectedBTAddress())){
            remoteBtDeviceImp.getListener().onConnectSuccessful(
                    _bluetooth.getRemoteDevice(BluetoothService.getConnectedBTAddress()));
//            startMeasure();
        } else {
            startBtService();
            bindBtService();

            setBtDiscoverable();
            searchBluetooth();
        }
    }

    public void stopBTAffair(){
        if(mService != null){
            mService.disconnectToBT();
        }
        unbindBtService();
        stopBtService();
        context.unregisterReceiver(mReceiver);
    }

    /**
     * 通过反射调用setScanMode，设置蓝牙可见性
     */
    private void setBtDiscoverable(){
        try {
            Class<?> ba = _bluetooth.getClass();
            Method m = ba.getMethod("setScanMode", new Class<?>[]{int.class});
            boolean b = (Boolean) m.invoke(_bluetooth, BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE);
            Log.v(TAG, "setBtDiscoverable-设置蓝牙可见性：" + b);
        } catch (Exception e) {
            e.printStackTrace();
            Log.v(TAG, "setBtDiscoverable-设置蓝牙可见性错误");
        }
    }

    /**
     * 搜索蓝牙设备
     */
    public void searchBluetooth() {
        if(Build.VERSION.SDK_INT >= 23){
            requestLocationPerm();
        }else{
            doDiscovery();
        }
    }

    public static final int REQUEST_COARSE_LOCATION = PermissionUtil.REQUEST_COARSE_LOCATION;
    /** 23以上版本蓝牙扫描需要定位权限 */
    private void requestLocationPerm() {
        if (!PermissionUtil.checkLocationPermission(context)) {
            PermissionUtil.requestLocationPerm(context);
        } else {
            doDiscovery();
        }
    }

    /**
     * 搜索设备
     */
    private void doDiscovery() {
//        onDiscoveryFinishedCount = 0;
//        deviceList.clear();
        if(!_bluetooth.isEnabled()){
            remoteBtDevice.getListener().onError(ERROR_CODE_BT_NOT_ENABLE);
            return;
        }
        if (_bluetooth.isDiscovering()) {
            _bluetooth.cancelDiscovery();
        }
        _bluetooth.startDiscovery();
        Log.v(TAG, "开始搜索");
        remoteBtDevice.getListener().startSearch();
    }

    /** 连接蓝牙设备 */
    public void connectToBT(String addr){
        if(!BluetoothAdapter.checkBluetoothAddress(addr)){
            Log.v(TAG, "蓝牙地址非法，连接失败");
            return;
        }
        mService.connectToBT(addr);
//        Log.v(TAG, "开始连接蓝牙：" + addr);
//        if(FrameUtil.isServiceRunning("com.iknet.pzhdoctor.widget.bluetooth.BluetoothService", context)){
//            Intent intent = new Intent(BluetoothService.ACTION_BT_CONNECT_TO);
//            intent.putExtra("addr", addr);
//            context.sendBroadcast(intent);
//        }else{
//            //启动Service，连接蓝牙设备
//            Intent intent2 = new Intent(context.getApplicationContext(),BluetoothService.class);
//            intent2.putExtra("PREFS_BLUETOOTH_PRE_ADDR_STRING", addr);
//            context.startService(intent2);
//        }

    }

    private void startBtService(){
        if (!mIsRunning) {
            Log.v(TAG, "[SERVICE] Start");
            mIsRunning = true;
            Intent intent = new Intent(context, BluetoothService.class);
//            intent.putExtra("appUser", BaseApplication.getAppUser());
            context.startService(intent);
        }
    }

    private void bindBtService(){
        Log.v(TAG, "[SERVICE] Bind");
        context.bindService(new Intent(context, BluetoothService.class), mConnection,
                Context.BIND_AUTO_CREATE + Context.BIND_DEBUG_UNBIND);
    }

    private void unbindBtService(){
        if(isBinded){
            Log.v(TAG, "[SERVICE] Unbind");
            context.unbindService(mConnection);
        }
    }

    private void stopBtService(){
        if (mService != null) {
            Log.v(TAG, "[SERVICE] stopService");
            context.stopService(new Intent(context, BluetoothService.class));
        }
        mIsRunning = false;
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            mService = ((BluetoothService.BtBinder) service).getService();
            mService.setRemoteDevice(remoteBtDevice);
//            mService.registerCallback(mCallback);
            isBinded = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            mService = null;
            isBinded = false;
        }
    };

    // 监听蓝牙的连接状态
    private class MyReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent){
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String str = device.getName() + "-" + device.getAddress();
                Log.v(TAG, "搜索到的设备：" + str);
//                String arg = str.substring(0, 3);
                if (check(device.getAddress())) {
//                    if (arg.equals("RBP") || arg.contains("BP")) {
                        String addr = remoteBtDevice.getListener().found(device);
                        if(!TextUtils.isEmpty(addr) && BluetoothAdapter.checkBluetoothAddress(addr)){
                            deviceList.add(0, device);
                            if (_bluetooth.isDiscovering()) {
                                //取消搜索后系统会发送BluetoothAdapter.ACTION_DISCOVERY_FINISHED广播
                                _bluetooth.cancelDiscovery();
                            }
                        }else{
                            deviceList.add(device);
                        }
//                    }
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
//                ++onDiscoveryFinishedCount;
//                if(onDiscoveryFinishedCount == 1){
                    Log.v(TAG, "搜索完成-搜索到设备数量：" + deviceList.size());
                    int index = remoteBtDevice.getListener().foundFinish(deviceList);
                    if(index >= 0 && deviceList.size() > 0 && index < deviceList.size()){
                        connectToBT(deviceList.get(index).getAddress());
                    }
//                }
            } else if (action.equals(BluetoothDevice.ACTION_ACL_DISCONNECTED)) {
                //断开连接
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                remoteBtDevice.getListener().onDisconnected(device);
                deviceList.clear();
                Log.i(TAG, "蓝牙断开连接：" + device.getAddress());
            }
        }
    }

    // 检查列表中的蓝牙地址中是否存在该地址，避免重复添加
    private boolean check(String address) {
        int count = deviceList.size();
        for (int i = 0; i < count; i++) {
            if (deviceList.get(i).getAddress().equals(address))
                return false;
        }
        return true;
    }

}

package com.dev.mevur.bluetooth;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;

import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements ListView.OnItemClickListener{
//    public final static String EXTRA_MESSAGE="com.dev.mevur.bluetooth.MESSAGE";
    public static final UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    public Button btn1 = null;
    public Button btn2 = null;
    public Button btn3 = null;
    public TextView tv1 = null;
    public ListView lv1 = null;
    BluetoothAdapter mBluetoothAdapter  = BluetoothAdapter.getDefaultAdapter();
    public ArrayList<String> data = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btn1    = findViewById(R.id.Button1);
        btn2    = findViewById(R.id.Button2);
        btn3    = findViewById(R.id.Button3);
        tv1     = findViewById(R.id.TextView1);
        lv1     = findViewById(R.id.ListView1);
    }

    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 1:
                    tv1.append(msg.obj.toString()+ " has connected!\n");
                    break;
                case 2:
                    tv1.append(msg.obj.toString()+ "\n");
                    break;
            }
        }
    };

    public void btn1Click(View view) {
        tv1.append("设置蓝牙设备显示!\n");
        //创建一个Intent对象，设置蓝牙设备为可见状态
        Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        //指定可见状态时间, max 300s
        intent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION,300);
        startActivity(intent);

    }
//
//    //server function
//    public void btn3Click(View view) {
//        tv1.append("Start server!\n");
//        SerThread sth = new SerThread();
//        sth.run();
//    }
//
//
//    private boolean acceptFlag = false;
//    private class SerThread extends Thread{
//        private BluetoothServerSocket mBTSS;
//        public SerThread(){
//            try {
//                mBTSS = mBluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord("BTser",uuid);
//
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//        @Override
//        public void run(){
//            BluetoothSocket bs = null;
//            Log.i("ser","waiting");
//                try {
//                    bs = mBTSS.accept();
//                    while(true){
//                    if(null != bs){
//                        Log.i("ser","connect" + bs.getRemoteDevice().getName() + "\n");
//                        acceptFlag = true;
//                        Message msg = new Message();
//                          msg.what = 1;
//                        msg.obj = bs.getRemoteDevice().getName();
//                        handler.sendMessage(msg);
//                    }
//                    else{
//                        Log.i("ser","none!\n");
//                    }
//                    }
//                }
//                catch (IOException e){
//                    e.printStackTrace();
//                }
//
//            new Thread(new ReceivingThread(bs)).start();
//
//        }
//        public void cancel(){
//            try {
//                mBTSS.close();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//    }
private class ReceivingThread implements Runnable{
        BluetoothSocket bs;
        public ReceivingThread(BluetoothSocket bs){
            super();
            this.bs = bs;
        }
    @Override
    public void run() {
            try{
                while(true){
                    InputStream input = bs.getInputStream();
                    byte buf[] = new byte[1024];
                    int n = input.read(buf);
                    String str = new String(buf,0,n);
                    Message msg = new Message();
                    msg.obj = bs.getRemoteDevice().getName() + ":" + str;
                    msg.what = 2;
                    handler.sendMessage(msg);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

    }
}

    public void btn2Click(View view) {
        tv1.append("开始扫描蓝牙设备……\n");
        //得到 Adpater对象
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        //判断是否为空，空则说明本机不支持Bluetooth
        if(mBluetoothAdapter != null){
            tv1.append("本机拥有蓝牙设备!\n");
            //检查本机蓝牙是否可用
            if(!mBluetoothAdapter.isEnabled()){
                //若不可用，则提示用户启动蓝牙适配器
                Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivity(intent);
            }
            //蓝牙可用
            //得到所有已经配对的蓝牙对象
            Set<BluetoothDevice> devices = mBluetoothAdapter.getBondedDevices();
            data = new ArrayList<String>();

            if(devices.size()>0){
                //显示已经配对的设备
                tv1.append("已经连接的蓝牙设备如下:\n");
                for(Iterator iterator = devices.iterator(); iterator.hasNext();){
                    BluetoothDevice device = (BluetoothDevice)iterator.next();
                    tv1.append("设备名:" + device.getName() + " 地址:" + device.getAddress() + "\n");
                    data.add(device.getAddress());
                }
                ArrayAdapter aa = new ArrayAdapter(MainActivity.this,android.R.layout.simple_list_item_1,data);
                lv1.setAdapter(aa);
                lv1.setOnItemClickListener(this);// ListView 监听器
            }

        }
        else{
            tv1.append("本机无蓝牙设备!\n");
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        String serMac = data.get(position);
        tv1.append("当前点击:" + serMac + "\n");
        Intent intent = new Intent(MainActivity.this,ControlActivity1.class);//跳转
        intent.putExtra("serMac",serMac);
        startActivity(intent);
    }
}

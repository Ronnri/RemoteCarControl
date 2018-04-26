package com.dev.mevur.bluetooth;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

/**
 * Created by Mevur on 03/13/18.
 */

public class ControlActivity1 extends Activity {
    Button  btnUp       = null;
    Button  btnDown     = null;
    Button  btnLeft     = null;
    Button  btnRight    = null;

    Button  btnSpeedUp      = null;
    Button  btnSpeedDown    = null;
    Button  btnSppedStop    = null;

    TextView tv1        =null;

    public BluetoothDevice serMacMsg = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_control1);
        btnUp           =   findViewById(R.id.btnUp);
        btnDown         =   findViewById(R.id.btnDown);
        btnLeft         =   findViewById(R.id.btnLeft);
        btnRight        =   findViewById(R.id.btnRight);

        btnSpeedUp      =   findViewById(R.id.btnSpeedUp);
        btnSpeedDown    =   findViewById(R.id.btnSpeedDown);
        btnSppedStop    =   findViewById(R.id.btnSpeedStop);

        tv1             =   findViewById(R.id.TextView1);


        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();
        String msg = bundle.getString("serMac");
        serMacMsg = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(msg);

        new Thread(new ControlActivity1.ConnThread(serMacMsg)).start();
    }

    private BluetoothSocket mBluetoothSocket;



    private class ConnThread extends Thread {
        private final BluetoothDevice mBluetoothDevice;
        public ConnThread(BluetoothDevice device) {
            mBluetoothDevice = device;
            //得到一个bluetooth线程
            try {
                mBluetoothSocket = mBluetoothDevice.createRfcommSocketToServiceRecord(MainActivity.uuid);
            } catch (IOException e) {
                e.printStackTrace();
                mBluetoothSocket = null;
            }
        }

        public void run() {
            Log.i("tag", "begin connThread");
            try {
                mBluetoothSocket.connect();//阻塞
                if (mBluetoothSocket.isConnected()) {
                    Log.i("conn", "succeed");
                } else {
                    Log.i("conn", "fail");
                }
            } catch (IOException e) {
                e.printStackTrace();
                try {
                    //连接失败，关闭线程
                    mBluetoothSocket.close();
                } catch (IOException e2) {
                    e2.printStackTrace();
                }
                return;
            }
            //启动传输线程
            Log.i("tag", "conn succeed!");
            new Thread(new ControlActivity1.ReceivingThread(mBluetoothSocket)).start();
        }

        //断开连接，点击某个按钮后断开蓝牙连接
        public void cancel() {
            try {
                mBluetoothSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    private class SendingThread implements Runnable {
        String sendmsg;
        BluetoothSocket bs;
        public SendingThread(BluetoothSocket bs, String sendmsg) {
            super();
            this.bs = bs;
            this.sendmsg = sendmsg;
            Log.i("sendthr", sendmsg);
        }

        @Override
        public void run() {
            if (sendmsg.length() < 1) {
                Log.i("sendthr2","length = 0  :"+sendmsg.length());
                return;
            }
            Log.i("sendthr3", sendmsg);
            try {
                if (!bs.isConnected()) {
                    bs.connect();
                    // new Thread(new ConnThread(serMacMsg)).start();
                }
                OutputStream output = bs.getOutputStream();
                output.write(sendmsg.getBytes());
                output.flush();
                Log.i("outputstring", sendmsg);
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    private class ReceivingThread implements Runnable {
        BluetoothSocket bs;

        public ReceivingThread(BluetoothSocket bs) {
            super();
            this.bs = bs;
        }

        @Override
        public void run() {
            try {
                if (!bs.isConnected())
                    bs.connect();
                InputStream inputStream = bs.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println("from server:" + line);
                    Message msg = Message.obtain();
                    msg.obj = bs.getRemoteDevice().getName() + ":" + line;
                    msg.what = 2;
                    handler.sendMessage(msg);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 1:
                    tv1.append(msg.obj.toString() + " has connected!\n");
                    break;
                case 2:
                    tv1.append(msg.obj.toString() + "\n");
                    break;
            }
        }
    };

    public void btnUpClick(View view) {
        tv1.append("前进\n");
        new Thread(new SendingThread(mBluetoothSocket,"w")).start();
    }

    public void btnSpeedStopClick(View view) {
        tv1.append("制动\n");
       // new Thread(new SendingThread(mBluetoothSocket,""));
    }

    public void btnLeftClick(View view) {
        tv1.append("左转\n");
        new Thread(new SendingThread(mBluetoothSocket,"a")).start();
    }

    public void btnDownClick(View view) {
        tv1.append("后退\n");
        new Thread(new SendingThread(mBluetoothSocket,"s")).start();
    }

    public void btnRightClick(View view) {
        tv1.append("右转\n");
        new Thread(new SendingThread(mBluetoothSocket,"d")).start();
    }

    public void btnSpeedUpClick(View view) {
        tv1.append("加速\n");
        new Thread(new SendingThread(mBluetoothSocket,"f")).start();
    }

    public void btnSpeedDownClick(View view) {
        tv1.append("减速\n");
        String str = "g";
        new Thread(new SendingThread(mBluetoothSocket,str)).start();
    }

    /**
     * 字符串转换成为16进制(无需Unicode编码)
     * @param str
     * @return
     */
    public static String str2HexStr(String str) {
        char[] chars = "0123456789ABCDEF".toCharArray();
        StringBuilder sb = new StringBuilder("");
        byte[] bs = str.getBytes();
        int bit;
        for (int i = 0; i < bs.length; i++) {
            bit = (bs[i] & 0x0f0) >> 4;
            sb.append(chars[bit]);
            bit = bs[i] & 0x0f;
            sb.append(chars[bit]);
            // sb.append(' ');
        }
        return sb.toString().trim();
    }
    /**
     * 16进制直接转换成为字符串(无需Unicode解码)
     * @param hexStr
     * @return
     */
    public static String hexStr2Str(String hexStr) {
        String str = "0123456789ABCDEF";
        char[] hexs = hexStr.toCharArray();
        byte[] bytes = new byte[hexStr.length() / 2];
        int n;
        for (int i = 0; i < bytes.length; i++) {
            n = str.indexOf(hexs[2 * i]) * 16;
            n += str.indexOf(hexs[2 * i + 1]);
            bytes[i] = (byte) (n & 0xff);
        }
        return new String(bytes);
    }
}

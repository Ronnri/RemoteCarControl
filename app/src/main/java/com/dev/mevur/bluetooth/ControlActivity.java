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
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.CharBuffer;
import java.nio.charset.Charset;

/**
 * Created by Mevur on 03/12/18.
 */

public class ControlActivity extends Activity {
    public TextView tv1 = null;
    public EditText et1 = null;
    public Button bt1 = null;
    public BluetoothDevice serMacMsg = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_control);
        tv1 = (TextView) findViewById(R.id.TextView1);
        et1 = (EditText) findViewById(R.id.EditText1);
        bt1 = (Button) findViewById(R.id.Button1);

        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();
        String msg = bundle.getString("serMac");
        serMacMsg = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(msg);

//        ConnThread cth = new ConnThread(serMacMsg);
//        cth.run();
        new Thread(new ConnThread(serMacMsg)).start();


    }

    public void btn1Click(View view) {
        String sendMsg = et1.getText().toString();
        Log.i("sendmsg", "this is sending thread,context:" + sendMsg);
        //  SendingThread sth = new SendingThread(mBluetoothSocket,sendMsg);
        //sth.run();
        new Thread(new SendingThread(mBluetoothSocket, sendMsg)).start();
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
            new Thread(new ReceivingThread(mBluetoothSocket)).start();
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
        BluetoothSocket bs;
        String sendmsg;

        public SendingThread(BluetoothSocket bs, String sendmsg) {
            super();
            this.bs = bs;
            this.sendmsg = sendmsg;
            Log.i("sendthr", sendmsg);
        }

        @Override
        public void run() {
            if (sendmsg.length() < 1) {
                return;
            }
            Log.i("sendthr2", sendmsg);
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


}

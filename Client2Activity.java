package com.example.zhouyong0701.servicethreadtest;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;

public class Client2Activity extends AppCompatActivity {
    private String mIPAddress;
    private MyThread thread;
    private String mAimId;
    private boolean mAimIpAvaliable = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client);

        Intent intent = getIntent();
        mIPAddress = intent.getStringExtra("ip");

        thread = new MyThread();
        thread.start();
    }

    private class MyThread extends Thread {
        public MyThread() {
            super();
        }

        @Override
        public void run() {
            try {
                Socket socket = new Socket(mIPAddress, 8905);
                InputStream in = socket.getInputStream();
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(in));
                OutputStream outputStream = socket.getOutputStream();
                int i = 1;
                Build build = new Build();
                String id = build.MODEL + "&&" + build.TIME;
                outputStream.write(("reg-server-"+id+"\n").getBytes());

                while (socket.isConnected()) {
                    if(!mAimIpAvaliable) {
                        String str = bufferedReader.readLine();
                        Log.d("zhouyongxyz","str = " +str);
                        if(str.startsWith("aim")) {
                            mAimId = str.split(":")[1];
                            mAimIpAvaliable = true;
                        }
                    } else {
                        String format = "format-byte-"+mAimId+"\n";
                        outputStream.write(format.getBytes());
                        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.test2);
                        ByteArrayOutputStream bout = new ByteArrayOutputStream();
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 20, bout);
                        bout.flush();
                        Log.d("zhouyongxyz", "send cap jpg ...bout.size() = " + bout.size());
                        int size = bout.size();
                        String header = String.format("start-%08d",size);
                        Log.d("zhouyongxyz","header = "+header+" size = "+header.getBytes().length);
                        outputStream.write(header.getBytes());
                        outputStream.write(bout.toByteArray());
                        outputStream.flush();
//                        String data = "dat-"+mAimIp+"-count>>"+i+"\n";
//                        Log.d("zhouyongxyz","send data = " +data);
//                        outputStream.write(data.getBytes());
//                        outputStream.flush();
//                        i++;
                    }
                    Thread.sleep(1000);
                }
            } catch (Exception e) {
                Log.d("zhouyong","client interrupt ..");
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onDestroy() {
        if(thread != null) {
            thread.interrupt();
        }
        super.onDestroy();
    }
}

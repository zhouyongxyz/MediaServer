package com.example.zhouyong0701.servicethreadtest;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class ClientActivity extends AppCompatActivity {
    private String mIPAddress;
    MyThread thread;
    private List<String> mList;
    private String mAimId;
    private String mLocalId;
    private boolean mAimIpAvaliable = false;
    private boolean mIsByteStream = false;

    private Bitmap mBitmap;

    private SurfaceView mSView;
    private SurfaceHolder mSHolder;
    private boolean mUpdateImage = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client);

        Intent intent = getIntent();
        mIPAddress = intent.getStringExtra("ip");
        thread = new MyThread();
        thread.start();

        mList = new ArrayList<>();
        mSView = (SurfaceView) findViewById(R.id.image);
        mSHolder = mSView.getHolder();
        mSHolder.addCallback(new UpdateImage());

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
                InputStream inputStream = socket.getInputStream();
                OutputStream outputStream = socket.getOutputStream();
                int i = 1;
                Build build = new Build();
                mLocalId = build.MODEL + "&&" + build.TIME;
                outputStream.write(("reg-client-"+mLocalId+"\n").getBytes());
                outputStream.write("lst-server\n".getBytes());

                while (true) {
                    if(!mIsByteStream) {
                        String str = bufferedReader.readLine();
                        Log.d("zhouyongxyz", "str = " + str);
                        if (str.startsWith("iplst")) {
                            String[] ids = str.split(":");
                            mAimId = ids[1];
                            mAimIpAvaliable = true;
                            //mLocalIp = ips[1];//socket.getLocalAddress().toString().substring(1);
                            Log.d("zhouyongxyz", "mLocalId = " + mLocalId);
                            String data = "dat-" + mAimId + "-aim:" + mLocalId + "\n";
                            outputStream.write(data.getBytes());
                        } else if (str.startsWith("format")) {
                            String format = str.split("-")[1];
                            if("byte".equals(format)) {
                                mIsByteStream = true;
                            }
                        }
                    } else {
                        byte[] header = new byte[13];
                        byte[] start = new byte[1];
                        int len;
                        int imagesize = 0;
                        while((len = inputStream.read(start,0,start.length)) > 0) {
                            if(start[0] == 115) {
                                len = inputStream.read(header,0,header.length);
                                String str = new String(header);
                                if (str.startsWith("tart")) {
                                    imagesize = Integer.parseInt(str.split("-")[1]);
                                    Log.d("zhouyong", "server header str = " + str + " size = " + imagesize + "header[0] = " + header[0]);

                                    byte[] data = new byte[1024];
                                    ByteArrayOutputStream imageData = new ByteArrayOutputStream();
                                    int n = imagesize / 1024;
                                    len = 0;
                                    int imglen;
                                    while((imglen = inputStream.read(data,0,data.length)) > 0) {
                                        len += imglen;
                                        //Log.d("zhouyong","imglen = "+imglen + "len = "+len);
                                        imageData.write(data,0,imglen);
                                        if(len == imagesize) {
                                            break;
                                        }
                                    }
                                    imageData.flush();
                                    Log.d("zhouyong", "server read successs size = "+len +" data size = "+imageData.size());
                                    if(len == imagesize ){
                                        mBitmap = BitmapFactory.decodeByteArray(imageData.toByteArray(),0,imageData.size());
                                        mUpdateImage = true;
                                        imageData.close();
                                        break;
                                    }
                                }
                            }
                        }
                    }
                    //Thread.sleep(500);
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

    private class UpdateImage implements SurfaceHolder.Callback {
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    while(true) {
                        if(mUpdateImage) {
                            Log.d("zhouyong","update surface ...");
                            mUpdateImage = false;
                            Canvas canvas = mSHolder.lockCanvas();
                            Paint paint = new Paint();
                            paint.setColor(Color.WHITE);
                            canvas.drawBitmap(mBitmap,0,0,paint);
                            mSHolder.unlockCanvasAndPost(canvas);
                            mBitmap.recycle();
                        }
                    }
                }
            }).start();
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {

        }
    }
}

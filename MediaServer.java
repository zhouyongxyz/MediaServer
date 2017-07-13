package com.example;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by zhou on 17-7-8.
 */

public class MediaServer {
    private ServerThread mServerThread;
    private ServerSocket mServer;

    private Map<String,Socket> mServerMap;
    private List<String> mServerList;
    private Map<String,Socket> mClientMap;
    private List<String> mClientList;

    public void init() {
        mServerMap = new HashMap<>();
        mClientMap = new HashMap<>();
        mServerList = new ArrayList<>();
        mClientList = new ArrayList<>();

        mServerThread = new ServerThread();
        mServerThread.start();

    }


    private class ServerThread extends Thread {

        public ServerThread() {
            super();
        }

        @Override
        public void run() {
            try {
                System.out.print("Server is waiting ...\n");
                mServer = new ServerSocket(8905);
                while(true) {
                    Socket socket = mServer.accept();
                    String ip = socket.getInetAddress().toString().substring(1);
                    System.out.print("client address = " + ip + "\n");
                    //mServerMap.put(ip, socket); do not use ip map,use client id-info instead
                    //mServerList.add(socket.getInetAddress().toString().substring(1));

                    ServerSocketThread st = new ServerSocketThread(ip,socket);
                    st.start();
                }
            } catch (Exception e) {
                //exception
                e.printStackTrace();
                System.out.print("exit ...");
                try {
                    mServer.close();
                } catch (Exception e1) {
                    //
                }
            }
        }

        private class ServerSocketThread extends Thread {
            private Socket mSocket;
            private String mIp;
            private String mClientId;
            private boolean mIsByteStream = false;
            private String mAimId;

            public ServerSocketThread(String ip,Socket socket) {
                mSocket = socket;
                mIp = ip;
            }

            @Override
            public void run() {
                try {
                    BufferedReader br = new BufferedReader(new InputStreamReader(mSocket.getInputStream()));
                    InputStream input = mSocket.getInputStream();
                    while(mSocket.isConnected()) {
                        if(!mIsByteStream) {
                            String str = br.readLine();
                            System.out.println("str = " + str);
                            if (str.startsWith("reg")) {
                                if (str.split("-")[1].equals("server")) {
                                    String id = str.split("-")[2];
                                    mClientId = id;
                                    mServerMap.put(id,mSocket);
                                    mServerList.add(id);
                                    System.out.println("server id = " + id);
                                } else if (str.split("-")[1].equals("client")){
                                    String id = str.split("-")[2];
                                    mClientId = id;
                                    mServerMap.put(id,mSocket);
                                    System.out.println("client id = " + id);
                                }
                            } else if (str.startsWith("dat")) {
                                String ip = str.split("-")[1];
                                String data = str.split("-")[2] + "\n";
                                Socket socket = mServerMap.get(ip);
                                OutputStream out = socket.getOutputStream();
                                out.write(data.getBytes());
                                out.flush();
                            } else if (str.startsWith("lst")) {
                                String data = "iplst:" + mServerList.get(0);
                                for (int i = 1; i < mServerList.size(); i++) {
                                    data += ":";
                                    data += mServerList.get(i);
                                }
                                data += "\n";
                                OutputStream out = mSocket.getOutputStream();
                                System.out.println("lst = " + data);
                                out.write(data.getBytes());
                                out.flush();
                            } else if (str.startsWith("format")){
                                String format = str.split("-")[1];
                                String id = str.split("-")[2];
                                if("byte".equals(format)) {
                                    mIsByteStream = true;
                                    mAimId = id;
                                }
                                String data = "format-byte\n";
                                Socket socket = mServerMap.get(id);
                                OutputStream out = socket.getOutputStream();
                                out.write(data.getBytes());
                                out.flush();
                            }
                        } else {
                            //tansform byte data
                            int len;
                            byte[] data = new byte[1024];
                            len = input.read(data,0,data.length);
                            System.out.println("len = " + len);
                            Socket socket = mServerMap.get(mAimId);
                            OutputStream out = socket.getOutputStream();
                            out.write(data,0,len);
                            out.flush();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

}

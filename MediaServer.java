package com.example;

import java.io.BufferedReader;
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
    private ClientThread mClientThread;
    private ServerSocket mServer;
    private ServerSocket mClient;

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
                while(true) {
                    mServer = new ServerSocket(8905);
                    Socket socket = mServer.accept();
                    System.out.print("server address = " + socket.getInetAddress().toString().substring(1) + "\n");
                    mServerMap.put(socket.getInetAddress().toString().substring(1), socket);
                    mServerList.add(socket.getInetAddress().toString().substring(1));

                    ServerSocketThread st = new ServerSocketThread(socket);
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
            public ServerSocketThread(Socket socket) {
                mSocket = socket;
            }

            @Override
            public void run() {
                super.run();
            }
        }
    }

    private class ClientThread extends Thread {
        public ClientThread() {
            super();
        }

        @Override
        public void run() {
            try {
                System.out.print("ClientServer is waiting ...\n");
                mClient = new ServerSocket(8906);
                while(true) {
                    Socket socket = mClient.accept();
                    System.out.print("client address = " + socket.getInetAddress().toString().substring(1) + "\n");
                    mClientMap.put(socket.getInetAddress().toString().substring(1), socket);
                    mClientList.add(socket.getInetAddress().toString().substring(1));
                    ClientSocketThread st = new ClientSocketThread(socket);
                    st.start();
                }
            } catch (Exception e) {
                //exception
                e.printStackTrace();
            }
        }


        private class ClientSocketThread extends Thread {
            private Socket mSocket;

            public ClientSocketThread(Socket socket) {
                mSocket = socket;
            }

            @Override
            public void run() {
                try {
                    BufferedReader br = new BufferedReader(new InputStreamReader(mSocket.getInputStream()));
                    OutputStream out = mSocket.getOutputStream();
                    while(mSocket.isConnected()) {
                        String str = br.readLine();
                        if("serverlist".equals(str)) {
                            String outstr = mServerList.get(0);
                            for(int i=1;i<mServerList.size();i++) {
                                outstr += ":";
                                outstr += mServerList.get(i);
                            }
                            out.write(outstr.getBytes());
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

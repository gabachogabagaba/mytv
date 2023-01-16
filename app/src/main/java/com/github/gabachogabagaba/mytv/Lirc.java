package com.github.gabachogabagaba.mytv;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;

public class Lirc extends Thread {
    String address;
    int port;
    public Handler handler;
    Socket socket = null;
    OutputStream outputStream;
    PrintWriter printWriter;

    static final int MSG_CONNECT = 0;
    static final int MSG_SEND = 1;
    static final int MSG_DISCONNECT = 2;
    static final int MSG_STOP = 3;
    static final String TAG = "Lirc";
    Lirc(String address, int port) {
        this.address = address;
        this.port = port;
    }

    public void run() {
        Looper.prepare();

        handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {

                    if (msg.what == MSG_CONNECT) {
                        try {
                            socket = new Socket(address, port);
                            outputStream = socket.getOutputStream();
                            printWriter = new PrintWriter(outputStream, true);
                        }
                        catch(SocketException e) {
                            socket = null;
                        }
                        catch(Exception e) {
                            socket = null;
                        }
                    }

                else if (msg.what == MSG_SEND) {
                    String remote;
                    String button;
                    String cmd;
                    remote = ((LircData)msg.obj).remote;
                    button = ((LircData)msg.obj).button;
                    cmd = String.format("SEND_ONCE %s %s", remote, button);
                    Log.i(TAG, cmd);
                    printWriter.println(cmd);
                }
                else if (msg.what == MSG_DISCONNECT) {
                    try {
                        socket.close();
                    }
                    catch(Exception e) {
                    }
                }
                else if (msg.what == MSG_STOP) {
                    Looper.myLooper().quit();
                }
            }
        };
        Looper.loop();
    }
    public void connect(){
        handler.sendEmptyMessage(MSG_CONNECT);
    }
    public void send(LircData lircData){
        Message msg = Message.obtain();
        msg.what = MSG_SEND;
        msg.obj = (Object)lircData;
        handler.sendMessage(msg);
    }

    public void disconnect(){
        handler.sendEmptyMessage(MSG_DISCONNECT);
    }

    public void quit() {
        handler.sendEmptyMessage(MSG_STOP);
    }
}

class LircData {
    String remote = "";
    String button = "";
}

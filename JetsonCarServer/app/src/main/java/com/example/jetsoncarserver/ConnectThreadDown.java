package com.example.jetsoncarserver;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

/**
 * Created by yuanx on 2016/7/13.
 * 小车倒车数据发送线程
 */

public class ConnectThreadDown extends Thread {
    private BluetoothSocket socket;
    private final BluetoothDevice mmDevice;
    BluetoothAdapter mBluetoothAdapter =null;
    Handler handler = null;

    public ConnectThreadDown(BluetoothDevice device,Handler handler, BluetoothAdapter mBluetoothAdapter, BluetoothSocket socket) {
        mmDevice = device;
        BluetoothSocket tmp = null;
        this.handler = handler;
        this.mBluetoothAdapter = mBluetoothAdapter;
        this.socket =socket;
    }

    public void run() {
        mkmsg("Client running\n");
        // Always cancel discovery because it will slow down a connection
        mBluetoothAdapter.cancelDiscovery();
        if (socket != null) {
            mkmsg("Connection made\n");
            mkmsg("Remote device address: "+socket.getRemoteDevice().getAddress()+"\n");
            //Note this is copied from the TCPdemo code.
            try {
                PrintWriter out = new PrintWriter( new BufferedWriter( new OutputStreamWriter(socket.getOutputStream())),true);
                mkmsg("Attempting to send message ...\n");
                out.print('B');
                out.flush();
                mkmsg("Message sent...\n");
            } catch(Exception e) {
                mkmsg("Error happened sending/receiving\n");
            }
        } else {
            mkmsg("Made connection, but socket is null\n");
        }
        mkmsg("Client ending \n");

    }
    public void mkmsg(String str) {
        //handler junk, because thread can't update screen!
        Message msg = new Message();
        Bundle b = new Bundle();
        b.putString("msg", str);
        msg.setData(b);
        handler.sendMessage(msg);
    }
}
package com.example.jetsoncarserver;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.io.IOException;
import java.util.Set;
import java.util.UUID;

import android.support.v7.app.AppCompatActivity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ImageView;
import android.widget.TextView;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    Bitmap bmp;
    TextView et;
    Runnable runnable = new Runnable() {
        @Override
        public void run() {
            startListen();
        }
    };

    //HC05蓝牙小车使用的UUID，以确保可以和小车正确匹配链接
    public static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private ImageButton upButton, leftButton, rightButton, downButton;
    private ImageButton stopButton;
    BluetoothAdapter mBluetoothAdapter = null;
    BluetoothDevice device;
    String TAG = "client";
    TextView stateView;
    int state = 0;
    private BluetoothSocket socket = null;

    private String intToIp(int i) {

        return (i & 0xFF ) + "." +
                ((i >> 8 ) & 0xFF) + "." +
                ((i >> 16 ) & 0xFF) + "." +
                ( i >> 24 & 0xFF) ;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //获取wifi服务
        WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        //判断wifi是否开启
        if (!wifiManager.isWifiEnabled()) {
            wifiManager.setWifiEnabled(true);
        }
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        int ipAddress = wifiInfo.getIpAddress();
        String ip = intToIp(ipAddress);
        et = (TextView)findViewById(R.id.textView2);
//        et.setText("手机的IP为:" + ip);

        upButton = (ImageButton) findViewById(R.id.upButton);
        leftButton = (ImageButton) findViewById(R.id.LeftUpButton);
        rightButton = (ImageButton) findViewById(R.id.RightUpButton);
        downButton = (ImageButton) findViewById(R.id.downButton);
        stopButton = (ImageButton) findViewById(R.id.stopButton);
        stateView = (TextView) findViewById(R.id.textView1);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        new Thread(runnable).start();
    }

    private void startListen() {
        try {
            final ServerSocket server = new ServerSocket(9090);
            Thread th = new Thread(new Runnable() {
                public void run() {
                    while (true) {
                        try {
                            Socket socket = server.accept();
                            receiveFile(socket);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
            th.run(); // 启动线程运行
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Handler myHandler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            super.handleMessage(msg);
            ImageView view = (ImageView) findViewById(R.id.imageView1);
            view.setImageBitmap(bmp);
        };
    };

    public void receiveFile(Socket socket) {
        int length = 0;
        DataInputStream dis = null;
        try {
            try {
                List<Byte> list = new ArrayList<Byte>();
                dis = new DataInputStream(socket.getInputStream());
                byte[] inputByte = new byte[1024];
                System.out.println("开始接收数据...");
                while ((length = dis.read(inputByte, 0, inputByte.length)) > 0) {
                    // 重要：把length作为参数传进去，
                    // 由于网络原因，DataInputStream.read并不是每次都返回byteCount个字节，有可能比byteCount小，
                    // 那么inputByte[]后面的字节填充的是无效数据，要把它忽略掉.否则。图片拿过来有马赛克
//                    appendByteArrayIntoByteList(list, inputByte, length);
                    for (int ii = 0; ii < length; ++ii) {
                        list.add(inputByte[ii]);
                    }
                }

                Byte[] IMG = (Byte[]) list.toArray(new Byte[0]);

                byte[] img = new byte[IMG.length];
                for (int i = 0; i < IMG.length; i++) {
                    img[i] = IMG[i];
                }

                ByteArrayOutputStream outPut = new ByteArrayOutputStream();
                bmp = BitmapFactory.decodeByteArray(img, 0, img.length);
                bmp.compress(CompressFormat.PNG, 100, outPut);

                myHandler.obtainMessage().sendToTarget();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (dis != null)
                    dis.close();
                if (socket != null)
                    socket.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void onClickUp(View view) {
        startUp();
    }

    public void onClickDown(View view) {
        startDown();
    }

    public void onClickLeft(View view) {
        startLeft();
    }

    public void onClickRight(View view) {
        startRight();
    }

    public void onClickStop(View view) {
        startStop();
    }

    private Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
//            output.append(msg.getData().getString("msg"));
            return true;
        }
    });

    //寻找附近可配对的所有蓝牙设备
    public void querypaired() {
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        // If there are paired devices
        if (pairedDevices.size() > 0) {
            // Loop through paired devices
//            output.append("at least 1 paired device\n");
            final BluetoothDevice blueDev[] = new BluetoothDevice[pairedDevices.size()];
            String[] items = new String[blueDev.length];
            int i = 0;
            for (BluetoothDevice devicel : pairedDevices) {
                blueDev[i] = devicel;
                items[i] = blueDev[i].getName() + ": " + blueDev[i].getAddress();
//                output.append("Device: " + items[i] + "\n");
                //mArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                i++;
            }
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle("Choose Bluetooth:");
            builder.setSingleChoiceItems(items, -1, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int item) {
                    dialog.dismiss();
                    if (item >= 0 && item < blueDev.length) {
                        device = blueDev[item];
                        try {
                            socket = device.createRfcommSocketToServiceRecord(MainActivity.MY_UUID);
                        } catch (IOException e) {
                            Log.d("nihao","nihao");
                        }
                        mBluetoothAdapter.cancelDiscovery();
                        // Make a connection to the BluetoothSocket
                        try {
                            // This is a blocking call and will only return on a
                            // successful connection or an exception
                            socket.connect();
                            Toast.makeText(MainActivity.this, "连接成功", Toast.LENGTH_SHORT)
                                    .show();
                        } catch (IOException e) {
                            try {
                                socket.close();
                                socket = null;
                            } catch (IOException e2) {
                                //mkmsg("unable to close() socket during connection failure: "+e2.getMessage()+"\n");
                                socket = null;
                            }
                            // Start the service over to restart listening mode
                        }
                    }
                }
            });
            AlertDialog alert = builder.create();
            alert.show();
        }
    }

    public void startUp() {
        if (device != null) {
            new Thread(new com.example.jetsoncarserver.ConnectThreadUp(device, handler, mBluetoothAdapter,socket)).start();
        }
    }

    public void startDown() {
        if (device != null) {
            new Thread(new com.example.jetsoncarserver.ConnectThreadDown(device, handler, mBluetoothAdapter,socket)).start();
        }
    }

    public void startLeft() {
        if (device != null) {
            new Thread(new com.example.jetsoncarserver.ConnectThreadLeft(device, handler, mBluetoothAdapter,socket)).start();
        }
    }

    public void startRight() {
        if (device != null) {
            new Thread(new com.example.jetsoncarserver.ConnectThreadRight(device, handler, mBluetoothAdapter,socket)).start();
        }
    }

    public void startStop() {
        if (device != null) {
            new Thread(new com.example.jetsoncarserver.ConnectThreadStop(device, handler, mBluetoothAdapter,socket)).start();
        }
    }

    public void Switch() {
        if (device != null) {
            new Thread(new com.example.jetsoncarserver.SwitchThread(device, handler, mBluetoothAdapter,socket)).start();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        menu.add(Menu.NONE, 0, Menu.NONE, "打开蓝牙");
        menu.add(Menu.NONE, 1, Menu.NONE, "关闭蓝牙");
        menu.add(Menu.NONE, 2, Menu.NONE, "扫描周围蓝牙设备");
        menu.add(Menu.NONE, 3, Menu.NONE, "切换模式");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        switch (id) {
            case 0:
                if (!mBluetoothAdapter.isEnabled()) {
                    mBluetoothAdapter.enable();
                    Toast.makeText(MainActivity.this, "蓝牙开启中", Toast.LENGTH_SHORT)
                            .show();
                }
                break;
            case 1:
                if (mBluetoothAdapter.isEnabled()) {
                    mBluetoothAdapter.disable();
                    Toast.makeText(MainActivity.this, "蓝牙已关闭", Toast.LENGTH_SHORT)
                            .show();
                }
                break;

            case 2:
                if (mBluetoothAdapter.isEnabled()) {
                    Toast.makeText(MainActivity.this, "开始扫描", Toast.LENGTH_SHORT)
                            .show();
                    querypaired();
                } else {
                    Toast.makeText(MainActivity.this, "请打开蓝牙", Toast.LENGTH_SHORT)
                            .show();
                }
                break;
            case 3:
                if (mBluetoothAdapter.isEnabled()) {
                    Toast.makeText(MainActivity.this, "切换成功", Toast.LENGTH_SHORT)
                            .show();
                    Switch();
                    state = (state + 1) % 2;
                    if (state == 0) {
                        stateView.setText("当前模式为：手动模式");
                    } else {
                        stateView.setText("当前模式为：自动模式");
                    }
                } else {
                    Toast.makeText(MainActivity.this, "请打开蓝牙", Toast.LENGTH_SHORT)
                            .show();
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}
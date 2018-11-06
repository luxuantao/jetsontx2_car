package com.example.jetsonserver;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    private String hostIp;
    private ServerSocket server = null;
    private Socket client = null;
    private static final int PORT = 7999;
    private ExecutorService executorService = null;
    private List<Socket> clients = new ArrayList<Socket>();
    private TextView textView1;
    private ImageView imageView;
    private Handler recImgHandler ;

    @SuppressLint("HandlerLeak")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        hostIp = getLocalIpAddress();
//        textView1 = (TextView)findViewById(R.id.textView);
//        textView1.setText(hostIp);
        imageView = (ImageView) findViewById(R.id.imageView);
        recImgHandler = new Handler() {
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case 1:
                        Bitmap bitmap = (Bitmap)msg.obj;
                        imageView.setImageBitmap(bitmap);
                        break;
                }
            }
        };
        ServerThread serverThread = new ServerThread();
        serverThread.start();
    }

    public static String getLocalIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface
                    .getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf
                        .getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress() && !inetAddress.isLinkLocalAddress()) {
                        return inetAddress.getHostAddress().toString();
                    }
                }
            }
        } catch (SocketException ex) {
            Log.e("Wifi IpAddress", ex.toString());
        }
        return null;
    }

    //Server端主线程
    class ServerThread extends Thread {
        public void run() {
            try {
                server = new ServerSocket(PORT);
            } catch (IOException e) {
                System.out.println("New Server Error");
                e.printStackTrace();
            }
            executorService = Executors.newCachedThreadPool();   //创建线程池
            System.out.println("Server has Started at port 9090");
            while(true) {
                try {
                    client = server.accept();
                    clients.add(client);
                    executorService.execute(new ImageService(client));
                }catch (IOException e) {
                    Log.e("client", e.toString());
                }
            }
        }
    }

    //接收client端图片的线程
    class ImageService implements Runnable {
        private Socket clientSocket = null;
        private InputStream inStream = null;
        private OutputStream outStream = null;
        private String imageStream = null;

        public ImageService(Socket socket) {
            this.clientSocket = socket;

        }
        public void run() {
            try {
                inStream = clientSocket.getInputStream();
                DataInputStream dataInputStream = new DataInputStream(inStream);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                while(true) {
                    int size = dataInputStream.readInt();
                    System.out.println("size = " + size);
                    byte[] data = new byte[40000];
//                    int len = dataInputStream.read(data);   读取图片最大长度是65536有限制
                    while(true) {
                        int n = dataInputStream.read(data);
                        size = size - n;   //判断剩余读取的字节数
                        System.out.println("n = " + n);
                        baos.write(data, 0, n);
                        if(size == 0 )  break;
                    }
                    byte[] imageBytes = baos.toByteArray();
                    baos.reset();
                    int len = imageBytes.length;
                    System.out.println("lenth = " + len );
                    System.out.println(Arrays.toString(imageBytes));
                    Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, len);
                    Message message = recImgHandler.obtainMessage();
                    message.what = 1;
                    message.obj = bitmap;
                    recImgHandler.sendMessage(message);
                }

            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        //向客户端发送信息
        public void sendMsg() {

        }
    }
}

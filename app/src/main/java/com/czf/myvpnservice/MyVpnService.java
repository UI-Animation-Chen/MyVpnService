package com.czf.myvpnservice;

import android.content.Intent;
import android.net.VpnService;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class MyVpnService extends VpnService {

  private DatagramSocket mSocket;
  private ParcelFileDescriptor mVpnFd;
  private Thread mReadThread;
  private Thread mWriteThread;

  private String tunInterfaceIP = "192.168.4.234";
  private String serverIP = "192.168.4.144";
  private int serverPort = 9999;

  public MyVpnService() {

  }

  @Override
  public void onCreate() {
    if (setUpDatagramSocket()) {
      protect(mSocket); // 此socket不走vpn，不然就死循环了。
      Builder builder = new Builder();
      mVpnFd = builder
          .addAddress(tunInterfaceIP, 24) //
          .addRoute("0.0.0.0", 0) // 转发所有的流量
          .establish();
      startReadThread();
      startWriteThread();
    }
    Log.d("---------", "service oncreate");
  }

  private boolean setUpDatagramSocket() {
    try {
      mSocket = new DatagramSocket();
      return true;
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    }
  }

  private void startReadThread() {
    mReadThread = new Thread(new Runnable() {
      @Override
      public void run() {
        FileInputStream fis = new FileInputStream(mVpnFd.getFileDescriptor());
        byte[] readBuf = new byte[1500];
        try {
          mSocket.connect(InetAddress.getByName(serverIP), serverPort); // udp的connect仅仅是指明目的地
          Log.d("--------", "socket connected");
          while (true) {
            int readLen = fis.read(readBuf);
            if (readLen > 0) {
              Log.d("----------", "read thread, len: " + readLen);
              DatagramPacket packet = new DatagramPacket(readBuf, readLen);
              mSocket.send(packet);
              Thread.sleep(300);
            } else {
              Thread.sleep(500);
            }
          }
        } catch (Exception e) {
          e.printStackTrace();
        }
        Log.d("-------", "read thread exit");
      }
    });
    mReadThread.start();
  }

  private void startWriteThread() {
    mWriteThread = new Thread(new Runnable() {
      @Override
      public void run() {
        FileOutputStream fos = new FileOutputStream(mVpnFd.getFileDescriptor());
        byte[] readBuf = new byte[1500];
        DatagramPacket packet = new DatagramPacket(readBuf, readBuf.length);
        try {
          mSocket.connect(InetAddress.getByName(serverIP), serverPort); // udp的connect仅仅是指明目的地
          Log.d("--------", "socket connected");
          while (true) {
            mSocket.receive(packet);
            fos.write(readBuf, 0, packet.getLength());
            Log.d("----------", "write thread, len: " + packet.getLength());
          }
        } catch (IOException e) {
          e.printStackTrace();
        }
        Log.d("-------", "write thread exit");
      }
    });
    mWriteThread.start();
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    Log.d("---------", "service onstart command");
    return super.onStartCommand(intent, flags, startId);
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    Log.d("---------", "service ondestroy");
  }

  @Override
  public void onRevoke() {
    super.onRevoke();
    if (mVpnFd != null) {
      try {
        mVpnFd.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }
}

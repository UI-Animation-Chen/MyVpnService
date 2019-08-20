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

  private String tunInterfaceIP = "192.168.8.178";
  private String serverIP = "192.168.8.141";

  public MyVpnService() {

  }

  @Override
  public void onCreate() {
    if (setUpDatagramSocket()) {
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
        byte[] readBuf = new byte[8192];
        int readLen;
        try {
          mSocket.connect(InetAddress.getByName(serverIP), 7777); // udp的connect仅仅是指明目的地
          while ((readLen = fis.read(readBuf)) > 0) {
            Log.d("----------", "read thread: " + readBuf.toString());
            DatagramPacket packet = new DatagramPacket(readBuf, readLen);
            mSocket.send(packet);
          }
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    });
    mReadThread.start();
  }

  private void startWriteThread() {
    mWriteThread = new Thread(new Runnable() {
      @Override
      public void run() {
        FileOutputStream fos = new FileOutputStream(mVpnFd.getFileDescriptor());
        byte[] readBuf = new byte[8192];
        DatagramPacket packet = new DatagramPacket(readBuf, 8192);
        try {
          mSocket.connect(InetAddress.getByName(serverIP), 7777); // udp的connect仅仅是指明目的地
          while (true) {
            mSocket.receive(packet);
            fos.write(readBuf, 0, packet.getLength());
            Log.d("----------", "write thread: " + readBuf.toString());
          }
        } catch (IOException e) {
          e.printStackTrace();
        }
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

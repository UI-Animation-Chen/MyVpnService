package com.czf.myvpnservice;

import android.content.Intent;
import android.net.VpnService;
import android.os.ParcelFileDescriptor;

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

  public MyVpnService() {

  }

  @Override
  public void onCreate() {
    if (setUpDatagramSocket()) {
      Builder builder = new Builder();
      mVpnFd = builder
          .addAddress("", 24) //
          .addRoute("0.0.0.0", 0) // 转发所有的流量
          .establish();
      startReadThread();
      startWriteThread();
    }
  }

  private boolean setUpDatagramSocket() {
    try {
      mSocket = new DatagramSocket();
      mSocket.connect(InetAddress.getByName("localhost"), 7777); // udp的connect仅仅是指明目的地
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
          while ((readLen = fis.read(readBuf)) != -1) {
            DatagramPacket packet = new DatagramPacket(readBuf, readLen);
            mSocket.send(packet);
          }
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    });
  }

  private void startWriteThread() {
    mWriteThread = new Thread(new Runnable() {
      @Override
      public void run() {
        FileOutputStream fos = new FileOutputStream(mVpnFd.getFileDescriptor());
        byte[] readBuf = new byte[8192];
        DatagramPacket packet = new DatagramPacket(readBuf, 8192);
        try {
          while (true) {
            mSocket.receive(packet);
            fos.write(readBuf, 0, packet.getLength());
          }
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    });
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    return super.onStartCommand(intent, flags, startId);
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
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

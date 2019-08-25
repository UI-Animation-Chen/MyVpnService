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
import java.net.SocketTimeoutException;

public class MyVpnService extends VpnService {

  private DatagramSocket mTunnel;
  private ParcelFileDescriptor mVpnFd;
  private Thread mReadThread;
  private Thread mWriteThread;

  private boolean tunnelCreated = false;

  private String tunInterfaceIP = "192.168.8.234";
  private String serverIP = "192.168.8.141";
//  private String serverIP = "10.200.0.45";
  private int serverPort = 12346;

  public MyVpnService() {

  }

  @Override
  public void onCreate() {
    if (createTunnel()) {
      protect(mTunnel); // 此socket不走vpn，不然就死循环了。
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

  private boolean createTunnel() {
    tunnelCreated = false;
    Thread t = new Thread(new Runnable() {
      @Override
      public void run() {
        try {
          mTunnel = new DatagramSocket();
          mTunnel.connect(InetAddress.getByName(serverIP), serverPort);
          mTunnel.setSoTimeout(10 * 1000);
          byte[] buf = new byte[]{0, 0, 0, 0};
          DatagramPacket p = new DatagramPacket(buf, buf.length);
          while (true) {
            mTunnel.send(p);
            try {
              mTunnel.receive(p);
            } catch (SocketTimeoutException e) {
              e.printStackTrace();
              continue;
            }
            mTunnel.setSoTimeout(0); // 去掉超时设置
            tunnelCreated = true;
            Log.d("------", "tunnel created");
            break;
          }
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    });
    t.start();
    try {
      t.join();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    return tunnelCreated;
  }

  private void startReadThread() {
    mReadThread = new Thread(new Runnable() {
      @Override
      public void run() {
        FileInputStream fis = new FileInputStream(mVpnFd.getFileDescriptor());
        byte[] readBuf = new byte[1500];
        try {
          while (true) {
            int readLen = fis.read(readBuf, 0, readBuf.length); // 读到的是一个IP包
            if (readLen > 0) {
              Log.d("----------", "read thread, len: " + readLen);
              for (int i = 0; i < readLen; i++) {
                Log.d("------", "buf[" + i + "]: " + (readBuf[i] & 0xff));
              }
              DatagramPacket packet = new DatagramPacket(readBuf, readLen);
              mTunnel.send(packet);
            } else {
              Thread.sleep(200);
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
          while (true) {
            mTunnel.receive(packet);
            if (readBuf[0] == 0 && readBuf[1] == 0 && readBuf[2] == 0 && readBuf[3] == 0) {
              continue; // skip handle shake packets
            }
            fos.write(packet.getData(), 0, packet.getLength()); // 写入的应该是一个IP包
            Log.d("-------", "write thread, len: " + packet.getLength());
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

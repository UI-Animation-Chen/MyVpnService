package com.czf.myvpnservice;

import android.content.Intent;
import android.net.VpnService;
import android.os.ParcelFileDescriptor;

import java.io.FileDescriptor;
import java.io.IOException;
import java.net.Socket;

public class MyVpnService extends VpnService {

  private Socket mScoket;
  private ParcelFileDescriptor mVpnFd;
  private Thread mReadThread;
  private Thread mWriteThread;

  public MyVpnService() {

  }

  @Override
  public void onCreate() {
    mScoket = new Socket();
    Builder builder = new Builder();
    mVpnFd = builder.establish();
    startReadThread();
    startWriteThread();
  }

  private void startReadThread() {
    mReadThread = new Thread(new Runnable() {
      @Override
      public void run() {
        FileDescriptor fd = mVpnFd.getFileDescriptor();

      }
    });
  }

  private void startWriteThread() {
    mWriteThread = new Thread(new Runnable() {
      @Override
      public void run() {

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

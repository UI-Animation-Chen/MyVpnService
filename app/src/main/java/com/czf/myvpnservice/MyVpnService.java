package com.czf.myvpnservice;

import android.content.Intent;
import android.net.VpnService;

public class MyVpnService extends VpnService {

  public MyVpnService() {

  }

  @Override
  public void onCreate() {
    super.onCreate();
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
  }
}

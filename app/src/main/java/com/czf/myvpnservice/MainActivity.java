package com.czf.myvpnservice;

import android.content.Intent;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import toyvpndemo.ToyVpnClient;

public class MainActivity extends AppCompatActivity {

  private final int START_VPN_REQUEST_CODE = 1;
  private boolean serviceStarted = false;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    findViewById(R.id.start_vpn).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        if (serviceStarted) {
          Intent i = new Intent(MainActivity.this, MyVpnService.class);
          i.setAction(MyVpnService.DISCONNECT_ACTION);
          startService(i);
        } else {
          Intent intent = MyVpnService.prepare(MainActivity.this);
          if (intent != null) {
            startActivityForResult(intent, 1);
          } else {
            startVpnService();
          }
        }
        serviceStarted = !serviceStarted;
      }
    });

    findViewById(R.id.start_toy_vpn).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        Intent i = new Intent(MainActivity.this, ToyVpnClient.class);
        startActivity(i);
      }
    });
  }

  private void startVpnService() {
    Intent i = new Intent(this, MyVpnService.class);
    i.setAction(MyVpnService.CONNECT_ACTION);
    startService(i);
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
    if (requestCode == START_VPN_REQUEST_CODE) {
      if (resultCode == RESULT_OK) {
        startVpnService();
      }
    }
  }
}

package com.czf.myvpnservice;

import android.content.Intent;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

public class MainActivity extends AppCompatActivity {

  private final int START_VPN_REQUEST_CODE = 1;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    findViewById(R.id.start_vpn).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        Intent intent = MyVpnService.prepare(MainActivity.this);
        if (intent != null) {
          startActivityForResult(intent, 1);
        } else {
          startVpnService();
        }
      }
    });
  }

  private void startVpnService() {
    startService(new Intent(this, MyVpnService.class));
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

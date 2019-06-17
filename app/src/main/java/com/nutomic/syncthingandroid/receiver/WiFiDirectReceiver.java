package com.nutomic.syncthingandroid.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;
import android.net.wifi.p2p.WifiP2pManager;

import com.nutomic.syncthingandroid.activities.MainActivity;

public class WiFiDirectReceiver extends BroadcastReceiver {
    private static final String TAG = "WiFiDirectReceiver";

    private WifiP2pManager mManager;
    private WifiP2pManager.Channel mChannel;
    private MainActivity mActivity;

    public WiFiDirectReceiver(WifiP2pManager mManager,
                WifiP2pManager.Channel mChannel,
                MainActivity mActivity) {
        this.mManager = mManager;
        this.mChannel = mChannel;
        this.mActivity = mActivity;
        Log.w(TAG, "WiFiDirectReceiver::init");
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.w(TAG, "WiFiDirectReceiver:onReceive " + action);
        switch (action) {
            case WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION:
                int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
                if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                    Toast.makeText(context, "Wifi is ON", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(context, "Wifi is OFF", Toast.LENGTH_SHORT).show();
                }
                break;
            case WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION:
                if (mManager != null) {
                    mManager.requestPeers(mChannel, mActivity.mPeerListListener);
                }
                break;
            case WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION:
                break;
            case WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION:
                break;
        }
    }
}

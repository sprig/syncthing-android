package com.nutomic.syncthingandroid.fragments;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import com.google.common.base.Optional;
import com.nutomic.syncthingandroid.R;
import com.nutomic.syncthingandroid.SyncthingApp;
import com.nutomic.syncthingandroid.activities.MainActivity;
import com.nutomic.syncthingandroid.activities.SettingsActivity;
import com.nutomic.syncthingandroid.activities.SyncthingActivity;
import com.nutomic.syncthingandroid.model.Connections;
import com.nutomic.syncthingandroid.model.SystemStatus;
import com.nutomic.syncthingandroid.receiver.WiFiDirectReceiver;
import com.nutomic.syncthingandroid.service.AppPrefs;
import com.nutomic.syncthingandroid.service.Constants;
import com.nutomic.syncthingandroid.service.RestApi;
import com.nutomic.syncthingandroid.service.SyncthingService;
import com.nutomic.syncthingandroid.util.Util;

import java.util.concurrent.TimeUnit;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;
import java.text.NumberFormat;

import javax.inject.Inject;

//
import com.nutomic.syncthingandroid.service.ReceiverManager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pDeviceList;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

/**
 * Displays why syncthing is running or disabled.
 */
public class WidiFragment extends Fragment implements SyncthingService.OnServiceStateChangeListener {

    private static final String TAG = "WidiFragment";

    private Boolean ENABLE_VERBOSE_LOG = false;

    @Inject SharedPreferences mPreferences;

/*
    private Runnable mRestApiQueryRunnable = new Runnable() {
        @Override
        public void run() {
            onTimerEvent();
            mRestApiQueryHandler.postDelayed(this, Constants.GUI_UPDATE_INTERVAL);
        }
    };*/

    private MainActivity mActivity;
    private ArrayAdapter mAdapter;
    private SyncthingService.State mServiceState = SyncthingService.State.INIT;
    // private final Handler mRestApiQueryHandler = new Handler();
    private Boolean mLastVisibleToUser = false;

    Button btnOnOff, btnDiscover, btnSend;
    ListView listView;
    TextView read_msg_box;
    public TextView connectionStatus;
    EditText writeMsg;

    WifiManager wifiManager;
    WifiP2pManager mManager;
    WifiP2pManager.Channel mChannel;
    WiFiDirectReceiver mWiFiDirectReceiver;
    IntentFilter mIntentFilter;

    List<WifiP2pDevice> peers = new ArrayList<WifiP2pDevice>();
    String[] deviceNameArray;
    WifiP2pDevice[] deviceArray;

    private ReceiverManager mReceiverManager;

    /**
     * Object that must be locked upon accessing the status holders.
     */
    //private final Object mStatusHolderLock = new Object();

    /**
     * Status holders, filled on callbacks.
     */
     /*
    private String mCpuUsage = "";
    private String mRamUsage = "";
    private String mDownload = "";
    private String mUpload = "";
    private String mAnnounceServer = "";
    private String mUptime = "";*/

    // ToDo
    View mWidiView;
    Context mContext;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((SyncthingApp) getActivity().getApplication()).component().inject(this);
        ENABLE_VERBOSE_LOG = AppPrefs.getPrefVerboseLog(mPreferences);
    }

    private void exqListener() {
        btnOnOff.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (wifiManager.isWifiEnabled()) {
                        wifiManager.setWifiEnabled(false);
                        btnOnOff.setText("OFF - TURN ON");
                    } else {
                        wifiManager.setWifiEnabled(true);
                        btnOnOff.setText("ON - TURN OFF");
                    }
                }
        });

        btnDiscover.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
                            @Override
                            public void onSuccess() {
                                connectionStatus.setText("Discovery succeeded.");
                            }

                            @Override
                            public void onFailure(int i) {
                                connectionStatus.setText("Discovery failed.");
                            }
                    });
                }
        });

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                    final WifiP2pDevice device = deviceArray[i];
                    WifiP2pConfig config = new WifiP2pConfig();
                    config.deviceAddress = device.deviceAddress;

                    mManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {
                            @Override
                            public void onSuccess() {
                                Toast.makeText(mContext, "Connected to " + device.deviceName + " succeeded.", Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onFailure(int i) {
                                Toast.makeText(mContext, "Failed to connect to " + device.deviceName + ".", Toast.LENGTH_SHORT).show();
                            }
                    });
                }
        });
    }

    public final WifiP2pManager.PeerListListener mPeerListListener = new WifiP2pManager.PeerListListener() {
            @Override
            public void onPeersAvailable(WifiP2pDeviceList peerList) {
                if (!peerList.getDeviceList().equals(peers)) {
                    peers.clear();
                    peers.addAll(peerList.getDeviceList());

                    deviceNameArray = new String[peerList.getDeviceList().size()];
                    deviceArray = new WifiP2pDevice[peerList.getDeviceList().size()];
                    int index = 0;
                    for (WifiP2pDevice device : peerList.getDeviceList()) {
                        deviceNameArray[index] = device.deviceName;
                        deviceArray[index] = device;
                        index++;
                    }

                    ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                            mContext,
                            android.R.layout.simple_list_item_1,
                            deviceNameArray);
                    listView.setAdapter(adapter);
                }

                if (peers.size() == 0) {
                    Toast.makeText(mContext, "No Device Found", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
    };

    public final WifiP2pManager.ConnectionInfoListener connectionInfoListener = new WifiP2pManager.ConnectionInfoListener() {
        @Override
        public void onConnectionInfoAvailable(WifiP2pInfo wifiP2pInfo) {
            final InetAddress groupOwnerAddress = wifiP2pInfo.groupOwnerAddress;
            if (wifiP2pInfo.groupFormed) {
                connectionStatus.setText(wifiP2pInfo.isGroupOwner ? "Group Owner" : "Group Member");
            }
        }
    };







    @Override
    public void setUserVisibleHint(boolean isVisibleToUser)
    {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {
            // User switched to the current tab, start handler.
            startRestApiQueryHandler();
        } else {
            // User switched away to another tab, stop handler.
            stopRestApiQueryHandler();
        }
        mLastVisibleToUser = isVisibleToUser;
    }

    @Override
    public void onPause() {
        stopRestApiQueryHandler();
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mLastVisibleToUser) {
            startRestApiQueryHandler();
        }
    }

    private void startRestApiQueryHandler() {
        LogV("startUpdateListHandler");
        //mRestApiQueryHandler.removeCallbacks(mRestApiQueryRunnable);
        //mRestApiQueryHandler.post(mRestApiQueryRunnable);
        // mContext.registerReceiver(mWiFiDirectReceiver, mIntentFilter);
        if (mContext != null) {
            ReceiverManager.registerReceiver(mContext, mWiFiDirectReceiver, mIntentFilter);
        }
    }

    private void stopRestApiQueryHandler() {
        LogV("stopUpdateListHandler");
        //mRestApiQueryHandler.removeCallbacks(mRestApiQueryRunnable);
        //mContext.unregisterReceiver(mWiFiDirectReceiver);
        if (mContext != null) {
            mReceiverManager.unregisterAllReceivers(mContext);
        }
    }

    @Override
    public void onServiceStateChange(SyncthingService.State currentState) {
        //mServiceState = currentState;
        //updateStatus();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        Log.w(TAG, "onCreateView");
        mWidiView = inflater.inflate(R.layout.fragment_widi, container, false);
        return mWidiView;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        Log.w(TAG, "onViewCreated");
        super.onViewCreated(view, savedInstanceState);
        //mAdapter = new ArrayAdapter(getActivity(), android.R.layout.simple_list_item_1);
        //setListAdapter(mAdapter);
        //setHasOptionsMenu(true);
        // updateStatus();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        Log.w(TAG, "onActivityCreated");
        super.onActivityCreated(savedInstanceState);
        mActivity = (MainActivity) getActivity();
        mContext = getActivity().getApplicationContext();

        // Initialwork.
        btnOnOff = (Button) mWidiView.findViewById(R.id.onOff);
        btnDiscover = (Button) mWidiView.findViewById(R.id.discover);
        btnSend = (Button) mWidiView.findViewById(R.id.sendButton);
        listView = (ListView) mWidiView.findViewById(R.id.peerListView);
        read_msg_box = (TextView) mWidiView.findViewById(R.id.readMsg);
        connectionStatus = (TextView) mWidiView.findViewById(R.id.connectionStatus);
        writeMsg = (EditText) mWidiView.findViewById(R.id.writeMsg);

        wifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);

        mManager = (WifiP2pManager) mContext.getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mManager.initialize(mActivity, Looper.getMainLooper(), null);

        // mWiFiDirectReceiver = new WiFiDirectReceiver(mManager, mChannel, mActivity);
        mWiFiDirectReceiver = new WiFiDirectReceiver(mManager, mChannel, WidiFragment.this);

        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        if (!wifiManager.isWifiEnabled()) {
            wifiManager.setWifiEnabled(true);
            btnOnOff.setText("ON - TURN OFF");
        }
        btnOnOff.setText("OFF - TURN ON");
        exqListener();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        //inflater.inflate(R.menu.status_list, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            /*
            case R.id.open_preferences:
                startActivity(new Intent(getContext(), SettingsActivity.class));
                return true;
                */
            default:
                return super.onOptionsItemSelected(item);
        }
    }

/*
    private void updateStatus() {
        SyncthingActivity syncthingActivity = (SyncthingActivity) getActivity();
        if (syncthingActivity == null || getView() == null || syncthingActivity.isFinishing()) {
            return;
        }
        SyncthingService syncthingService = syncthingActivity.getService();
        if (syncthingService == null) {
            return;
        }

        // Add status line showing the syncthing service state.
        ArrayList<String> statusItems = new ArrayList<String>();
        switch (mServiceState) {
            case INIT:
            case STARTING:
                statusItems.add(getString(R.string.syncthing_starting));
                break;
            case ACTIVE:
                statusItems.add(getString(R.string.syncthing_running));
                break;
            case DISABLED:
                statusItems.add(getString(R.string.syncthing_not_running));
                break;
            case ERROR:
                statusItems.add(getString(R.string.syncthing_has_crashed));
                break;
        }

        // Add explanation why syncthing is (not) running.
        switch (mServiceState) {
            case ACTIVE:
            case DISABLED:
                statusItems.add(getString(R.string.reason) + "\n" +
                    "- " + syncthingService.getRunDecisionExplanation().trim().replace("\n", "\n- "));
            default:
                break;
        }

        // Add status holders refreshed by callbacks to the list.
        if (mServiceState == SyncthingService.State.ACTIVE) {
            synchronized (mStatusHolderLock) {
                if (!TextUtils.isEmpty(mUptime)) {
                    statusItems.add(getString(R.string.uptime) + ": " + mUptime);
                }
                if (!TextUtils.isEmpty(mRamUsage)) {
                    statusItems.add(getString(R.string.ram_usage) + ": " + mRamUsage);
                }
                if (!TextUtils.isEmpty(mCpuUsage)) {
                    statusItems.add(getString(R.string.cpu_usage) + ": " + mCpuUsage);
                }
                if (!TextUtils.isEmpty(mDownload)) {
                    statusItems.add(getString(R.string.download_title) + ": " + mDownload);
                }
                if (!TextUtils.isEmpty(mUpload)) {
                    statusItems.add(getString(R.string.upload_title) + ": " + mUpload);
                }
                if (!TextUtils.isEmpty(mAnnounceServer)) {
                    statusItems.add(getString(R.string.announce_server) + ": " + mAnnounceServer);
                }
            }
        }

        // Update list contents.
        mAdapter.setNotifyOnChange(false);
        mAdapter.clear();
        mAdapter.addAll(statusItems);
        mAdapter.notifyDataSetChanged();
    }*/

    /**
     * Invokes status callbacks via syncthing's REST API
     * while the user is looking at the current tab.
     */
     /*
    private void onTimerEvent() {
        MainActivity mainActivity = (MainActivity) getActivity();
        if (mainActivity == null) {
            return;
        }
        if (mainActivity.isFinishing()) {
            return;
        }
        if (mServiceState != SyncthingService.State.ACTIVE) {
            updateStatus();
            return;
        }
        RestApi restApi = mainActivity.getApi();
        if (restApi == null) {
            return;
        }
        LogV("Invoking REST status queries");
        restApi.getSystemStatus(this::onReceiveSystemStatus);
        restApi.getConnections(this::onReceiveConnections);
        // onReceiveSystemStatus, onReceiveConnections will call {@link #updateStatus}.
    }
    */

    /**
     * Populates status holders with status received via {@link RestApi#getSystemStatus}.
     */
     /*
    private void onReceiveSystemStatus(SystemStatus systemStatus) {
        if (getActivity() == null) {
            return;
        }
        NumberFormat percentFormat = NumberFormat.getPercentInstance();
        percentFormat.setMaximumFractionDigits(2);
        int announceTotal = systemStatus.discoveryMethods;
        int announceConnected =
                announceTotal - Optional.fromNullable(systemStatus.discoveryErrors).transform(Map::size).or(0);
        synchronized (mStatusHolderLock) {
            mCpuUsage = (systemStatus.cpuPercent < 5) ? "" : percentFormat.format(systemStatus.cpuPercent / 100);
            mRamUsage = Util.readableFileSize(mActivity, systemStatus.sys);
            mAnnounceServer = (announceTotal == 0) ?
                    "" :
                    String.format(Locale.getDefault(), "%1$d/%2$d", announceConnected, announceTotal);

            /**
             * Calculate readable uptime.
             */
             /*
            long uptimeDays = TimeUnit.SECONDS.toDays(systemStatus.uptime);
            long uptimeHours = TimeUnit.SECONDS.toHours(systemStatus.uptime) - TimeUnit.DAYS.toHours(uptimeDays);
            long uptimeMinutes = TimeUnit.SECONDS.toMinutes(systemStatus.uptime) - TimeUnit.HOURS.toMinutes(uptimeHours) - TimeUnit.DAYS.toMinutes(uptimeDays);
            if (uptimeDays > 0) {
                mUptime = String.format(Locale.getDefault(), "%dd %02dh %02dm", uptimeDays, uptimeHours, uptimeMinutes);
            } else if (uptimeHours > 0) {
                mUptime = String.format(Locale.getDefault(), "%dh %02dm", uptimeHours, uptimeMinutes);
            } else {
                mUptime = String.format(Locale.getDefault(), "%dm", uptimeMinutes);
            }
        }
        updateStatus();
    }*/

    private void LogV(String logMessage) {
        if (ENABLE_VERBOSE_LOG) {
            Log.v(TAG, logMessage);
        }
    }
}

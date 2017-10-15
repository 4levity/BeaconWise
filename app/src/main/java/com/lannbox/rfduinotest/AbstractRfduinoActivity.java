package com.lannbox.rfduinotest;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import java.util.UUID;

/**
 * Created by ivan on 10/15/17.
 */

public abstract class AbstractRfduinoActivity extends AppCompatActivity
        implements BluetoothAdapter.LeScanCallback {

    private static final String TAG = AbstractRfduinoActivity.class.getSimpleName();

    // State machine
    final private static int STATE_BLUETOOTH_OFF = 1;
    final private static int STATE_DISCONNECTED = 2;
    final private static int STATE_CONNECTING = 3;
    final private static int STATE_CONNECTED = 4;

    private int state;

    private boolean scanStarted;
    private boolean scanning;

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothDevice bluetoothDevice;

    private RFduinoService rfduinoService;

    /**
     * Subclass implements to handle data from rfduino.
     *
     * @param byteArrayExtra data
     */
    protected abstract void handleRfduinoExtraData(byte[] byteArrayExtra);

    /**
     * Subclass implements to display status string.
     *
     * @param status BLE status
     */
    protected abstract void showBluetoothStatus(String status);

    /**
     * Subclass implements to receive scan data.
     *
     * @param device device
     * @param rssi signal strength
     * @param description text
     */
    protected abstract void acceptDeviceInfo(BluetoothDevice device, int rssi, String description);

    /**
     * Subclass calls to send data to rfduino.
     *
     * @param bytes data
     */
    protected void rfduinoSend(byte[] bytes) {
        if (rfduinoService != null) {
            rfduinoService.send(bytes);
        } else {
            Log.w(TAG, "not sending, rfduinoService not available");
        }
    }

    protected void rfduinoStartScan() {
        scanStarted = true;
        bluetoothAdapter.startLeScan(
                new UUID[]{ RFduinoService.UUID_SERVICE },
                this);
        updateUi();
    }

    protected void rfduinoConnect() {
        Intent rfduinoIntent = new Intent(this, RFduinoService.class);
        bindService(rfduinoIntent, rfduinoServiceConnection, BIND_AUTO_CREATE);
        updateUi();
    }

    private final BroadcastReceiver bluetoothStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0);
            if (state == BluetoothAdapter.STATE_ON) {
                upgradeState(STATE_DISCONNECTED);
            } else if (state == BluetoothAdapter.STATE_OFF) {
                downgradeState(STATE_BLUETOOTH_OFF);
            }
        }
    };

    private final BroadcastReceiver scanModeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            scanning = (bluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_NONE);
            scanStarted &= scanning;
            updateUi();
        }
    };

    private final ServiceConnection rfduinoServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            rfduinoService = ((RFduinoService.LocalBinder) service).getService();
            if (rfduinoService.initialize()) {
                Log.i(TAG, "Connecting to device");
                if (rfduinoService.connect(bluetoothDevice.getAddress())) {
                    Log.i(TAG, "Connected to device");
                    upgradeState(STATE_CONNECTED);
                } else {
                    Log.e(TAG, "Did not connect to device!");
                }
            } else {
                Log.e(TAG, "rfduinoService did not initialize!");
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            rfduinoService = null;
            downgradeState(STATE_DISCONNECTED);
        }
    };

    private final BroadcastReceiver rfduinoReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, "onRecieve " + intent.getAction());
            final String action = intent.getAction();
            if (RFduinoService.ACTION_CONNECTED.equals(action)) {
                upgradeState(STATE_CONNECTED);
            } else if (RFduinoService.ACTION_DISCONNECTED.equals(action)) {
                downgradeState(STATE_DISCONNECTED);
            } else if (RFduinoService.ACTION_DATA_AVAILABLE.equals(action)) {
                handleRfduinoExtraData(intent.getByteArrayExtra(RFduinoService.EXTRA_DATA));
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        showBluetoothStatus("BLE initializing..");
    }

    @Override
    protected void onStart() {
        super.onStart();

        registerReceiver(scanModeReceiver, new IntentFilter(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED));
        registerReceiver(bluetoothStateReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
        registerReceiver(rfduinoReceiver, RFduinoService.getIntentFilter());

        updateState(bluetoothAdapter.isEnabled() ? STATE_DISCONNECTED : STATE_BLUETOOTH_OFF);
    }

    @Override
    protected void onStop() {
        super.onStop();

        bluetoothAdapter.stopLeScan(this);

        unregisterReceiver(scanModeReceiver);
        unregisterReceiver(bluetoothStateReceiver);
        unregisterReceiver(rfduinoReceiver);
    }

    private void upgradeState(int newState) {
        Log.i(TAG, "upgradeState: " + state + " -> " + newState);
        if (newState > state) {
            updateState(newState);
        }
    }

    private void downgradeState(int newState) {
        Log.i(TAG, "downgradeState: " + state + " -> " + newState);
        if (newState < state) {
            updateState(newState);
        }
    }

    private void updateState(int newState) {
        Log.i(TAG, "updateState: " + newState);
        state = newState;
        updateUi();
    }

    private void updateUi() {
        final String[] message = new String[1];
        // Scan
        if (scanStarted && scanning) {
            message[0] = "BLE Scanning...";
        } else if (scanStarted) {
            message[0] = "BLE Scan started...";
        } else if (state == STATE_CONNECTING) {
            message[0] = "BLE Connecting...";
        } else if (state == STATE_CONNECTED) {
            message[0] = "BLE Connected";
        } else {
            message[0] = "BLE Disconnected";
        }
        Log.i(TAG, "update UI: " + message[0]);
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                showBluetoothStatus(message[0]);
            }
        });
    }

    @Override
    public void onLeScan(BluetoothDevice device, final int rssi, final byte[] scanRecord) {
        bluetoothAdapter.stopLeScan(this);
        bluetoothDevice = device;

        Log.i(TAG, "onLeScan() found device, stopping scan");
        String description = BluetoothHelper.getDeviceInfoText(bluetoothDevice,
                rssi, scanRecord);
        acceptDeviceInfo(bluetoothDevice, rssi, description);
        updateUi();
    }
}

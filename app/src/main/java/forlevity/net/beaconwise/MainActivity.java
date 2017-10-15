package forlevity.net.beaconwise;

import android.bluetooth.BluetoothDevice;
import android.os.Handler;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.lannbox.rfduinotest.AbstractRfduinoActivity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AbstractRfduinoActivity implements Runnable {

    private static final String TAG = MainActivity.class.getSimpleName();

    private static final int SAMPLE_RATE_SAMPLES_PER_SEC = 96000; // or try 48000, 44100
    private static final int WINDOW_SIZE = 2048;

    private static final long UPDATE_INTERVAL_MILLIS = 100;
    //  (long) (1000.0 / (SAMPLE_RATE_SAMPLES_PER_SEC / WINDOW_SIZE));

    // bands / frequency histogram buckets: 17500, 17750, 18000, 18250... 22500
    private static final int MINIMUM_FREQUENCY = 17250;
    private static final int STEP_FREQUENCY = 500;
    private static final int NUM_BANDS = 10;
    private static final int ROW_FREQUENCIES = NUM_BANDS;
    private static final int ROW_BLE_STATUS = NUM_BANDS + 1;

    private static final int LAST_SIGNALS_BUFFER_SIZE = 32;

    private Handler handler;
    private Thread microphoneListenerThread;
    private UltrasoundDetector ultrasoundDetector;
    private RingBuffer<boolean[]> signals;
    private List<String> bandActivity;
    private ArrayAdapter frequencyListAdapter;
    private boolean[] currentLedState = new boolean[2];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "starting!");
        setContentView(R.layout.activity_main);
        bandActivity = new ArrayList<>();
        frequencyListAdapter = new ArrayAdapter<>(this,
                R.layout.frequency_list_item, bandActivity);
        ListView list = (ListView)findViewById(R.id.frequencyList);
        list.setAdapter(frequencyListAdapter);
        for (int band = 0; band < NUM_BANDS; band++) {
            bandActivity.add("starting ...");
        }
        bandActivity.add("please wait ...");
        bandActivity.add("BLE");

        super.onCreate(savedInstanceState);

        this.signals = new RingBuffer<>(new boolean[LAST_SIGNALS_BUFFER_SIZE][NUM_BANDS]);
        ultrasoundDetector = new UltrasoundDetector(SAMPLE_RATE_SAMPLES_PER_SEC, WINDOW_SIZE,
                MINIMUM_FREQUENCY, STEP_FREQUENCY, NUM_BANDS, signals);
        MicrophoneListener listener =
                new MicrophoneListener(SAMPLE_RATE_SAMPLES_PER_SEC, ultrasoundDetector);

        microphoneListenerThread = new Thread(listener);
        microphoneListenerThread.start();

        // start updating window
        handler = new Handler();
        handler.post(this);

        rfduinoStartScan();
    }

    @Override
    public void run() {
        update();

        // update again in a few ms
        handler.postDelayed(this, UPDATE_INTERVAL_MILLIS);
    }

    private void update() {
        StringBuilder frequenciesBuilder = new StringBuilder("frequencies: ");
        int anyUltrasound = 0;
        int beaconFrequenciesDetected = 0;
        for (int band = 0; band < NUM_BANDS; band++) {
            StringBuilder activity = new StringBuilder(
                    String.format("%d ",ultrasoundDetector.frequency(band)));
            boolean[] signal = null;
            for (int ix = 0; ix < signals.count(); ix++) {
                signal = signals.get(ix);
                if (signal[band]) {
                    activity.append("*");
                    if (ix > signals.count() / 2) {
                        // only look at last half of buffer
                        anyUltrasound++;
                    }
                    int freq = ultrasoundDetector.frequency(band);
                    if (freq == 18500 || freq == 21000) {
                        // look at whole buffer, certain frequencies
                        beaconFrequenciesDetected++;
                    }
                } else {
                    activity.append(" ");
                }
            }
            if (signal != null && signal[band]) {
                frequenciesBuilder.append(String.format("%d ", ultrasoundDetector.frequency(band)));
            }
            this.bandActivity.set(band, activity.toString());
        }
        boolean beaconDetected = beaconFrequenciesDetected > (LAST_SIGNALS_BUFFER_SIZE);
        boolean ultrasound = anyUltrasound > (LAST_SIGNALS_BUFFER_SIZE / 8);
        setLedState(ultrasound, beaconDetected);
        String frequencies = frequenciesBuilder.toString();
        bandActivity.set(ROW_FREQUENCIES, frequencies);
        //Log.i(TAG, frequencies);
        frequencyListAdapter.notifyDataSetChanged();
    }

    private void setLedState(boolean ledState1, boolean ledState2) {
        if (currentLedState[0] != ledState1 || currentLedState[1] != ledState2) {
            currentLedState[0] = ledState1;
            currentLedState[1] = ledState2;
            Log.i(TAG, "LEDs = " + ledState1 + ", " + ledState2);
            sendLedState();
        }
    }

    private void sendLedState() {
        byte[] bytes = new byte[2];
        if (currentLedState[0]) {
            bytes[0] = 0x01;
        }
        if (currentLedState[1]) {
            bytes[1] = 0x01;
        }
        rfduinoSend(bytes);
    }

    @Override
    protected void handleRfduinoExtraData(byte[] byteArrayExtra) {
        Log.i(TAG, "recieved data from rfduino: " + Arrays.toString(byteArrayExtra));
    }

    @Override
    protected void showBluetoothStatus(String status) {
        Log.i(TAG, "bluetooth status: " + status);
        bandActivity.set(ROW_BLE_STATUS, status);
        frequencyListAdapter.notifyDataSetChanged();
    }

    @Override
    protected void acceptDeviceInfo(BluetoothDevice device, int rssi, String description) {
        Log.i(TAG, "scan device: " + description);
        rfduinoConnect();
    }
}

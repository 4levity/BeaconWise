package forlevity.net.beaconwise;

import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements Runnable {

    private static final int SAMPLE_RATE_SAMPLES_PER_SEC = 96000; // or try 48000, 44100
    private static final int WINDOW_SIZE = 2048;

    private static final long UPDATE_INTERVAL_MILLIS = 100;
    //  (long) (1000.0 / (SAMPLE_RATE_SAMPLES_PER_SEC / WINDOW_SIZE));

    // bands / frequency histogram buckets: 17500, 17750, 18000, 18250... 22500
    private static final int MINIMUM_FREQUENCY = 17250;
    private static final int STEP_FREQUENCY = 500;
    private static final int NUM_BANDS = 10;

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int LAST_SIGNALS_BUFFER_SIZE = 32;

    private Handler handler;
    private Thread microphoneListenerThread;
    private UltrasoundDetector ultrasoundDetector;
    private RingBuffer<boolean[]> signals;
    private List<String> bandActivity;
    private ArrayAdapter frequencyListAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "starting!");
        super.onCreate(savedInstanceState);
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
    }

    @Override
    public void run() {
        updateGraph();

        // update again in a few ms
        handler.postDelayed(this, UPDATE_INTERVAL_MILLIS);
    }

    private void updateGraph() {
        StringBuilder frequenciesBuilder = new StringBuilder("frequencies: ");
        for (int band = 0; band < NUM_BANDS; band++) {
            StringBuilder activity = new StringBuilder(
                    String.format("%d ",ultrasoundDetector.frequency(band)));
            boolean[] signal = null;
            for (int ix = 0; ix < signals.count(); ix++) {
                signal = signals.get(ix);
                if (signal[band]) {
                    activity.append("*");
                } else {
                    activity.append(" ");
                }
            }
            if (signal != null && signal[band]) {
                frequenciesBuilder.append(String.format("%d ", ultrasoundDetector.frequency(band)));
            }

            this.bandActivity.set(band, activity.toString());
        }
        String frequencies = frequenciesBuilder.toString();
        bandActivity.set(NUM_BANDS, frequencies);
        //Log.i(TAG, frequencies);
        frequencyListAdapter.notifyDataSetChanged();
    }
}

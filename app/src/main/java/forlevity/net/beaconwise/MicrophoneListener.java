package forlevity.net.beaconwise;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

/**
 * Created by ivan on 10/14/17.
 */

public class MicrophoneListener implements Runnable {

    private static final String TAG = MicrophoneListener.class.getSimpleName();

    private static final int CHANNELS = 1;
    private static final int ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    private static final int SOURCE = MediaRecorder.AudioSource.MIC;
    private static final int BUFFER_SIZE = 32;

    private final ConsumerShort consumer;
    private final int samplesPerSec;

    public MicrophoneListener(int samplesPerSec, ConsumerShort consumer) {
        this.consumer = consumer;
        this.samplesPerSec = samplesPerSec;
    }

    @Override
    public void run() {
        int bufferSize = AudioRecord.getMinBufferSize(samplesPerSec, CHANNELS, ENCODING);
        AudioRecord recorder = new AudioRecord(SOURCE, samplesPerSec, CHANNELS, ENCODING,
                bufferSize);
        recorder.startRecording();
        Log.i(TAG, "MicrophoneListener has started");
        while (true) {
            short[] buffer = new short[BUFFER_SIZE];
            int bytes = recorder.read(buffer, 0, BUFFER_SIZE);
            if (bytes < 0) {
                return;
            }
            for (int ix = 0; ix < bytes; ix++) {
                consumer.accept(buffer[ix]);
            }
        }
    }
}

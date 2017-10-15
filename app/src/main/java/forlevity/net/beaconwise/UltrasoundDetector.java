package forlevity.net.beaconwise;

import org.jtransforms.fft.DoubleFFT_1D;

/**
 * Created by ivan on 10/14/17.
 */

class UltrasoundDetector implements ConsumerShort {

    // amplitude threshold by experimentation, depends on background noise, mic sensitivity, ?
    private static final double MINIMUM_MAGNITUDE = 5.1;
    private final double[] hammingWindow;
    private final long samplesPerSecond;
    private final short[] samples;
    private final int windowSizeSamples;
    private final int minimumFrequency;
    private final int stepFrequency;
    private final int numBands;
    private final RingBuffer<boolean[]> signals;
    private final double signalThreshold;

    private int currentPosition;
    private double[] lastFrequencyHistogram;

    UltrasoundDetector(long samplesPerSecond, int windowSizeSamples,
                       int minimumFrequency, int stepFrequency, int numBands,
                       RingBuffer<boolean[]> signals) {
        this.samplesPerSecond = samplesPerSecond;
        this.windowSizeSamples = windowSizeSamples;
        this.minimumFrequency = minimumFrequency;
        this.stepFrequency = stepFrequency;
        this.numBands = numBands;
        this.signals = signals;
        this.signalThreshold = MINIMUM_MAGNITUDE * windowSizeSamples;

        samples = new short[windowSizeSamples];
        currentPosition = 0;
        // https://en.wikipedia.org/wiki/Window_function#Hamming_window
        hammingWindow = new double[windowSizeSamples];
        for(int i = 0; i < windowSizeSamples; ++i) {
            hammingWindow[i] = .54 - .46 * Math.cos(2 * Math.PI * i / (windowSizeSamples - 1.0));
        }
    }

    @Override
    public void accept(Short sample) {
        samples[currentPosition++] = sample;
        if (currentPosition == windowSizeSamples) {
            currentPosition = 0;
            lastFrequencyHistogram = frequencyHistogram();
            signals.add(signals(lastFrequencyHistogram));
        }
    }

    private boolean[] signals(double[] histogram) {
        boolean[] signals = new boolean[numBands];
        for (int band = 0; band < numBands; band++) {
            signals[band] = histogram[band] > signalThreshold;
        }
        return signals;
    }

    public double[] getLastFrequencyHistogram() {
        return lastFrequencyHistogram;
    }

    private double[] frequencyHistogram() {
        DoubleFFT_1D fft = new DoubleFFT_1D(windowSizeSamples);
        double processedSamples[] = applyHammWindow(samples);
        fft.realForward(processedSamples);
        double[] bins = new double[numBands];
        for(int ix = 1; ix < windowSizeSamples / 2; ix++) {
            double frequency = samplesPerSecond * ix / windowSizeSamples;
            int bin = band(frequency);
            if (bin >= 0 && bin < numBands) {
                double re = processedSamples[2*ix];
                double im = processedSamples[2*ix+1];
                double magnitude = Math.sqrt(re * re + im * im);
                bins[bin] += magnitude;
            }
        }
        return bins;
    }

    private double[] applyHammWindow(short[] input) {
        double[] res = new double[windowSizeSamples];
        for(int i = 0; i < windowSizeSamples; ++i) {
            res[i] = (double)input[i] * hammingWindow[i];
        }
        return res;
    }

    int band(double frequency) {
        return (int) ((frequency - minimumFrequency) / stepFrequency);
    }

    int frequency(int band) {
        return minimumFrequency + (band * stepFrequency) + (stepFrequency / 2);
    }
}

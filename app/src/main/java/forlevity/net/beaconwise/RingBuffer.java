package forlevity.net.beaconwise;

/**
 * Created by ivan on 10/15/17.
 */

public class RingBuffer<T> {

    private final T[] buffer;
    private int count;
    private int next;

    public RingBuffer(T[] buffer) {
        this.buffer = buffer;
    }

    public void add(T item) {
        buffer[next] = item;
        next = (next + 1) % buffer.length;
        if (count < buffer.length) {
            count++;
        }
    }

    public T get(int ix) {
        return buffer[(next + ix + (buffer.length - count)) % buffer.length];
    }

    public int count() {
        return count;
    }
}

package forlevity.net.beaconwise;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class TestRingBuffer {

    @Test
    public void ringBufferWorks() throws Exception {
        RingBuffer<Integer> buffer = new RingBuffer<>(new Integer[5]);
        buffer.add(1);
        buffer.add(2);
        assertEquals(1, (int)buffer.get(0));
        assertEquals(2, (int)buffer.get(1));
        buffer.add(3);
        assertEquals(3, buffer.count());
        buffer.add(4);
        buffer.add(5);
        buffer.add(6);
        assertEquals(2, (int)buffer.get(0));
        assertEquals(3, (int)buffer.get(1));
        assertEquals(4, (int)buffer.get(2));
        assertEquals(5, (int)buffer.get(3));
        assertEquals(6, (int)buffer.get(4));
        buffer.add(7);
        assertEquals(5, buffer.count());
        assertEquals(3, (int)buffer.get(0));
        assertEquals(4, (int)buffer.get(1));
        assertEquals(5, (int)buffer.get(2));
        assertEquals(6, (int)buffer.get(3));
        assertEquals(7, (int)buffer.get(4));
    }
}
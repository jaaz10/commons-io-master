package org.apache.commons.io.input.buffer;

import static org.junit.Assert.*;
import org.junit.Test;

public class CircularByteBufferTest {

    @Test
    public void testBasicOperationsAndBoundaries() {
        // Test initialization
        CircularByteBuffer buffer = new CircularByteBuffer(4);
        assertTrue(buffer.getCurrentNumberOfBytes() == 0);
        assertTrue(buffer.hasSpace());
        assertFalse(buffer.hasBytes());

        // Test basic operations
        buffer.add((byte) 1);
        assertEquals(1, buffer.getCurrentNumberOfBytes());
        buffer.add((byte) 2);
        assertEquals(2, buffer.getCurrentNumberOfBytes());

        byte value = buffer.read();
        assertEquals(1, value);
        assertEquals(1, buffer.getCurrentNumberOfBytes());

        value = buffer.read();
        assertEquals(2, value);
        assertEquals(0, buffer.getCurrentNumberOfBytes());
    }

    @Test
    public void testCircularWraparoundBehavior() {
        CircularByteBuffer buffer = new CircularByteBuffer(3);

        // Fill buffer
        buffer.add((byte) 1);
        buffer.add((byte) 2);
        buffer.add((byte) 3);

        // Read two bytes to create wraparound condition
        assertEquals(1, buffer.read());
        assertEquals(2, buffer.read());

        // Add bytes at wraparound point
        buffer.add((byte) 4);
        buffer.add((byte) 5);

        // Verify wraparound behavior
        assertEquals(3, buffer.read());
        assertEquals(4, buffer.read());
        assertEquals(5, buffer.read());
        assertEquals(0, buffer.getCurrentNumberOfBytes());
    }

    @Test
    public void testErrorConditions() {
        // Test zero size
        try {
            new CircularByteBuffer(0);
            fail("Should throw IllegalArgumentException");
        } catch (IllegalArgumentException expected) {}

        // Test negative size
        try {
            new CircularByteBuffer(-1);
            fail("Should throw IllegalArgumentException");
        } catch (IllegalArgumentException expected) {}

        // Test read from empty buffer
        CircularByteBuffer buffer = new CircularByteBuffer(2);
        try {
            buffer.read();
            fail("Should throw IllegalStateException");
        } catch (IllegalStateException expected) {}

        // Test add to full buffer
        buffer.add((byte) 1);
        buffer.add((byte) 2);
        try {
            buffer.add((byte) 3);
            fail("Should throw IllegalStateException");
        } catch (IllegalStateException expected) {}
    }

    @Test
    public void testMultipleReadWriteCycles() {
        CircularByteBuffer buffer = new CircularByteBuffer(3);

        for (int cycle = 0; cycle < 3; cycle++) {
            assertTrue(buffer.getCurrentNumberOfBytes() == 0);
            assertEquals(3, buffer.getSpace());

            buffer.add((byte) (cycle * 3 + 1));
            buffer.add((byte) (cycle * 3 + 2));
            buffer.add((byte) (cycle * 3 + 3));

            assertEquals(3, buffer.getCurrentNumberOfBytes());
            assertEquals(0, buffer.getSpace());
            assertEquals(cycle * 3 + 1, buffer.read());
            assertEquals(cycle * 3 + 2, buffer.read());
            assertEquals(cycle * 3 + 3, buffer.read());
        }
    }

    @Test
    public void testSpaceManagement() {
        CircularByteBuffer buffer = new CircularByteBuffer(3);
        assertEquals(3, buffer.getSpace());
        assertTrue(buffer.hasSpace());
        assertTrue(buffer.hasSpace(3));
        assertFalse(buffer.hasSpace(4));

        buffer.add((byte) 1);
        assertEquals(2, buffer.getSpace());
        assertTrue(buffer.hasSpace(2));
        assertFalse(buffer.hasSpace(3));

        buffer.add((byte) 2);
        assertEquals(1, buffer.getSpace());

        buffer.add((byte) 3);
        assertEquals(0, buffer.getSpace());
        assertFalse(buffer.hasSpace());
    }

    @Test
    public void testBulkOperations() {
        CircularByteBuffer buffer = new CircularByteBuffer(4);
        byte[] writeData = new byte[]{1, 2, 3};
        byte[] readData = new byte[3];

        // Test bulk write
        buffer.add(writeData, 0, 3);
        assertEquals(3, buffer.getCurrentNumberOfBytes());

        // Test bulk read
        buffer.read(readData, 0, 3);
        assertEquals(0, buffer.getCurrentNumberOfBytes());
        assertArrayEquals(writeData, readData);

        // Test invalid offset
        try {
            buffer.read(readData, 3, 1);
            fail("Should throw IllegalArgumentException");
        } catch (IllegalArgumentException expected) {}
    }

    @Test
    public void testPeekOperations() {
        CircularByteBuffer buffer = new CircularByteBuffer(3);
        byte[] peekData = new byte[]{1, 2};

        // Add data to buffer
        buffer.add((byte) 1);
        buffer.add((byte) 2);

        // Test exact match peek
        assertTrue(buffer.peek(peekData, 0, 2));
        assertEquals(2, buffer.getCurrentNumberOfBytes()); // Verify peek didn't consume data

        // Test partial peek
        byte[] partialPeek = new byte[]{1};
        assertFalse(buffer.peek(partialPeek, 0, 1)); // Should fail as we require exact match

        // Test wraparound peek
        buffer.read(); // Read one byte
        buffer.add((byte) 3);

        byte[] wraparoundPeek = new byte[]{2, 3};
        assertTrue(buffer.peek(wraparoundPeek, 0, 2));

        // Verify original data wasn't modified
        assertEquals(2, buffer.read());
        assertEquals(3, buffer.read());
        assertEquals(0, buffer.getCurrentNumberOfBytes());
    }

    @Test
    public void testStateTransitions() {
        CircularByteBuffer buffer = new CircularByteBuffer(2);

        // Test empty to full transitions
        assertTrue(buffer.hasSpace(2));
        buffer.add((byte) 1);
        assertTrue(buffer.hasSpace(1));
        buffer.add((byte) 2);
        assertFalse(buffer.hasSpace());

        // Test full to empty transitions
        assertEquals(1, buffer.read());
        assertTrue(buffer.hasSpace(1));
        assertEquals(2, buffer.read());
        assertTrue(buffer.hasSpace(2));

        // Test clear operation
        buffer.add((byte) 1);
        buffer.clear();
        assertEquals(0, buffer.getCurrentNumberOfBytes());
        assertTrue(buffer.hasSpace(2));
    }

    @Test
    public void testComplexScenarios() {
        CircularByteBuffer buffer = new CircularByteBuffer(3);

        // Mix of operations
        buffer.add((byte) 1);
        buffer.add((byte) 2);
        assertEquals(1, buffer.read());
        buffer.add((byte) 3);
        buffer.add((byte) 4);

        // Verify sequence
        assertEquals(2, buffer.read());
        assertEquals(3, buffer.read());
        assertEquals(4, buffer.read());

        // Test wraparound with partial buffer
        buffer.add((byte) 5);
        buffer.add((byte) 6);
        assertEquals(5, buffer.read());
        buffer.add((byte) 7);
        assertEquals(6, buffer.read());
        assertEquals(7, buffer.read());
    }

    @Test
    public void testEdgeCases() {
        // Test single byte buffer
        CircularByteBuffer singleBuffer = new CircularByteBuffer(1);
        singleBuffer.add((byte) 42);
        assertEquals(42, singleBuffer.read());

        // Test near-capacity operations
        CircularByteBuffer buffer = new CircularByteBuffer(3);
        buffer.add((byte) 1);
        buffer.add((byte) 2);
        assertEquals(1, buffer.read());
        buffer.add((byte) 3);
        buffer.add((byte) 4);

        assertEquals(2, buffer.read());
        assertEquals(3, buffer.read());
        assertEquals(4, buffer.read());
    }
}
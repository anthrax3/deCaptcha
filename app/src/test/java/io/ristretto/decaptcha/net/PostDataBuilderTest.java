package io.ristretto.decaptcha.net;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


public class PostDataBuilderTest {

    @Test
    public void addInt() throws Exception {
        PostDataBuilder builder = new PostDataBuilder();
        builder.add("test", 5);
        builder.add("negative", -100);
        builder.add("test", 10);
        builder.add("pi", 10000);
        builder.add("ünicöde", 10);
        String result = builder.toString();
        assertTrue(result.contains("test=5"));
        assertTrue(result.contains("test=10"));
        assertTrue(result.contains("pi=10000"));
        assertTrue(result.contains("negative=-100"));
        assertTrue(result.contains("ünicöde=10"));
    }

    @Test
    public void addLong() throws Exception {
        PostDataBuilder builder = new PostDataBuilder();
        builder.add("test", 5L);
        builder.add("test", 10L);
        builder.add("pi", 10000L);
        String result = builder.toString();
        assertEquals("test=5&test=10&pi=10000", result);
    }

    @Test
    public void addLongs() throws Exception {
        long[] values = {1000000000, 1, 2,3,4,5,6};
        PostDataBuilder builder = new PostDataBuilder();
        builder.add("test", values);
        assertEquals("test=1000000000&test=1&test=2&test=3&test=4&test=5&test=6", builder.toString());
    }

    @Test
    public void addIntegers() throws Exception {
        int[] values = {1000000000, 1, 2,3,4,5,6};
        PostDataBuilder builder = new PostDataBuilder();
        builder.add("test", values);
        assertEquals("test=1000000000&test=1&test=2&test=3&test=4&test=5&test=6", builder.toString());
    }

}
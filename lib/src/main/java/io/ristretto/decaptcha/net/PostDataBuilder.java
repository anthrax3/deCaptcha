package io.ristretto.decaptcha.net;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class PostDataBuilder {
    private final StringBuilder stringBuilder;

    public PostDataBuilder() {
        this.stringBuilder = new StringBuilder();
    }

    private void addAmpIsNotEmpty() {
        if(stringBuilder.length() > 0) {
            stringBuilder.append("&");
        }
    }

    @Contract("null, _ -> fail")
    public PostDataBuilder add(String name, int value) {
        return add(name, Integer.toString(value));
    }

    @Contract("null, _ -> fail")
    public PostDataBuilder add(String name, long value) {
        return add(name, Long.toString(value));
    }

    @Contract("null, _ -> fail; _, null -> fail; !null, !null -> !null")
    public PostDataBuilder add(String name, String value) {
        if(name == null) throw new NullPointerException("name is null");
        if(name.isEmpty()) throw new IllegalArgumentException("name is empty");
        if(value == null) throw new NullPointerException("values is null");
        addAmpIsNotEmpty();
        try {
            stringBuilder.append(URLEncoder.encode(name, "UTF-8"))
                    .append("=")
                    .append(URLEncoder.encode(value, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        return this;
    }

    @Contract("null, _ -> fail; _, null -> fail; !null, !null -> !null")
    public PostDataBuilder addIntegers(String name, Iterable<Integer> values) {
        if(name == null) throw new NullPointerException("name is null");
        if(values == null) throw new NullPointerException("values is null");
        for(int value: values) {
            add(name, value);
        }
        return this;
    }

    @Contract("null, _ -> fail; _, null -> fail; !null, !null -> !null")
    public PostDataBuilder add(String name, int[] values) {
        if(name == null) throw new NullPointerException("name is null");
        if(values == null) throw new NullPointerException("values is null");
        for(int value: values) {
            add(name, value);
        }
        return this;
    }

    @Contract("null, _ -> fail; _, null -> fail; !null, !null -> !null")
    public PostDataBuilder add(String name, long[] values) {
        if(name == null) throw new NullPointerException("name is null");
        if(values == null) throw new NullPointerException("values is null");
        for(long value: values) {
            add(name, value);
        }
        return this;
    }

    @Contract("null, _ -> fail; _, null -> fail; !null, !null -> !null")
    public PostDataBuilder add(String name, Iterable<String> values) {
        if(name == null) throw new NullPointerException("name is null");
        if(values == null) throw new NullPointerException("values is null");
        for(String value: values) {
            add(name, value);
        }
        return this;
    }

    @Override
    @NotNull
    public String toString() {
        return stringBuilder.toString();
    }

    @NotNull
    public byte[] getBytes() {
        try {
            return stringBuilder.toString().getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
}



package com.masyaman.datapack.serializers.primitives;

import com.masyaman.datapack.serializers.Deserializer;

import java.io.IOException;
import java.io.InputStream;

public class UnsignedLongReader implements Deserializer<Long> {

    private InputStream is;

    public UnsignedLongReader(InputStream is) {
        this.is = is;
    }

    @Override
    public Long deserialize() throws IOException {
        int b = is.read();
        if (b == 0x7F) {
            return null; // -64 in single byte representation
        }
        int bytesToRead = 0;
        while ((b & (0x80 >> bytesToRead)) != 0) {
            bytesToRead++;
        }
        long result = b & ~(0xFFFFFF80 >> bytesToRead);
        for (int i = 0; i < bytesToRead; i++) {
            result = (result << 8) | is.read();
        }
        return result;
    }
}

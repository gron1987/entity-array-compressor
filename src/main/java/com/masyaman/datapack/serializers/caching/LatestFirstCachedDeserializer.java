package com.masyaman.datapack.serializers.caching;

import com.masyaman.datapack.cache.ObjectIdCache;
import com.masyaman.datapack.cache.ObjectIdCacheRingBuffer;
import com.masyaman.datapack.cache.ObjectIdCacheRingTree;
import com.masyaman.datapack.serializers.Deserializer;
import com.masyaman.datapack.streams.DataReader;

import java.io.IOException;
import java.util.LinkedList;

/**
 * Caching wrapper for Deserializer.
 * For data format see {@link LatestFirstCachedSerializer}
 */
public class LatestFirstCachedDeserializer<E> implements Deserializer<E> {

    private DataReader is;
    private Deserializer<E> deserializer;
    private int cacheSize;

    private ObjectIdCache<E> cache;

    public LatestFirstCachedDeserializer(DataReader is, Deserializer<E> deserializer) throws IOException {
        this.is = is;
        this.deserializer = deserializer;
        cacheSize = is.readUnsignedLong().intValue();
        cache = new ObjectIdCacheRingTree<>(cacheSize);
    }

    @Override
    public E deserialize() throws IOException {
        Long id = is.readUnsignedLong();
        if (id == null) {
            return null;
        }
        E value;
        if (id <= 0) {
            value = deserializer.deserialize();
        } else {
            value = cache.removePosition(id.intValue() - 1);
        }
        cache.addHead(value);
        return value;
    }
}

package com.masyaman.datapack.streams;

import com.masyaman.datapack.reflection.TypeDescriptor;
import com.masyaman.datapack.serializers.Deserializer;
import com.masyaman.datapack.serializers.primitives.SignedLongReader;
import com.masyaman.datapack.serializers.primitives.StringReader;
import com.masyaman.datapack.serializers.primitives.UnsignedLongReader;

import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public abstract class DataReader implements ObjectReader {

    protected PushbackInputStream is;

    private Deserializer<Long> signedLongDeserializer;
    private Deserializer<Long> unsignedLongDeserializer;
    private Deserializer<String> stringDeserializer;

    public DataReader(InputStream inputStream) throws IOException {
        this.is = new PushbackInputStream(inputStream);

        signedLongDeserializer = new SignedLongReader(is);
        unsignedLongDeserializer = new UnsignedLongReader(is);
        stringDeserializer = new StringReader(is, unsignedLongDeserializer);
    }

    @Override
    public void close() throws IOException {
        is.close();
    }

    @Override
    public boolean hasObjects() throws IOException {
        int read = is.read();
        if (read >= 0) {
            is.unread(read);
            return true;
        } else {
            return false;
        }
    }

    public byte readByte() throws IOException {
        return (byte) is.read();
    }

    public int readUnsignedByte() throws IOException {
        return is.read();
    }
    
    public Long readSignedLong() throws IOException {
        return signedLongDeserializer.deserialize();
    }

    public Long readUnsignedLong() throws IOException {
        return unsignedLongDeserializer.deserialize();
    }

    public String readString() throws IOException {
        return stringDeserializer.deserialize();
    }

    public Object readObject() throws IOException {
        return readObject(new TypeDescriptor<>(Object.class));
    }

    @Override
    public <T> Iterable<T> asIterable(Class<T> clazz) {
        TypeDescriptor<T> typeDescriptor = new TypeDescriptor<T>(clazz);
        return new Iterable<T>() {
            @Override
            public Iterator<T> iterator() {
                return new Iterator<T>() {
                    @Override
                    public boolean hasNext() {
                        try {
                            return hasObjects();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }

                    @Override
                    public T next() {
                        try {
                            return readObject(typeDescriptor);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                };
            }
        };
    }

    public abstract <T> T readObject(TypeDescriptor<T> type) throws IOException;

    public abstract ClassManager getClassManager();

    public abstract SerializationFactoryLookup getSerializationFactoryLookup();

    public abstract <E> Deserializer<E> createAndRegisterDeserializer(TypeDescriptor<E> type) throws IOException;


    public static class Wrapper extends DataReader {
        DataReader parent;

        public Wrapper(InputStream is, DataReader parent) throws IOException {
            super(is);
            this.parent = parent;
        }

        @Override
        public <T> T readObject(TypeDescriptor<T> type) throws IOException {
            return parent.readObject(type);
        }

        @Override
        public ClassManager getClassManager() {
            return parent.getClassManager();
        }

        @Override
        public SerializationFactoryLookup getSerializationFactoryLookup() {
            return parent.getSerializationFactoryLookup();
        }

        @Override
        public <E> Deserializer<E> createAndRegisterDeserializer(TypeDescriptor<E> type) throws IOException {
            return parent.createAndRegisterDeserializer(type);
        }
    }


    public static abstract class Abstract extends DataReader {

        protected ClassManager classManager;
        protected SerializationFactoryLookup serializationFactoryLookup;

        protected List<Deserializer> registeredDeserializers = new ArrayList<>();

        public Abstract(InputStream is, ClassManager classManager, SerializationFactoryLookup serializationFactoryLookup) throws IOException {
            super(is);
            this.classManager = classManager;
            this.serializationFactoryLookup = serializationFactoryLookup;
        }

        public <T> T readObject(TypeDescriptor<T> type) throws IOException {
            Long id = readUnsignedLong();
            if (id == null) {
                return null;
            }
            if (id <= 0) {
                return readAndRegisterDeserializer(type).deserialize();
            } else {
                return (T) registeredDeserializers.get(id.intValue() - 1).deserialize();
            }
        }

        public ClassManager getClassManager() {
            return classManager;
        }

        public SerializationFactoryLookup getSerializationFactoryLookup() {
            return serializationFactoryLookup;
        }

        public <E> Deserializer<E> createAndRegisterDeserializer(TypeDescriptor<E> type) throws IOException {
            Long id = readUnsignedLong();
            if (id == null) {
                return readDeserializer(type);
            } else if (id <= 0) {
                return readAndRegisterDeserializer(type);
            } else {
                return registeredDeserializers.get(id.intValue() - 1);
            }
        }

        private <E> Deserializer<E> readAndRegisterDeserializer(TypeDescriptor<E> type) throws IOException {
            int index = registeredDeserializers.size();
            registeredDeserializers.add(null);
            Deserializer<E> deserializer = readDeserializer(type);
            registeredDeserializers.set(index, deserializer);
            return deserializer;
        }

        protected abstract <E> Deserializer<E> readDeserializer(TypeDescriptor<E> type) throws IOException;
    }
}

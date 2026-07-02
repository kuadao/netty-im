package com.kang.nettyim.protocol;

public interface Serializer {

    Serializer DEFAULT = new JSONSerializer();

    byte getSerializerAlgorithm();

    byte[] serialize(Object object);

    <T> T deserialize(byte[] bytes, Class<T> clazz);
}

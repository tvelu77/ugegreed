package fr.uge.ugegreed.records;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

public record Id(InetSocketAddress socketAddress) implements Encoder {
    @Override
    public boolean encode(ByteBuffer buffer) {
        var addressBytes = socketAddress.getAddress().getAddress();
        var addressBytesSize = addressBytes.length;
        if(buffer.remaining() < Byte.BYTES + addressBytesSize + Integer.BYTES) {
            return false;
        }

        if(addressBytesSize == 4) {
            buffer.put((byte) 4);
        } else {
            buffer.put((byte) 6);
        }

        buffer.put(addressBytes);
        var port = socketAddress.getPort();
        buffer.putInt(port);
        return true;
    }

    @Override
    public int size() {
        var addressBytes = socketAddress.getAddress().getAddress();
        return Byte.BYTES + addressBytes.length + Integer.BYTES;
    }

    @Override
    public String toString() {
        return socketAddress.getAddress() + " : " + socketAddress.getPort();
    }
}

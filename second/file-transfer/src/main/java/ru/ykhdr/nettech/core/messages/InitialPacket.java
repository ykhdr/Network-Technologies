package ru.ykhdr.nettech.core.messages;

public record InitialPacket(short titleSize, String fileName, long dataSize) {
}

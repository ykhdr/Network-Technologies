package ru.ykhdr.nettech.client.cli;

import java.net.InetAddress;

public record ClientInfo(InetAddress serverAddress, int serverPort, String filePath) {
}

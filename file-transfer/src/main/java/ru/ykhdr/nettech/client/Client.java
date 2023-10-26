package ru.ykhdr.nettech.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.ykhdr.nettech.client.cli.ClientInfo;

import java.io.IOException;
import java.net.Socket;

@Slf4j
@RequiredArgsConstructor
public class Client {

    private final ClientInfo clientInfo;

    public void connectToServer() {
        try (Socket socket = new Socket(clientInfo.serverAddress(), clientInfo.serverPort())) {
            log.info("Start ");
            FileTransfer fileTransfer = new FileTransfer(socket, clientInfo.filePath());
            fileTransfer.sendFile();

        } catch (IOException e) {
            log.error("Socket error", e);
        }
    }
}

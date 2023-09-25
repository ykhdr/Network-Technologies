package ru.ykhdr.nettech.server;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.ykhdr.nettech.core.messages.InitialPacket;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Arrays;

@Slf4j
@RequiredArgsConstructor
public class ClientHandler implements Runnable {
    private final Socket clientSocket;
    private static final int BUFFER_SIZE = 1000;
    private long allBytesRead;

    @Override
    public void run() {
        final ByteBuffer byteBuffer = ByteBuffer.allocate(BUFFER_SIZE);
        int bytesRead;
        try (clientSocket;
             DataInputStream dis = new DataInputStream(clientSocket.getInputStream());
             DataOutputStream dos = new DataOutputStream(clientSocket.getOutputStream());) {

            bytesRead = dis.read(byteBuffer.array());
            InitialPacket initialPacket = collectInitialPacket(byteBuffer);

            System.out.println(initialPacket.fileName());
            System.out.println(initialPacket.titleSize());
            System.out.println(initialPacket.dataSize());

        } catch (IOException e) {
            log.error("Client Handler of client " + clientSocket.getInetAddress().getHostAddress() + " error", e);
        }
    }

    private InitialPacket collectInitialPacket(ByteBuffer byteBuffer){
        short titleSize = byteBuffer.getShort();
        byte[] fileName = new byte[titleSize];
        byteBuffer.get(fileName,2,titleSize);
        long dataSize = byteBuffer.getLong();

        return new InitialPacket(titleSize,new String(fileName),dataSize);
    }
}

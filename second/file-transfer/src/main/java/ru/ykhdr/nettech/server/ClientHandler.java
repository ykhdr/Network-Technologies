package ru.ykhdr.nettech.server;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.ykhdr.nettech.core.messages.InitialPacket;

import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

@Slf4j
@RequiredArgsConstructor
public class ClientHandler implements Runnable {
    private final Socket clientSocket;
    private static final int BUFFER_SIZE = 1000;
    private static final String UPLOAD_DIR = "uploads";
    private long allBytesRead;

    @Override
    public void run() {
        final ByteBuffer byteBuffer = ByteBuffer.allocate(BUFFER_SIZE);
        log.info("Start receive file from Client " + clientSocket.getInetAddress().getHostAddress());
        try (clientSocket;
             BufferedInputStream dis = new BufferedInputStream(clientSocket.getInputStream());
             DataOutputStream dos = new DataOutputStream(clientSocket.getOutputStream())) {

            dis.read(byteBuffer.array());
            // Чтение данных о файле
            InitialPacket initialPacket = collectInitialPacket(byteBuffer);
            String fileName = initialPacket.fileName();
            long fileSize = initialPacket.dataSize();

            System.out.println(fileName);

            Path uploadDir = Path.of(UPLOAD_DIR);
            if (!Files.exists(uploadDir)) {
                try {
                    Files.createDirectories(uploadDir);
                } catch (IOException e){
                    log.error("Uploads dir creation error",e);
                }
            }

            Path filePath = uploadDir.resolve(fileName);

            try{
                Files.createFile(filePath);
                System.out.println("File \"" + fileName +"\" created");
            } catch (IOException e){
                log.error("Client File creation error",e);
//                return;
            }
            byte[] fileBuffer = new byte[BUFFER_SIZE];

            // Создание файла для записи данных
            try (BufferedOutputStream fos = new BufferedOutputStream(Files.newOutputStream(filePath))) {
                int bytesRead;
                long totalBytesRead = 0;

                // Чтение данных файла и запись их в файл
                while (totalBytesRead < fileSize && (bytesRead = dis.read(fileBuffer)) != -1) {
                    fos.write(fileBuffer, 0, bytesRead);
                    totalBytesRead += bytesRead;
                }
            } catch (IOException e) {
                log.error("Error writing file", e);
            }

            log.info("Received file from Client " + clientSocket.getInetAddress().getHostAddress());
        } catch (IOException e) {
            log.error("Client Handler of client " + clientSocket.getInetAddress().getHostAddress() + " error", e);
        }

        log.info("Client " + clientSocket.getInetAddress().getHostAddress() + "sent all data");
    }

    private InitialPacket collectInitialPacket(ByteBuffer byteBuffer){
        short titleSize = byteBuffer.getShort();
        byte[] fileName = new byte[titleSize];
        byteBuffer.get(fileName,0,titleSize);
        long dataSize = byteBuffer.getLong();

        return new InitialPacket(titleSize,new String(fileName),dataSize);
    }
}

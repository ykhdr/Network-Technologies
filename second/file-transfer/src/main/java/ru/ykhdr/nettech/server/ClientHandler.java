package ru.ykhdr.nettech.server;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.ykhdr.nettech.core.messages.InitialPacket;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
public class ClientHandler implements Runnable {
    private final Socket clientSocket;
    private static final int BUFFER_SIZE = 655035;
    private static final String UPLOAD_DIR = "uploads";
    @Override
    public void run() {

        log.info("Start receive file from Client " + clientSocket.getInetAddress().getHostAddress());
        try (clientSocket;
             InputStream dis = clientSocket.getInputStream()) {
            byte[] buffer = new byte[BUFFER_SIZE];

            dis.read(buffer);
            InitialPacket initialPacket = collectInitialPacket(buffer);
            Optional<Path> filePathOpt = getFileToWrite(initialPacket);

            if(filePathOpt.isEmpty()){
                log.info("Client socket close");
                return;
            }

            Path filePath = filePathOpt.get();

            try (BufferedOutputStream fos = new BufferedOutputStream(Files.newOutputStream(filePath))) {
                int bytesRead;
                long totalBytesRead = 0;

                while ((bytesRead = dis.read(buffer)) != -1) {
                    fos.write(buffer, 0, bytesRead);
                    System.out.println(new String(Arrays.copyOfRange(buffer, 0, bytesRead)));
                    totalBytesRead += bytesRead;
                }

                System.out.println(totalBytesRead);
            } catch (IOException e) {
                log.error("Error writing file", e);
            }

            log.info("Received file from Client " + clientSocket.getInetAddress().getHostAddress());
        } catch (IOException e) {
            log.error("Client Handler of client " + clientSocket.getInetAddress().getHostAddress() + " error", e);
        }

        log.info("Client " + clientSocket.getInetAddress().getHostAddress() + "sent all data");
    }

    private Optional<Path> getFileToWrite(InitialPacket initialPacket) {
        String fileName = initialPacket.fileName();

        Path uploadDir = Path.of(UPLOAD_DIR);
        if (!Files.exists(uploadDir)) {
            try {
                Files.createDirectories(uploadDir);
            } catch (IOException e) {
                log.error("Uploads dir creation error", e);
                return Optional.empty();
            }
        }

        Path filePath = uploadDir.resolve(fileName);

        if (!Files.exists(filePath)) {
            try {
                Files.createFile(filePath);
            } catch (IOException e) {
                log.error("Client File creation error", e);
                return Optional.empty();
            }
        }

        log.info("File \"" + fileName + "\" created");

        return Optional.of(filePath);
    }

    private InitialPacket collectInitialPacket(byte[] bytes) {
        try (DataInputStream dataInputStream = new DataInputStream(new ByteArrayInputStream(bytes));
             DataOutputStream os = new DataOutputStream(new ByteArrayOutputStream(1))) {
            dataInputStream.transferTo(os);
            dataInputStream.reset();

            short titleSize = dataInputStream.readShort();
            byte[] fileName = new byte[titleSize];
            dataInputStream.readFully(fileName);
            long dataSize = dataInputStream.readLong();
            return new InitialPacket(titleSize, new String(fileName), dataSize, 0);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


    }
}

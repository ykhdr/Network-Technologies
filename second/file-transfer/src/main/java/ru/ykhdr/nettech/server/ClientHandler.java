package ru.ykhdr.nettech.server;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.ykhdr.nettech.core.messages.InitialPacket;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@RequiredArgsConstructor
public class ClientHandler implements Runnable {
    private final Socket clientSocket;
    private static final int BUFFER_SIZE = 655035;
    private static final String UPLOAD_DIR = "uploads";
    private final AtomicLong totalBytesRead = new AtomicLong();

    @Getter
    private Boolean isReceivingStopped = false;

    @Override
    public void run() {
        log.info("Start receive file from Client " + clientSocket.getInetAddress().getHostAddress());

        try (clientSocket;
             InputStream sis = clientSocket.getInputStream()) {
            byte[] buffer = new byte[BUFFER_SIZE];

            sis.read(buffer);

            InitialPacket initialPacket = collectInitialPacket(buffer);
            Optional<Path> filePathOpt = getFileToWrite(initialPacket);

            if (filePathOpt.isEmpty()) {
                log.info("Client socket close");
                return;
            }

            Path filePath = filePathOpt.get();

            SpeedRecorder speedRecorder = new SpeedRecorder(
                    initialPacket.dataSize(),
                    this::getBytesRead,
                    this::getIsReceivingStopped,
                    initialPacket.fileName());

            Thread thread = new Thread(speedRecorder);
            thread.start();
            long startTime = System.currentTimeMillis();

            try (BufferedOutputStream fos = new BufferedOutputStream(Files.newOutputStream(filePath))) {
                int bytesRead;

                while ((bytesRead = sis.read(buffer)) != -1) {
                    fos.write(buffer, 0, bytesRead);
                    fos.flush();
                    totalBytesRead.addAndGet(bytesRead);
                }

            } catch (IOException e) {
                log.error("Error writing file " + filePath.getFileName().toString(), e);
                isReceivingStopped = true;
                return;
            }

            long endTime = System.currentTimeMillis();
            long receivingTime = (endTime - startTime) / 1000;

            log.info("Received file from Client " + clientSocket.getInetAddress().getHostAddress());
            log.info("Average speed:\t\t" + speedRecorder.getAverageSpeed(receivingTime));

            if (totalBytesRead.get() != initialPacket.dataSize()) {
                log.warn("""
                        Total received file's length does not equal to length of the sent file:
                            Received: %d bytes
                            Sent: %d bytes
                        """
                        .formatted(totalBytesRead.get(), initialPacket.dataSize()));
            }

        } catch (IOException e) {
            log.error("Client Handler of client " + clientSocket.getInetAddress().getHostAddress() + " error", e);
        }

        isReceivingStopped = true;
        log.info("Client " + clientSocket.getInetAddress().getHostAddress() + " sent all data. Closing connection");

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

    public long getBytesRead() {
        return totalBytesRead.get();
    }
}

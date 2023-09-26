package ru.ykhdr.nettech.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.ykhdr.nettech.core.messages.InitialPacket;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
public class FileTransfer {
    private final Socket socket;
    private final String filePath;
    private static final short TITLE_MAX_LENGTH = 4096;
    private static final int BUFFER_SIZE = 8192;

    public void sendFile() {
        Optional<Path> fileOptional = getFile(filePath);

        if (fileOptional.isEmpty()) {
            return;
        }

        Path file = fileOptional.get();

        Optional<InitialPacket> initialPacketOpt = fillInitialPacket(file);
        if (initialPacketOpt.isEmpty()) {
            log.error("Canceled sending file");
            return;
        }

        transferFile(file, initialPacketOpt.get());

    }

    private void transferFile(Path file, InitialPacket initialPacket) {
        try (DataOutputStream sos = new DataOutputStream(socket.getOutputStream());
             DataInputStream fis = new DataInputStream(Files.newInputStream(file));
             socket) {
            byte[] initialPacketData = fillByteBufferWithInitialPacketData(initialPacket);
            byte[] buffer = new byte[BUFFER_SIZE];

            int bytesRead;

            sos.write(initialPacketData, 0, initialPacketData.length);
            sos.flush();

            while ((bytesRead = fis.read(buffer)) != -1) {
                sos.write(buffer, 0, bytesRead);
                sos.flush();
            }

            log.info("Successful sent file");
        } catch (IOException e) {
            log.error("Socket stream error", e);
        }
    }

    private byte[] fillByteBufferWithInitialPacketData(InitialPacket initialPacket) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             DataOutputStream dataOutputStream = new DataOutputStream(baos)) {
            dataOutputStream.writeShort(initialPacket.titleSize());
            dataOutputStream.writeBytes(initialPacket.fileName());
            dataOutputStream.writeLong(initialPacket.dataSize());

            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Optional<InitialPacket> fillInitialPacket(Path file) {
        Path fileName = file.getFileName();
        int nameSize = fileName.toString().length();

        if (nameSize > TITLE_MAX_LENGTH) {
            log.warn("File title is longer than allowed (max size - 4096 bytes)");
            return Optional.empty();
        }

        String fileNameStr = fileName.toString();
        long fileSize;

        try {
            fileSize = Files.size(file);
        } catch (IOException e) {
            log.error("File size error", e);
            return Optional.empty();
        }

        int packetSize = 2 + fileNameStr.length() + 8;

        return Optional.of(new InitialPacket((short) nameSize, fileNameStr, fileSize, packetSize));
    }

    private Optional<Path> getFile(String filePath) {
        Path file = Path.of(filePath);
        if (!Files.exists(file) && !Files.isReadable(file)) {
            log.error("File isn't exist or readable");
            return Optional.empty();
        }

        return Optional.of(file);
    }
}


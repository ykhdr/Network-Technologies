package ru.ykhdr.nettech.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.ykhdr.nettech.core.messages.InitialPacket;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
public class FileTransfer {
    private final Socket socket;
    private final String filePath;
    private static final short TITLE_MAX_LENGTH = 4096;
    private static final int TCP_DATA_LENGTH = 655035;
    private final ByteBuffer byteBuffer = ByteBuffer.allocate(TCP_DATA_LENGTH);

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

        fillByteBufferWithInitialPacketData(initialPacketOpt.get());
        transferFile(file);

    }

    private void transferFile(Path file) {
        try (DataOutputStream sos = new DataOutputStream(socket.getOutputStream());
             DataInputStream fis = new DataInputStream(Files.newInputStream(file));
             socket) {

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            int bytesRead;
            // Initial package
            sos.write(byteBuffer.array());
            byteBuffer.clear();

            sos.flush();
            byte[] arr = new byte[8192];

            //TODO проверить работает ли без очистки
            while ((bytesRead = fis.read(arr)) != -1) {
                sos.write(arr, 0, bytesRead);
            }

            socket.close();

            log.info("Successful sent file");
        } catch (IOException e) {
            log.error("Socket stream error", e);
        }
    }

    private void fillByteBufferWithInitialPacketData(InitialPacket initialPacket) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            baos.write((byte) ((initialPacket.titleSize() >> 8) & 0xff));
            baos.write((byte) (initialPacket.titleSize() & 0xff));

            baos.write(initialPacket.fileName().getBytes());

            baos.write((byte) ((initialPacket.dataSize() >> 56) & 0xff));
            baos.write((byte) ((initialPacket.dataSize() >> 48) & 0xff));
            baos.write((byte) ((initialPacket.dataSize() >> 40) & 0xff));
            baos.write((byte) ((initialPacket.dataSize() >> 32) & 0xff));
            baos.write((byte) ((initialPacket.dataSize() >> 24) & 0xff));
            baos.write((byte) ((initialPacket.dataSize() >> 16) & 0xff));
            baos.write((byte) ((initialPacket.dataSize() >> 8) & 0xff));
            baos.write((byte) (initialPacket.dataSize() & 0xff));
            byteBuffer.put(baos.toByteArray());
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

        return Optional.of(new InitialPacket((short) nameSize, fileNameStr, fileSize));
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


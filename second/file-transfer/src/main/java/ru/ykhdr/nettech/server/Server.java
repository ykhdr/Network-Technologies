package ru.ykhdr.nettech.server;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.ykhdr.nettech.server.cli.ServerInfo;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Slf4j
@RequiredArgsConstructor
public class Server {
    private final ServerInfo serverInfo;
    private static final int CLIENTS_THREAD_PULL = 5;
    private final ExecutorService clientThreadPool = Executors.newFixedThreadPool(CLIENTS_THREAD_PULL);

    public void acceptClients() {
        log.info("Server start accepting clients");
        try (ServerSocket serverSocket = new ServerSocket(serverInfo.port())) {
            while (true){
                ClientHandler clientHandler = new ClientHandler(serverSocket.accept());
                log.info("Connected new Client");
                clientThreadPool.execute(clientHandler);
            }
        } catch (IOException e) {
            log.error("Server socket error", e);
        }
    }
}

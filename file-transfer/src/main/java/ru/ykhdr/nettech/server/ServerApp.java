package ru.ykhdr.nettech.server;

import lombok.extern.slf4j.Slf4j;
import ru.ykhdr.nettech.server.cli.ServerCli;
import ru.ykhdr.nettech.server.cli.ServerInfo;

import java.util.Optional;

@Slf4j
public class ServerApp {
    public static void main(String[] args) {
        Optional<ServerInfo> cliInfo = ServerCli.parseCli(args);
        if(cliInfo.isEmpty()){
            log.error("Bad cli arguments");
            return;
        }

        ServerInfo serverInfo = cliInfo.get();

        Server server = new Server(serverInfo);
        server.acceptClients();
    }
}

package ru.ykhdr.nettech.client;

import lombok.extern.slf4j.Slf4j;
import ru.ykhdr.nettech.client.cli.ClientCli;
import ru.ykhdr.nettech.client.cli.ClientInfo;

import java.util.Optional;

@Slf4j
public class ClientApp {
    public static void main(String[] args) {
        Optional<ClientInfo> clientInfoOp = ClientCli.parseCli(args);

        if(clientInfoOp.isEmpty()){
            log.warn("Client shutdown");
            return;
        }
        log.info("Successful arguments passed");

        ClientInfo clientInfo = clientInfoOp.get();

        Client client = new Client(clientInfo);
        client.connectToServer();
    }
}

package ru.ykhdr.nettech.client.cli;

import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Optional;


@Slf4j
public class ClientCli {
    public static Optional<ClientInfo> parseCli(String[] args) {
        if (args.length < 3) {
            log.warn("Too few arguments passed");
            return Optional.empty();
        }

        try {
            InetAddress address = InetAddress.getByName(args[0]);
            int port = Integer.parseInt(args[1]);
            String filePath = args[2];

            return Optional.of(new ClientInfo(address, port, filePath));

        } catch (UnknownHostException e) {
            log.warn("Unknown ip address");
            return Optional.empty();
        } catch (NumberFormatException e) {
            log.warn("Bad port entered");
            return Optional.empty();
        }

    }
}

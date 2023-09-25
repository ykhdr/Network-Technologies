package ru.ykhdr.nettech.server.cli;

import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class ServerCli {

    public static Optional<ServerInfo> parseCli(String[] args){
        if(args.length < 1){
            return Optional.empty();
        }

        try {
           return Optional.of(new ServerInfo(Integer.parseInt(args[0])));
        } catch (NumberFormatException e){
            return Optional.empty();
        }
    }
}

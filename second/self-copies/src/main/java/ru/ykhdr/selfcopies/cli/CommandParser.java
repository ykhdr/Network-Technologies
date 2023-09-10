package ru.ykhdr.selfcopies.cli;

import lombok.AllArgsConstructor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class CommandParser implements Runnable {
    private final BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
    @Override
    public void run() {

        while (true){
            try {
                if (!reader.ready()){
                    break;
                }

                String command = reader.readLine();

            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        }
    }

}


@AllArgsConstructor
enum Command{
    EXIT("exit"),
    SHOW_CONNECTIONS("show"),
    LEAVE("leave"),
    JOIN("join");

    final String cmd;

}
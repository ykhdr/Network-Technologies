package ru.ykhdr.selfcopies;

import ru.ykhdr.selfcopies.multicast.MulticastConfig;
import ru.ykhdr.selfcopies.multicast.MulticastPacketMessage;
import ru.ykhdr.selfcopies.multicast.MulticastPublisher;
import ru.ykhdr.selfcopies.multicast.MulticastReceiver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Optional;

public class Main {
    public static void main(String[] args) throws UnknownHostException {
        if (args.length < 1) {
            System.err.println("Expected address argument");
            return;
        }

        Optional<InetAddress> inetAddressOptional = getInetAddress(args[0]);

        if (inetAddressOptional.isEmpty()) {
            System.out.println("Unknown group address");
            return;
        }

        InetAddress inetAddress = inetAddressOptional.get();

        MulticastPublisher publisher = new MulticastPublisher(inetAddress);
        MulticastReceiver receiver = new MulticastReceiver(inetAddress, publisher);


        receiver.start();
        publisher.sendMessage(MulticastPacketMessage.JOIN);
        Group.getInstance().addAddress(InetAddress.getByName(InetAddress.getLocalHost().getHostAddress()));

        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            try {
                String command = reader.readLine();
                switch (command) {
                    case "exit " -> {
                        MulticastConfig.continueReading = false;
                        publisher.sendMessage(MulticastPacketMessage.EXIT);
                        return;
                    }
                    case "show" -> Group.getInstance().show();
                }

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

    }

    static Optional<InetAddress> getInetAddress(String addressName) {
        try {
            InetAddress inetAddress = InetAddress.getByName(addressName);
            if (inetAddress.isMulticastAddress()) {
                return Optional.of(inetAddress);
            }

        } catch (UnknownHostException ignored) {
        }

        return Optional.empty();
    }
}
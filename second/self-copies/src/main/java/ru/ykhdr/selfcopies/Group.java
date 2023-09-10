package ru.ykhdr.selfcopies;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

public class Group {
    private final List<InetAddress> addresses = new ArrayList<>();
    private static final Group INSTANCE = new Group();

    private Group() {
    }

    public static Group getInstance() {
        return INSTANCE;
    }

    public void addAddress(InetAddress address) {
        if (addresses.contains(address)) {
            return;
        }

        addresses.add(address);
        System.out.println("User join : " + address.getHostName() + "\n");
        show();
    }

    public void deleteAddress(InetAddress address) {
        addresses.remove(address);
        System.out.println("User leave : " + address.getHostName() + "\n");
        show();
    }

    public void show() {
        System.out.println("Current users in group:");
        addresses.forEach(address -> System.out.println("\tUser : " + address.getHostName()));
    }
}

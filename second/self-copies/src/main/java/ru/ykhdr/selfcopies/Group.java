package ru.ykhdr.selfcopies;

import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

@Component
@NoArgsConstructor
public class Group {
    private final List<String> users = new ArrayList<>();

    public void addUser(InetAddress address, int port) {
        String user = address.getHostAddress() + ":" + port;

        if (users.contains(user)) {
            return;
        }

        users.add(user);
        System.out.println("User join : " + user + "\n");
        show();
    }

    public void deleteUser(InetAddress address, int port) {
        String user = address.getHostAddress() + ":" + port;
        users.remove(user);
        System.out.println("User leave : " + user +  "\n");
        show();
    }

    public void show() {
        System.out.println("Current users in group:");
        users.forEach(address -> System.out.println("\tUser : " + address));
        System.out.println();
    }
}

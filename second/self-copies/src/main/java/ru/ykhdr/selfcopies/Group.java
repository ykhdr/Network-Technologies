package ru.ykhdr.selfcopies;

import lombok.NoArgsConstructor;
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@NoArgsConstructor
@Component
@PropertySource("classpath:group.properties")
public class Group {
    private final Set<String> users = new HashSet<>();
    private final Set<String> activeUsers = new HashSet<>();

    public void addUser(InetAddress address, int port) {
        String user = address.getHostAddress() + ":" + port;

//        if (users.contains(user) || activeUsers.contains(user)) {
//            return;
//        }

        synchronized (activeUsers) {
            activeUsers.add(user);
        }
    }

    public void deleteUser(InetAddress address, int port) {
        String user = address.getHostAddress() + ":" + port;
        synchronized (users) {
            users.remove(user);
        }
        synchronized (activeUsers) {
            activeUsers.remove(user);
        }

        System.out.println("User leave : " + user);
        System.out.println();
        show();
    }

    public void show() {
        System.out.println("Current users in group:");
        synchronized (users) {
            users.forEach(user -> System.out.println("\tUser : " + user));
        }
        System.out.println();
    }

    @Scheduled(fixedDelayString = "${group.scheduled.update.fixedDelay}",
            initialDelayString = "${group.scheduled.update.initialDelay}")
    public void updateActiveUsers() {
        List<String> newUsers;
        List<String> leavedUsers;
        synchronized (users) {
            synchronized (activeUsers) {
                newUsers = activeUsers.stream().filter(user -> !users.contains(user)).toList();
                leavedUsers = users.stream().filter(user -> !activeUsers.contains(user)).toList();
            }
            leavedUsers.forEach(users::remove);
            users.addAll(newUsers);
            synchronized (activeUsers) {
                activeUsers.clear();
            }
        }

        if (!leavedUsers.isEmpty()) {
            System.out.println("\nLeaved users:");
            leavedUsers.forEach(user -> System.out.println("\tUser: " + user));
            System.out.println();
        }

        if (!newUsers.isEmpty()) {
            System.out.println("\nNew users in group:");
            newUsers.forEach(user -> System.out.println("\tUser : " + user));
            System.out.println();
        }

        if (!leavedUsers.isEmpty() || !newUsers.isEmpty()) {
            show();
        }
    }
}

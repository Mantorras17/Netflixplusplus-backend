package org.netflixpp.service;

import org.netflixpp.model.User;
import java.util.HashMap;
import java.util.Map;

public class AuthService {
    // Simulação de base de dados em memória (depois substitui por DB real)
    private Map<String, User> users = new HashMap<>();

    public AuthService() {
        // Utilizador de teste
        users.put("admin", new User(1, "admin", "password", "admin@netflixpp.com"));
    }

    public boolean authenticate(String username, String password) {
        User user = users.get(username);
        return user != null && user.getPassword().equals(password);
    }

    public boolean register(User newUser) {
        if (users.containsKey(newUser.getUsername())) {
            return false; // Utilizador já existe
        }

        newUser.setId(users.size() + 1);
        users.put(newUser.getUsername(), newUser);
        return true;
    }

    public User getUserByUsername(String username) {
        return users.get(username);
    }
}
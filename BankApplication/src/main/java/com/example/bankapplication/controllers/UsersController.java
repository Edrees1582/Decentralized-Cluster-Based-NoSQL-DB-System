package com.example.bankapplication.controllers;

import com.example.bankapplication.exceptions.InsufficientBalanceException;
import com.example.bankapplication.models.User;
import com.example.bankapplication.services.Communication;
import jakarta.servlet.http.HttpSession;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;

@Controller
public class UsersController {
    @Autowired
    Environment environment;

    @GetMapping("")
    public String home() {
        return "redirect:/login";
    }

    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }

    @PostMapping("/register")
    public String addUser(@RequestBody String user) {
        ResponseEntity<String> response = Communication.callAPI("http://172.18.0.2:8080/user",
                HttpMethod.POST, user);
        if (response.getStatusCode() == HttpStatus.OK)
            return "redirect:/login";
        else
            return "redirect:/register";
    }

    @GetMapping("/login")
    public String loginPage(HttpSession session) {
        if (session.getAttribute("user") != null) return "redirect:/account";
        else return "login";
    }

    @PostMapping("/login")
    public String authenticateUser(@RequestBody String user, HttpSession session) {
        ResponseEntity<String> response = Communication.callAPI("http://172.18.0.2:8080/user/auth",
                HttpMethod.POST, user);

        JSONObject jsonObject = new JSONObject(Objects.requireNonNull(response.getBody()));

        String collectionName = Communication.callAPI("http://" +
                        jsonObject.getString("affinityNode") +
                        ":" + environment.getProperty("local.server.port") +
                        "/collection",
                HttpMethod.GET, "").getBody();

        Communication.callAPI("http://" +
                        jsonObject.getString("affinityNode") +
                        ":" + environment.getProperty("local.server.port") +
                        "/collection/use?collectionName=users",
                HttpMethod.POST, "");

        double balance = Double.parseDouble(Objects.requireNonNull(Communication.callAPI("http://" +
                        jsonObject.getString("affinityNode") +
                        ":" + environment.getProperty("local.server.port") +
                        "/document/" + jsonObject.getString("_id") + "/property?propertyKey=balance",
                HttpMethod.GET, "").getBody()));

        Communication.callAPI("http://" +
                        jsonObject.getString("affinityNode") +
                        ":" + environment.getProperty("local.server.port") +
                        "/collection/use?collectionName=" + collectionName,
                HttpMethod.POST, "");

        User userObject = User.createUser(jsonObject.getString("_id"),
                jsonObject.getString("username"),
                jsonObject.getString("affinityNode"),
                balance);

        if (response.getStatusCode() == HttpStatus.OK) {
            session.setAttribute("user", userObject);
            return "redirect:/account";
        }
        else return "redirect:/login";
    }

    @GetMapping("/account")
    public String bankAccountPage(Model model, HttpSession session) {
        if (session.getAttribute("user") != null) {
            model.addAttribute("user", session.getAttribute("user"));
            System.out.println("user: " + model.getAttribute("user"));
            return "bankaccount";
        }
        else return "redirect:/login";
    }

    @PostMapping("/account/deposit")
    public String deposit(@RequestParam double depositAmount, HttpSession session) {
        if (session.getAttribute("user") != null) {
            User user = ((User) session.getAttribute("user"));
            user.deposit(depositAmount);
            String[] keys = {"propertyKey", "propertyValue"};
            String[] value = {"balance", String.valueOf(user.getBalance())};
            String collectionName = setUsersCollection(user.getAffinityNode());
            Communication.callAPI("http://" +
                            user.getAffinityNode() +
                            ":" + environment.getProperty("local.server.port") +
                            "/document/" + user.getId() + "/property",
                    HttpMethod.PUT, keys, value);
            resetCollection(collectionName, user.getAffinityNode());
            return "redirect:/account";
        }
        else return "redirect:/login";
    }

    @PostMapping("/account/withdraw")
    public String withdraw(@RequestParam double withdrawAmount, HttpSession session) throws InsufficientBalanceException {
        if (session.getAttribute("user") != null) {
            User user = ((User) session.getAttribute("user"));
            user.withdraw(withdrawAmount);
            String[] keys = {"propertyKey", "propertyValue"};
            String[] value = {"balance", String.valueOf(user.getBalance())};
            String collectionName = setUsersCollection(user.getAffinityNode());
            Communication.callAPI("http://" +
                            user.getAffinityNode() +
                            ":" + environment.getProperty("local.server.port") +
                            "/document/" + user.getId() + "/property",
                    HttpMethod.PUT, keys, value);
            resetCollection(collectionName, user.getAffinityNode());
            return "redirect:/account";
        }
        else return "redirect:/login";
    }

    @PostMapping("/logout")
    public String logout(HttpSession session) {
        session.removeAttribute("user");
        return "redirect:/login";
    }

    private String setUsersCollection(String affinityNode) {
        String collectionName = Communication.callAPI("http://" +
                        affinityNode +
                        ":" + environment.getProperty("local.server.port") +
                        "/collection",
                HttpMethod.GET, "").getBody();

        Communication.callAPI("http://" +
                        affinityNode +
                        ":" + environment.getProperty("local.server.port") +
                        "/collection/use?collectionName=users",
                HttpMethod.POST, "");

        return collectionName;
    }

    private void resetCollection(String collectionName, String affinityNode) {
        Communication.callAPI("http://" +
                        affinityNode +
                        ":" + environment.getProperty("local.server.port") +
                        "/collection/use?collectionName=" + collectionName,
                HttpMethod.POST, "");
    }
}

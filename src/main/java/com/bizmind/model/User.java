package com.bizmind.model;

import java.util.UUID;

public class User {

    private final UUID   id;
    private final String fullName;
    private final String email;
    private final String passwordHash;
    private final String accountType; // "owner" or "worker"

    public User(UUID id, String fullName, String email, String passwordHash, String accountType) {
        this.id           = id;
        this.fullName     = fullName;
        this.email        = email;
        this.passwordHash = passwordHash;
        this.accountType  = accountType;
    }

    public UUID   getId()           { return id; }
    public String getFullName()     { return fullName; }
    public String getEmail()        { return email; }
    public String getPasswordHash() { return passwordHash; }
    public String getAccountType()  { return accountType; }
}

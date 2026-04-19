package com.bizmind.model;

import java.util.UUID;

public class Business {

    private final UUID   id;
    private final UUID   ownerId;
    private final String name;
    private final String joinCode;

    public Business(UUID id, UUID ownerId, String name, String joinCode) {
        this.id       = id;
        this.ownerId  = ownerId;
        this.name     = name;
        this.joinCode = joinCode;
    }

    public UUID   getId()       { return id; }
    public UUID   getOwnerId()  { return ownerId; }
    public String getName()     { return name; }
    public String getJoinCode() { return joinCode; }
}

package com.bizmind.model;

import java.util.UUID;

public class BusinessMember {

    private final UUID   id;
    private final UUID   businessId;
    private final UUID   userId;
    private final String role;   // store_manager, accountant, staff
    private final String status; // pending, accepted, rejected
    private String businessName;
    private String userName;

    public BusinessMember(UUID id, UUID businessId, UUID userId, String role, String status) {
        this.id         = id;
        this.businessId = businessId;
        this.userId     = userId;
        this.role       = role;
        this.status     = status;
    }

    public UUID   getId()           { return id; }
    public UUID   getBusinessId()   { return businessId; }
    public UUID   getUserId()       { return userId; }
    public String getRole()         { return role; }
    public String getStatus()       { return status; }
    public String getBusinessName() { return businessName; }
    public void   setBusinessName(String n) { businessName = n; }
    public String getUserName()     { return userName; }
    public void   setUserName(String n)     { userName = n; }

    public String getRoleDisplay() {
        return switch (role) {
            case "store_manager" -> "Store Manager";
            case "accountant"    -> "Accountant";
            case "staff"         -> "Staff";
            default              -> role;
        };
    }
}

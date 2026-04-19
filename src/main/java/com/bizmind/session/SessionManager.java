package com.bizmind.session;

import com.bizmind.model.Business;
import com.bizmind.model.User;

public class SessionManager {

    private static SessionManager instance;
    private User currentUser;
    private Business currentBusiness;
    private String currentRole; // "owner", "store_manager", "accountant", "staff"

    private SessionManager() {}

    public static SessionManager getInstance() {
        if (instance == null) instance = new SessionManager();
        return instance;
    }

    public User getCurrentUser()              { return currentUser; }
    public void setCurrentUser(User u)        { currentUser = u; }

    public Business getCurrentBusiness()      { return currentBusiness; }
    public void setCurrentBusiness(Business b){ currentBusiness = b; }

    public String getCurrentRole()            { return currentRole; }
    public void setCurrentRole(String role)   { currentRole = role; }

    public boolean isOwner() {
        return currentUser != null && "owner".equals(currentUser.getAccountType());
    }

    public void clear() {
        currentUser     = null;
        currentBusiness = null;
        currentRole     = null;
    }
}

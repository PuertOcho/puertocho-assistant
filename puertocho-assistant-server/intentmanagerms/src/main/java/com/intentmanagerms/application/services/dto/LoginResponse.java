package com.intentmanagerms.application.services.dto;

public class LoginResponse {
    private String token;
    private String type;
    private String username;
    private String email;
    private String fullName;
    
    public LoginResponse() {}
    
    public LoginResponse(String token, String type, String username, String email, String fullName) {
        this.token = token;
        this.type = type;
        this.username = username;
        this.email = email;
        this.fullName = fullName;
    }
    
    public String getToken() {
        return token;
    }
    
    public void setToken(String token) {
        this.token = token;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getFullName() {
        return fullName;
    }
    
    public void setFullName(String fullName) {
        this.fullName = fullName;
    }
}

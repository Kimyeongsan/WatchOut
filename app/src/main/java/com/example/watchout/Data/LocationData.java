package com.example.watchout.Data;

public class LocationData {
    private String user_location;
    private String user_destination;

    public LocationData() { }

    public LocationData(String user_location, String user_destination) {
        this.user_location = user_location;
        this.user_destination = user_destination;
    }

    public String getUser_location() {
        return user_location;
    }
    public String getUser_destination() { return user_destination;}

    public void setUser_location(String user_location) {
        this.user_location = user_location;
    }
    public void setUser_destination(String user_destination) { this.user_destination = user_destination;}
}

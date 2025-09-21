package com.example.feedmatepetfeedersystem;

public class History {
    public String timestamp;
    public int foodLevel;
    public int foodWeight;

    public History() { }

    public History(String timestamp, int foodLevel, int foodWeight) {
        this.timestamp = timestamp;
        this.foodLevel = foodLevel;
        this.foodWeight = foodWeight;
    }
}

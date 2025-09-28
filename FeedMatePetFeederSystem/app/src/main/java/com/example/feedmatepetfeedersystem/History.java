package com.example.feedmatepetfeedersystem;

public class History {
    public String date;     // from parent node (yyyy-MM-dd)
    public String time;     // from child key (HH:mm)
    public String source;   // manual / scheduled / system
    public int level;       // food level (%)
    public double weight;   // food weight (grams)

    public History() {}

    public History(String date, String time, String source, int level, double weight) {
        this.date = date;
        this.time = time;
        this.source = source;
        this.level = level;
        this.weight = weight;
    }
}

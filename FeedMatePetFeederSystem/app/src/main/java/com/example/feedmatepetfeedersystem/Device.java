package com.example.feedmatepetfeedersystem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Device {

    private String owner;
    private Config config;
    private State state;
    private Sensors sensors;
    private History history;
    private Alerts alerts;

    // 🔹 Empty constructor required for Firebase
    public Device() {}

    public Device(String owner) {
        this.owner = owner;
        this.config = new Config();
        this.state = new State();
        this.sensors = new Sensors();
        this.history = new History();
        this.alerts = new Alerts();
    }

    // ================== Config ==================
    public static class Config {
        private Schedule schedule;

        public Config() {
            this.schedule = new Schedule();
        }

        public static class Schedule {
            private List<String> feedingTimes;
            private String nextFeedingTime;

            public Schedule() {
                this.feedingTimes = new ArrayList<>(Arrays.asList("08:00", "12:00", "18:00"));
                this.nextFeedingTime = "12:00";
            }

            // Getters & Setters
            public List<String> getFeedingTimes() { return feedingTimes; }
            public void setFeedingTimes(List<String> feedingTimes) { this.feedingTimes = feedingTimes; }
            public String getNextFeedingTime() { return nextFeedingTime; }
            public void setNextFeedingTime(String nextFeedingTime) { this.nextFeedingTime = nextFeedingTime; }
        }

        public Schedule getSchedule() { return schedule; }
        public void setSchedule(Schedule schedule) { this.schedule = schedule; }
    }

    // ================== State ==================
    public static class State {
        private Controls controls;
        private SystemState system;
        private Servo servo;

        public State() {
            this.controls = new Controls();
            this.system = new SystemState();
            this.servo = new Servo();
        }

        public static class Controls {
            private boolean feedNow;

            public Controls() { this.feedNow = false; }

            public boolean isFeedNow() { return feedNow; }
            public void setFeedNow(boolean feedNow) { this.feedNow = feedNow; }
        }

        public static class SystemState {
            private boolean feedingInProgress;
            private int status; // 0 = idle, 1 = active, 2 = error
            private String lastUpdate;

            public SystemState() {
                this.feedingInProgress = false;
                this.status = 0;
                this.lastUpdate = "";
            }

            public boolean isFeedingInProgress() { return feedingInProgress; }
            public void setFeedingInProgress(boolean feedingInProgress) { this.feedingInProgress = feedingInProgress; }
            public int getStatus() { return status; }
            public void setStatus(int status) { this.status = status; }
            public String getLastUpdate() { return lastUpdate; }
            public void setLastUpdate(String lastUpdate) { this.lastUpdate = lastUpdate; }
        }

        public static class Servo {
            private int angle;

            public Servo() { this.angle = -1; }

            public int getAngle() { return angle; }
            public void setAngle(int angle) { this.angle = angle; }
        }

        public Controls getControls() { return controls; }
        public void setControls(Controls controls) { this.controls = controls; }
        public SystemState getSystem() { return system; }
        public void setSystem(SystemState system) { this.system = system; }
        public Servo getServo() { return servo; }
        public void setServo(Servo servo) { this.servo = servo; }
    }

    // ================== Sensors ==================
    public static class Sensors {
        private Food food;
        private double distance;
        private boolean objectDetected;

        public Sensors() {
            this.food = new Food();
            this.distance = 0.0;
            this.objectDetected = false;
        }

        public static class Food {
            private int level;
            private int weight;

            public Food() {
                this.level = 0;
                this.weight = 0;
            }

            public int getLevel() { return level; }
            public void setLevel(int level) { this.level = level; }
            public int getWeight() { return weight; }
            public void setWeight(int weight) { this.weight = weight; }
        }

        public Food getFood() { return food; }
        public void setFood(Food food) { this.food = food; }
        public double getDistance() { return distance; }
        public void setDistance(double distance) { this.distance = distance; }
        public boolean isObjectDetected() { return objectDetected; }
        public void setObjectDetected(boolean objectDetected) { this.objectDetected = objectDetected; }
    }

    // ================== History ==================
    public static class History {
        private Map<String, Integer> foodLevels;
        private Map<String, Integer> foodWeights;

        public History() {
            this.foodLevels = new HashMap<>(); // mutable maps for Firebase
            this.foodWeights = new HashMap<>();
        }

        public Map<String, Integer> getFoodLevels() { return foodLevels; }
        public void setFoodLevels(Map<String, Integer> foodLevels) { this.foodLevels = foodLevels; }
        public Map<String, Integer> getFoodWeights() { return foodWeights; }
        public void setFoodWeights(Map<String, Integer> foodWeights) { this.foodWeights = foodWeights; }
    }

    // ================== Alerts ==================
    public static class Alerts {
        private boolean lowFoodLevel;
        private String lastTriggered;

        public Alerts() {
            this.lowFoodLevel = false;
            this.lastTriggered = "";
        }

        public boolean isLowFoodLevel() { return lowFoodLevel; }
        public void setLowFoodLevel(boolean lowFoodLevel) { this.lowFoodLevel = lowFoodLevel; }
        public String getLastTriggered() { return lastTriggered; }
        public void setLastTriggered(String lastTriggered) { this.lastTriggered = lastTriggered; }
    }

    // ================== Getters & Setters ==================
    public String getOwner() { return owner; }
    public void setOwner(String owner) { this.owner = owner; }
    public Config getConfig() { return config; }
    public void setConfig(Config config) { this.config = config; }
    public State getState() { return state; }
    public void setState(State state) { this.state = state; }
    public Sensors getSensors() { return sensors; }
    public void setSensors(Sensors sensors) { this.sensors = sensors; }
    public History getHistory() { return history; }
    public void setHistory(History history) { this.history = history; }
    public Alerts getAlerts() { return alerts; }
    public void setAlerts(Alerts alerts) { this.alerts = alerts; }
}

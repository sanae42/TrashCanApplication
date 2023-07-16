package com.example.trashcanapplication;

public class TrashCanBean {
    private int Id;
    private int Distance;
    private int Humidity;
    private int Temperature;
    private double Latitude;
    private double Longitude;

    public double getId() {
        return Id;
    }

    public int getDistance() {
        return Distance;
    }

    public int getHumidity() {
        return Humidity;
    }

    public int getTemperature() {
        return Temperature;
    }

    public double getLatitude() {
        return Latitude;
    }

    public double getLongitude() {
        return Longitude;
    }

    public void setId(int id) {
        Id = id;
    }

    public void setDistance(int distance) {
        Distance = distance;
    }

    public void setHumidity(int humidity) {
        Humidity = humidity;
    }

    public void setTemperature(int temperature) {
        Temperature = temperature;
    }

    public void setLatitude(double latitude) {
        Latitude = latitude;
    }

    public void setLongitude(double longitude) {
        Longitude = longitude;
    }
}

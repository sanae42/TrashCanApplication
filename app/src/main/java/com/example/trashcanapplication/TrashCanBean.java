package com.example.trashcanapplication;

import java.util.Date;

/**
 * @Title：TrashCanBean.java
 * @Description: A class that stores trash can data.
 * @author P Geng
 */
public class TrashCanBean {
    private int Id;
    private int Distance;
    private int Humidity;
    private int Temperature;
    private double Latitude;
    private double Longitude;

    private int Depth;
    private int EstimatedTime;
    private int Variance;
    private Date LastEmptyTime;

    private String LocationDescription;
    private String Mode;

    public int getId() {
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

    public int getDepth() {
        return Depth;
    }

    public int getEstimatedTime() {
        return EstimatedTime;
    }

    public int getVariance() {
        return Variance;
    }

    public Date getLastEmptyTime() {
        return LastEmptyTime;
    }

    public String getLocationDescription() {
        return LocationDescription;
    }

    public String getMode() {
        return Mode;
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

    public void setDepth(int depth) {
        Depth = depth;
    }

    public void setEstimatedTime(int estimatedTime) {
        EstimatedTime = estimatedTime;
    }

    public void setVariance(int variance) {
        Variance = variance;
    }

    public void setLastEmptyTime(Date lastEmptyTime) {
        LastEmptyTime = lastEmptyTime;
    }

    public void setLocationDescription(String locationDescription) {
        LocationDescription = locationDescription;
    }

    public void setMode(String mode) {
        Mode = mode;
    }
}

package com.markopavicic.discovercroatia;

public class Location {
    private final String name;
    private final String description;
    private final Double latitude;
    private final Double longitude;

    public Location(String name, String description, Double latitude, Double longitude) {
        this.name = name;
        this.description = description;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Double getLatitude() {
        return latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

}

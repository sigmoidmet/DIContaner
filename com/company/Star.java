package com.company;

public class Star {
    int brightness;
    String name;

    public Star(int brightness, String name) {
        this.brightness = brightness;
        this.name = name;
    }

    @Override
    public String toString() {
        return "Star{" +
                "brightness=" + brightness +
                ", name='" + name + '\'' +
                '}';
    }
}

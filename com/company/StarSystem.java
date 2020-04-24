package com.company;

import java.util.List;

public class StarSystem {
    public Star star;
    public List<Planet> planets;

    @Override
    public String toString() {
        return "StarSystem{" +
                "star=" + star +
                ", planets=" + planets +
                '}';
    }
}

package com.company;

import com.company.Planet;
import com.company.Star;
import com.company.StarSystem;
import com.company.annotations.Bean;

import java.util.Arrays;
import java.util.List;

public class HardBeansConfig {
    @Bean("Sun")
    public static Star getSun() {
        return new Star(5, "Sun");
    }

    @Bean("Earth")
    public static Planet getEarth() {
        return new Planet("Earth");
    }

    @Bean("Mars")
    public Planet getMars() {
        return new Planet("Mars");
    }

    @Bean("Ssys")
    public static StarSystem getSolarSystem(Star star, List<Planet> planets) {

        StarSystem sSys = new StarSystem();
        sSys.planets = planets;
        sSys.star = star;
        return sSys;
    }

    @Bean("Ssys2")
    public static StarSystem getSolarSystem2(Star star, Planet... planets) {

        StarSystem sSys = new StarSystem();
        sSys.planets = Arrays.asList(planets);
        sSys.star = star;
        return sSys;
    }

    @Bean("Ssys3")
    public static StarSystem getSolarSystem3(Star star, Planet[] planets) {

        StarSystem sSys = new StarSystem();
        sSys.planets = Arrays.asList(planets);
        sSys.star = star;
        return sSys;
    }
}

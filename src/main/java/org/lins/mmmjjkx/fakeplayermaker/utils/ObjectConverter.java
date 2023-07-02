package org.lins.mmmjjkx.fakeplayermaker.utils;

import org.bukkit.Bukkit;
import org.bukkit.Location;

public class ObjectConverter {
    public static Location toLocation(String singleString) {
        String[] strings = singleString.split(",");
        if (strings.length == 4) {
            return new Location(Bukkit.getWorld(strings[0]),
                    Double.parseDouble(strings[1]),
                    Double.parseDouble(strings[2]),
                    Double.parseDouble(strings[3]));
        } else if (strings.length == 6) {
            return new Location(Bukkit.getWorld(strings[0]),
                    Double.parseDouble(strings[1]),
                    Double.parseDouble(strings[2]),
                    Double.parseDouble(strings[3]),
                    Float.parseFloat(strings[4]),
                    Float.parseFloat(strings[5]));
        }
        return null;
    }
    public static String toLocationString(Location location){
        return location.getWorld().getName()+","+location.getX()+","+location.getY()+","+location.getZ()+","+location.getYaw()+","+location.getPitch();
    }
}

package de.geisti.smashdashmash.RegionManager;

import org.bukkit.Location;
import org.bukkit.World;
import org.joml.Random;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Region {
    private final String name;
    private final Location pos1;
    private final Location pos2;
    private final Map<Integer, Location> spawns = new HashMap<>();

    public Region(String name, Location pos1, Location pos2) {
        this.name = name;
        this.pos1 = pos1;
        this.pos2 = pos2;
    }

    public String getName() {
        return name;
    }

    public Location getPos1() {
        return pos1;
    }

    public Location getPos2() {
        return pos2;
    }

    public void setSpawn(int index, Location location) {
        spawns.put(index, location);
    }

    public Location getSpawn(int index) {
        return spawns.get(index);
    }

    public boolean isInRegion(Location location) {
        return location.getWorld().equals(pos1.getWorld()) &&
                location.getX() >= Math.min(pos1.getX(), pos2.getX()) &&
                location.getX() <= Math.max(pos1.getX(), pos2.getX()) &&
                location.getY() >= Math.min(pos1.getY(), pos2.getY()) &&
                location.getY() <= Math.max(pos1.getY(), pos2.getY()) &&
                location.getZ() >= Math.min(pos1.getZ(), pos2.getZ()) &&
                location.getZ() <= Math.max(pos1.getZ(), pos2.getZ());
    }
}

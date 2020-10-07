package com.knoban.atlas.world;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

/**
 * @author Alden Bansemer (kNoAPP)
 */
public class Coordinate {
	
	private String worldName;
	private double x, y, z;
	private float yaw, pitch;

	/**
	 * TO BE USED BY GSON ONLY! DO NOT USE!
	 */
	public Coordinate() {}
	
	/**
	 * Create a Coordinate from an unparsed String.
	 * @param args - ex. world, 2.0, 3.0, 3.5, 0.1f, 0.2f
	 */
	public Coordinate(String[] args) {
		this(args[0], Double.parseDouble(args[1]), Double.parseDouble(args[2]), Double.parseDouble(args[3]), Float.parseFloat(args[4]), Float.parseFloat(args[5]));
	}
	
	public Coordinate(String worldName, double x, double y, double z) {
		this(worldName, x, y, z, 0F, 0F);
	}

	public Coordinate(String worldName, double x, double y, double z, float yaw, float pitch) {
		this.worldName = worldName;
		this.x = x;
		this.y = y;
		this.z = z;
		this.yaw = yaw;
		this.pitch = pitch;
	}
	
	public Coordinate(Location l) {
		this.worldName = l.getWorld().getName();
		this.x = l.getX();
		this.y = l.getY();
		this.z = l.getZ();
		this.yaw = l.getYaw();
		this.pitch = l.getPitch();
	}

	public World getWorld() {
		return Bukkit.getWorld(worldName);
	}
	
	public String getWorldName() {
		return worldName;
	}
	
	public double getX() {
		return x;
	}
	
	public int getBlockX() {
		return (int) x;
	}
	
	public Coordinate setX(double x) {
		this.x = x;
		return this;
	}
	
	public double getY() {
		return y;
	}
	
	public int getBlockY() {
		return (int) y;
	}
	
	public Coordinate setY(double y) {
		this.y = y;
		return this;
	}
	
	public double getZ() {
		return z;
	}
	
	public int getBlockZ() {
		return (int) z;
	}
	
	public Coordinate setZ(double z) {
		this.z = z;
		return this;
	}
	
	public float getYaw() {
		return yaw;
	}
	
	public float getPitch() {
		return pitch;
	}
	
	public Coordinate add(double x, double y, double z) {
		this.x += x;
		this.y += y;
		this.z += z;
		return this;
	}

	public Coordinate toBlock() {
		return new Coordinate(worldName, (int) x, (int) y, (int) z, 0F, 0F);
	}

	public Location getLocation() {
		World w = getWorld();
		if(w == null) return null;
		
		Location l = new Location(w, x, y, z);
		l.setYaw(yaw);
		l.setPitch(pitch);
		return l;
	}

	public Coordinate clone() {
		return new Coordinate(worldName, x, y, z, yaw, pitch);
	}


	public String serialize() {
		return worldName + ";" + x + ";" + y + ";" + z + ";" + yaw + ";" + pitch;
	}

	@NotNull
	public static Coordinate deserialize(String s) {
		String[] args = s.split(";");
		return new Coordinate(args[0], Double.parseDouble(args[1]), Double.parseDouble(args[2]), Double.parseDouble(args[3]), Float.parseFloat(args[4]), Float.parseFloat(args[5]));
	}

	@Override
	public boolean equals(Object o) {
		if(!(o instanceof Coordinate)) {
			return false;
		}

		Coordinate coordinate = (Coordinate) o;
		return coordinate.worldName.equals(worldName) && coordinate.x == x && coordinate.y == y && coordinate.z == z;
	}

	@Override
	public int hashCode() {
		int hash = worldName.hashCode();
		hash = 19 * hash + (int)(Double.doubleToLongBits(this.x) ^ Double.doubleToLongBits(this.x) >>> 32);
		hash = 19 * hash + (int)(Double.doubleToLongBits(this.y) ^ Double.doubleToLongBits(this.y) >>> 32);
		hash = 19 * hash + (int)(Double.doubleToLongBits(this.z) ^ Double.doubleToLongBits(this.z) >>> 32);
		return hash;
	}

	@Override
	public String toString() {
		return worldName + ":" + x + "," + y + "," + z + " [" + pitch + "," + yaw + "] - " + hashCode();
	}
}

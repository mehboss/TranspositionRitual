package me.mehboss.ritual;

import java.util.HashMap;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Candle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public class Ritual {

	private HashMap<String, Location> candles;
	private HashMap<Location, Location> pLocs;
	private HashMap<Location, Entity> standLocs;

	private Location center;
	private Boolean showName;
	private Boolean active;
	private String number;
	private String network;
	private String name;

	private OfflinePlayer owner;
	private Player user;

	private Color color;

	public Ritual(Location center) {
		this.center = center;
		this.pLocs = new HashMap<Location, Location>();
		this.standLocs = new HashMap<Location, Entity>();
		this.candles = new HashMap<String, Location>();
		this.showName = true;
	}

	public Boolean hasNetwork() {
		if (network == null)
			return false;
		return true;
	}

	public Boolean hasShowName() {
		return showName;
	}

	public Boolean hasName() {
		if (name == null)
			return false;
		return true;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setNameShow(Boolean b) {
		this.showName = b;
	}

	public String getNetwork() {
		return network;
	}

	public void setNetwork(String name) {
		this.network = name;
	}

	public String getConfigNumber() {
		return number;
	}

	public void setConfigNumber(String i) {
		this.number = i;
	}

	public Boolean hasArmorStand(Location l) {
		if (!(standLocs.containsKey(l)))
			return false;
		return true;
	}

	public void setArmorStand(Location standloc, Entity stand) {
		standLocs.put(standloc, stand);
	}

	public void removeArmorStand(Location standLoc) {
		standLocs.get(standLoc).remove();
		standLocs.remove(standLoc);
	}

	public HashMap<Location, Entity> getStands() {
		return standLocs;
	}

	public void clearStands() {

		if (standLocs.isEmpty())
			return;

		for (Entity e : standLocs.values())
			e.remove();

		standLocs.clear();
	}

	public HashMap<Location, Location> getParticles() {
		return pLocs;
	}

	public Location getParticle(Location particle) {
		// returns the second location, which should be the center of a different
		// ritual.
		return pLocs.get(particle);
	}

	public void setParticle(Location particle, Location center) {
		pLocs.put(particle, center);
	}

	public void removeParticle(Location particle) {
		pLocs.remove(particle);
	}

	public void clearParticles() {
		pLocs.clear();
	}

	public Location getCenter() {
		return center;
	}

	public Player getUser() {
		return user;
	}

	public void setUser(Player p) {
		this.user = p;
	}

	public Boolean inUse() {
		if (user == null)
			return false;
		return true;
	}

	public Boolean canTeleport() {
		if (hasCandles() && hasAmounts() && areAllLit())
			return true;

		return false;
	}

	public Boolean isActive() {
		return active;
	}

	public void setActive(Boolean b) {
		this.active = b;
	}

	public Color getColor() {
		return color;
	}

	public void setColor(Color color) {
		this.color = color;
	}

	public OfflinePlayer getOwner() {
		return owner;
	}

	public void setOwner(OfflinePlayer p) {
		this.owner = p;
	}

	public void setLocations() {
		World w = center.getWorld();

		Location first1 = center.clone().add(1, 0, 0);
		Location second1 = center.clone().add(2, 0, 0);
		Location third1 = center.clone().add(3, 0, 0);

		Location first2 = center.clone().subtract(1, 0, 0);
		Location second2 = center.clone().subtract(2, 0, 0);
		Location third2 = center.clone().subtract(3, 0, 0);

		Location first3 = center.clone().subtract(0, 0, 1);
		Location second3 = center.clone().subtract(0, 0, 2);
		Location third3 = center.clone().subtract(0, 0, 3);

		Location first4 = center.clone().add(0, 0, 1);
		Location second4 = center.clone().add(0, 0, 2);
		Location third4 = center.clone().add(0, 0, 3);

		Location corner1 = new Location(w, (center.getX() - 2), center.getY(), (center.getZ() + 2));
		Location corner2 = new Location(w, (center.getX() + 2), center.getY(), (center.getZ() - 2));
		Location corner3 = new Location(w, (center.getX() + 2), center.getY(), (center.getZ() + 2));
		Location corner4 = new Location(w, (center.getX() - 2), center.getY(), (center.getZ() - 2));

		candles.put("first1", first1);
		candles.put("first2", first2);
		candles.put("first3", first3);
		candles.put("first4", first4);

		candles.put("second1", second1);
		candles.put("second2", second2);
		candles.put("second3", second3);
		candles.put("second4", second4);

		candles.put("third1", third1);
		candles.put("third2", third2);
		candles.put("third3", third3);
		candles.put("third4", third4);

		candles.put("corner1", corner1);
		candles.put("corner2", corner2);
		candles.put("corner3", corner3);
		candles.put("corner4", corner4);
	}

	public boolean containsLocation(Location loc) {
		if (candles.containsValue(loc))
			return true;

		return false;

	}

	public boolean hasCandles() {
		for (Location loc : this.candles.values()) {

			BlockData candle = loc.getBlock().getBlockData();

			if (!(candle instanceof Candle))
				return false;
		}

		return true;
	}

	public boolean hasAmounts() {

		for (String candle : this.candles.keySet()) {

			Candle c = (Candle) this.candles.get(candle).getBlock().getBlockData();

			if (candle.equals("first1") || candle.equals("first2") || candle.equals("first3") || candle.equals("first4")
					|| candle.equals("corner1") || candle.equals("corner2") || candle.equals("corner3")
					|| candle.equals("corner4")) {

				if (c.getCandles() == 3) {
					continue;

				} else {
					return false;
				}
			}

			if (c.getCandles() != 1)
				return false;
		}

		return true;
	}

	public boolean areAllLit() {
		for (Location loc : this.candles.values()) {
			BlockData b = loc.getBlock().getBlockData();

			if (b == null || !(b instanceof Candle))
				return false;

			Candle c = (Candle) b;

			if (!(c.isLit()))
				return false;
		}

		return true;
	}
}

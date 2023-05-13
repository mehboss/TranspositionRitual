package me.mehboss.ritual;

import java.util.ArrayList;

import org.bukkit.Location;

public class PlayerRitual {
	private ArrayList<Location> ritualLocs;

	public PlayerRitual() {
		this.ritualLocs = new ArrayList<Location>();
	}

	public ArrayList<Location> getRitualCenters() {
		return ritualLocs;
	}

	public int getNoNetworkSize() {

		int i = 0;

		for (Location loc : ritualLocs) {

			if (!(Main.getInstance().rituals.containsKey(loc)))
				continue;

			if (!(Main.getInstance().rituals.get(loc).hasNetwork()))
				i++;
		}

		return i;
	}

	public void addRitual(Ritual ritual) {
		ritualLocs.add(ritual.getCenter());
	}

	public void removeRitual(Ritual ritual) {
		ritualLocs.remove(ritual.getCenter());
	}
}

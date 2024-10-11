package me.mehboss.ritual;

import java.util.HashMap;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Particle.DustOptions;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;

public class LocationManager {

	void manageStands(Location particleLoc, Location center, Ritual ritual, Ritual newRitual) {
		if (!(ritual.hasArmorStand(particleLoc.clone().subtract(0, 1, 0)))) {

			String defaultTitle = ChatColor.translateAlternateColorCodes('&', Main.getInstance().defaultTitle);
			Entity e = center.getWorld().spawnEntity(particleLoc.clone().subtract(0, 1, 0), EntityType.ARMOR_STAND);
			ArmorStand stand = (ArmorStand) e;
			stand.setVisible(false);
			stand.setInvulnerable(true);
			stand.setGravity(false);

			if (newRitual.getCenter().getX() == ritual.getCenter().getX()
					&& newRitual.getCenter().getZ() == ritual.getCenter().getZ()) {
				ritual.setArmorStand(e.getLocation(), e);

				if (!(newRitual.hasName()) || !(newRitual.hasShowName()))
					stand.setCustomName(defaultTitle);
				else
					stand.setCustomName(newRitual.getName() + " " + defaultTitle);

				stand.setCustomNameVisible(true);
				return;
			}

			if (newRitual.hasName() && newRitual.hasShowName()) {
				stand.setCustomName(newRitual.getName());
				stand.setCustomNameVisible(true);
			}

			ritual.setArmorStand(e.getLocation(), e);
		}
	}

	void noNetwork(Location particleLoc, Location center, Location blockc, PlayerRitual owner, Ritual ritual, int i) {

		DustOptions dustOptions = new DustOptions(getRituals().get(owner.getRitualCenters().get(i)).getColor(), 1);
		Ritual newRituals = getRituals().get(owner.getRitualCenters().get(i));

		if (ritual.getParticles().size() > owner.getNoNetworkSize()) {
			ritual.clearParticles();
			ritual.clearStands();
		}

		if ((newRituals != null && newRituals.hasNetwork()) || owner.getRitualCenters().get(i) == null) {
			return;
		}

		if (!(newRituals.canTeleport()))
			return;

		ritual.setParticle(particleLoc, owner.getRitualCenters().get(i));
		center.getWorld().spawnParticle(Particle.valueOf(Main.getInstance().particleManager("DUST")), particleLoc, 1, dustOptions);
		manageStands(particleLoc, center, ritual, newRituals);
	}

	void hasNetwork(Location particleLoc, Location center, Location blockc, PlayerRitual owner, Ritual ritual, int i) {

		Location ritualCenter = getAllNetworks().getRitualsFromNetwork(ritual.getNetwork()).get(i);
		int networkSize = getAllNetworks().getRitualsFromNetwork(ritual.getNetwork()).size();
		Ritual foundRitual = getRituals().get(ritualCenter);

		if (ritualCenter == null || foundRitual == null)
			return;

		DustOptions dustOptions = new DustOptions(foundRitual.getColor(), 1);

		if (ritual.getParticles().size() > networkSize) {
			for (Location loc : ritual.getParticles().keySet()) {
				Location value = ritual.getParticles().get(loc);

				if (!(getRituals().get(value).hasNetwork())
						|| !(getRituals().get(value).getNetwork().equals(ritual.getNetwork()))) {
					ritual.removeParticle(loc);
					ritual.removeArmorStand(loc.clone().subtract(0, 1, 0));
				}
			}
		}

		if (!(foundRitual.hasNetwork()) || !(foundRitual.canTeleport()))
			return;

		ritual.setParticle(particleLoc, ritualCenter);
		center.getWorld().spawnParticle(Particle.valueOf(Main.getInstance().particleManager("DUST")), particleLoc, 1, dustOptions);
		manageStands(particleLoc, center, ritual, foundRitual);
	}

	void spawnLocationParticles(Location blockc, UUID p, double radius, int points) {
		PlayerRitual owner = playerRitual().get(p);
		Ritual ritual = getRituals().get(blockc);

		if (!(ritual.hasNetwork()) && owner.getRitualCenters().size() < 2)
			return;

		if (ritual.hasNetwork() && getAllNetworks().getRitualsFromNetwork(ritual.getNetwork()).size() < 2)
			return;

		Location center = blockc.clone().add(.5, 1, .5);

		for (int i = 0; i < points; i++) {
			double angle = 2 * Math.PI * i / points;
			Location particleLoc = new Location(center.getWorld(), center.getX(), center.getY() + 1, center.getZ());
			particleLoc.setX(center.getX() + Math.cos(angle) * radius);
			particleLoc.setZ(center.getZ() + Math.sin(angle) * radius);

			if (ritual.hasNetwork()) {
				hasNetwork(particleLoc, center, blockc, owner, ritual, i);
			} else {
				noNetwork(particleLoc, center, blockc, owner, ritual, i);
			}
		}
	}

	void spawnCircle(Location blockc, double radius, int points) {

		Location center = blockc.clone().add(.5, 1, .5);

		for (int i = 0; i < points; i++) {
			double angle = 2 * Math.PI * i / points;
			Location particleLoc = new Location(center.getWorld(), center.getX(), center.getY(), center.getZ());
			particleLoc.setX(center.getX() + Math.cos(angle) * radius);
			particleLoc.setZ(center.getZ() + Math.sin(angle) * radius);

			DustOptions dustOptions = new DustOptions(Color.WHITE, 1);
			DustOptions ritualColor = new DustOptions(getRituals().get(blockc).getColor(), 1);

			center.getWorld().spawnParticle(Particle.valueOf(Main.getInstance().particleManager("DUST")), particleLoc, 1, dustOptions);
			center.getWorld().spawnParticle(Particle.valueOf(Main.getInstance().particleManager("DUST")), particleLoc, 1, ritualColor);
		}
	}

	void startRitualParticles(Ritual r) {
		spawnCircle(r.getCenter(), 2.828, 20);
	}

	void startLocationParticles(Ritual r) {
		if (r.hasNetwork()) {
			spawnLocationParticles(r.getCenter(), r.getOwner().getUniqueId(), 2.828,
					getAllNetworks().getRitualsFromNetwork(r.getNetwork()).size());
		} else {
			spawnLocationParticles(r.getCenter(), r.getOwner().getUniqueId(), 2.828, findNoNetworkRituals(r));
		}
	}

	HashMap<Location, Ritual> getRituals() {
		return Main.getInstance().rituals;
	}

	HashMap<UUID, PlayerRitual> playerRitual() {
		return Main.getInstance().owner;
	}

	int findNoNetworkRituals(Ritual r) {
		return playerRitual().get(r.getOwner().getUniqueId()).getNoNetworkSize();
	}

	NetworkManager getAllNetworks() {
		return Main.getInstance().networks;
	}

}

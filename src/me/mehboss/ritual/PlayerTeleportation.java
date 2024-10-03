package me.mehboss.ritual;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;

public class PlayerTeleportation implements Listener {

	private RitualManager RitualManager;

	public PlayerTeleportation(RitualManager plugin) {
		this.RitualManager = plugin;
	}

	@EventHandler
	public void onFire(BlockPlaceEvent e) {
		Player p = e.getPlayer();
		ItemStack mainHand = p.getInventory().getItemInMainHand();
		ItemStack offHand = p.getInventory().getItemInOffHand();

		if (mainHand == null || offHand == null || mainHand.getType() != Material.FLINT_AND_STEEL
				|| offHand.getType() != Material.ENDER_EYE || e.getBlockPlaced().getType() != Material.FIRE)
			return;

		Location playerLoc = p.getLocation();
		Location fireLoc = e.getBlockPlaced().getLocation();
		ArrayList<Double> closestLocs = new ArrayList<>();
		HashMap<Double, Location> closestRitual = new HashMap<Double, Location>();

		if ((playerLoc.getBlock().getX() != fireLoc.getX()) || (playerLoc.getBlock().getY() != fireLoc.getY())
				|| (playerLoc.getBlock().getZ() != fireLoc.getZ()))
			return;

		if (!(playerRitual().containsKey(p.getUniqueId()))
				|| playerRitual().get(p.getUniqueId()).getRitualCenters().isEmpty()) {
			sendMessage(p, "No-Ritual-Travel");
			return;
		}

		if (inUse().containsKey(p)) {
			sendMessage(p, "Not-While-Using");
			return;
		}

		if (!(p.hasPermission("tr.use.endereye"))) {
			sendMessage(p, "No-Endereye-Travel-Perms");
			return;
		}

		for (Location closestRituals : playerRitual().get(p.getUniqueId()).getRitualCenters()) {
			Double distance = playerLoc.getWorld().equals(closestRituals.getWorld()) ? playerLoc.distance(closestRituals) : 1000000000;
			closestLocs.add(distance);
			closestRitual.put(distance, closestRituals);
		}

		Collections.sort(closestLocs);
		Location newLoc = closestRitual.get(closestLocs.get(0));
		Location difference = p.getLocation().clone().subtract(newLoc);

		e.setCancelled(true);
		p.setFireTicks(0);

		Ritual ritual = getRituals().get(newLoc);
		ritual.setActive(true);
		ritual.setUser(p);
		inUse().put(p.getPlayer(), ritual.getCenter());

		RitualManager.teleportEntities(newLoc.clone().add(.5, 0, .5), difference, p);
		RitualManager.startTimer(p.getPlayer(), ritual, System.currentTimeMillis());
	}

	public HashMap<UUID, PlayerRitual> playerRitual() {
		return Main.getInstance().owner;
	}

	public HashMap<Player, Location> inUse() {
		return Main.getInstance().inUse;
	}

	public HashMap<Location, Ritual> getRituals() {
		return Main.getInstance().rituals;
	}

	public void sendMessage(Player p, String configPath) {
		Main.getInstance().sendMessage(p, configPath);
	}
}

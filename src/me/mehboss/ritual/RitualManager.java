package me.mehboss.ritual;

import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class RitualManager implements Listener {

	private LocationManager locationManager;

	public RitualManager(LocationManager loc) {
		this.locationManager = loc;
	}

	void checkGround(Item item, Consumer<Location> callback) {
		new BukkitRunnable() {
			@Override
			public void run() {

				if (item.isDead() || item.isInWater())
					this.cancel();

				if (item.isOnGround()) {
					this.cancel();

					Location loc = item.getLocation();
					callback.accept(loc);
				}
			}
		}.runTaskTimer(Main.getInstance(), 10L, 1L);
	}

	@EventHandler
	void createNetwork(PlayerInteractEntityEvent e) {
		if (!(e.getRightClicked() instanceof ItemFrame))
			return;

		Player p = e.getPlayer();
		ItemFrame frame = (ItemFrame) e.getRightClicked();

		ItemStack iteminHand = p.getInventory().getItemInMainHand();
		Location frameLoc = new Location(frame.getWorld(), (frame.getLocation().getX() - .5),
				Math.floor(frame.getLocation().getY()), (frame.getLocation().getZ() - .5));
		ItemStack item = frame.getItem();

		if (!(getRituals().containsKey(frameLoc) || item == null || p.getInventory().getItemInMainHand() == null)
				|| iteminHand.getType() != Material.NAME_TAG || !(iteminHand.getItemMeta().hasDisplayName()))
			return;

		Ritual ritual = getRituals().get(frameLoc);
		String networkName = iteminHand.getItemMeta().getDisplayName();

		getAllNetworks().createNetwork(networkName);
		getAllNetworks().addRitualToNetwork(networkName, ritual.getCenter());
		ritual.setNetwork(networkName);

		ritualConfig().set("Rituals." + ritual.getConfigNumber() + ".Network", ritual.getNetwork());
		saveRitualConfig();

	}

	@EventHandler
	void removeNetwork(EntityDamageByEntityEvent e) {

		if (!(e.getDamager() instanceof Player) || !(e.getEntity() instanceof ItemFrame))
			return;

		ItemFrame frame = (ItemFrame) e.getEntity();
		Location frameLoc = new Location(frame.getWorld(), (frame.getLocation().getX() - .5),
				Math.floor(frame.getLocation().getY()), (frame.getLocation().getZ() - .5));

		if (!(getRituals().containsKey(frameLoc) || frame.getItem() == null))
			return;

		Ritual ritual = getRituals().get(frameLoc);

		if (ritual.getNetwork() != null
				&& getAllNetworks().isMemberOfNetwork(ritual.getNetwork(), ritual.getCenter())) {
			getAllNetworks().removeRitualFromNetwork(ritual.getNetwork(), ritual.getCenter());
			ritual.setNetwork(null);

			ritualConfig().set("Rituals." + ritual.getConfigNumber() + ".Network", ritual.getNetwork());
			saveRitualConfig();
		}
	}

	void manageName(Item item, Ritual ritual, Location landed, String manage) {
		if (!(ritual.hasName()) || (manage.equals("show") && ritual.hasShowName())
				|| (manage.equals("hide") && !(ritual.hasShowName())))
			return;

		ArmorStand stand = locateRitualByStand(ritual);

		if (stand == null)
			return;

		if (manage.equals("hide")) {
			ritual.setNameShow(false);
			stand.setCustomName("(here)");

		} else if (manage.equals("show")) {
			ritual.setNameShow(true);
			stand.setCustomName(ritual.getName() + " (here)");
		}

		ritualConfig().set("Rituals." + ritual.getConfigNumber() + ".Show-Name", ritual.hasShowName());
		saveRitualConfig();
		item.remove();
	}

	void changeColor(Item item, Ritual ritual, Location landed) {

		String[] st = item.getItemStack().getType().toString().split("_");
		String string = st[0].toUpperCase();
		Color color = Color.WHITE;

		switch (string) {

		case "RED":
			color = Color.RED;
			break;
		case "ORANGE":
			color = Color.ORANGE;
			break;
		case "YELLOW":
			color = Color.YELLOW;
			break;
		case "GREEN":
			color = Color.GREEN;
			break;
		case "LIME":
			color = Color.LIME;
			break;
		case "BLUE":
			color = Color.BLUE;
			break;
		case "CYAN":
			color = Color.TEAL;
			break;
		case "PURPLE":
			color = Color.PURPLE;
			break;
		case "BLACK":
			color = Color.BLACK;
			break;
		case "GRAY":
			color = Color.GRAY;
			break;
		case "WHITE":
			color = Color.WHITE;
			break;
		}
		ritual.setColor(color);
		item.remove();
	}

	ArmorStand locateRitualByStand(Ritual ritual) {
		for (Entity e : ritual.getStands().values())
			if (getRituals().get(ritual.getParticle(e.getLocation().clone().add(0, 1, 0))).getCenter() == ritual
					.getCenter())
				return (ArmorStand) e;
		return null;
	}

	void changeName(Item item, Ritual ritual, Location landed) {
		ItemStack itemStack = item.getItemStack();
		ArmorStand stand = locateRitualByStand(ritual);

		if (!(itemStack.getItemMeta().hasDisplayName()))
			return;

		String name = itemStack.getItemMeta().getDisplayName();
		ritual.setName(name);
		item.remove();

		if (stand != null) {
			stand.setCustomName(name + " (here)");
			stand.setCustomNameVisible(true);
		}
		ritualConfig().set("Rituals." + ritual.getConfigNumber() + ".Name", ritual.getName());
		ritualConfig().set("Rituals." + ritual.getConfigNumber() + ".Show-Name", true);
		saveRitualConfig();
	}

	@EventHandler
	void changeAttribute(PlayerDropItemEvent e) {
		if (!(inUse().containsKey(e.getPlayer())))
			return;

		if (e.getItemDrop().getItemStack().getType() == null)
			return;

		Ritual ritual = getRituals().get(inUse().get(e.getPlayer()));

		checkGround(e.getItemDrop(), (landedLocation) -> {

			Location landed = new Location(landedLocation.getWorld(), Math.floor(landedLocation.getX()),
					landedLocation.getY(), Math.floor(landedLocation.getZ()));

			if (landed.getX() != ritual.getCenter().getX() && landed.getZ() != ritual.getCenter().getZ())
				return;

			if (e.getItemDrop().getItemStack().getType().toString().contains("DYE")) {
				changeColor(e.getItemDrop(), ritual, landed);
			} else if (e.getItemDrop().getItemStack().getType() == Material.PAPER) {
				changeName(e.getItemDrop(), ritual, landed);
			} else if (e.getItemDrop().getItemStack().getType() == Material.INK_SAC) {
				manageName(e.getItemDrop(), ritual, landed, "hide");
			} else if (e.getItemDrop().getItemStack().getType() == Material.BONE_MEAL) {
				manageName(e.getItemDrop(), ritual, landed, "show");
			}
		});
	}

	void findRitual(Ritual rit, Location block, Player p, String action) {
		Ritual ritual = rit;

		if (ritual == null && block != null)
			for (Ritual r : getRituals().values()) {

				if (r.containsLocation(block)) {
					ritual = r;
					break;
				}
			}

		if (ritual == null)
			return;

		if (action.equals("remove")) {
			ritual.clearStands();

			if (ritual.getOwner().getUniqueId() != p.getUniqueId() && !(p.hasPermission("tr.remove.others"))) {
				// if not owner of ritual (aka only breaks it or unlites candle) and no
				// permission to remove other players rituals then simply deactivate it
				if (ritual.isActive())
					ritual.setActive(false);

				ritual.clearParticles();
				sendMessage(p, "Deactivated-Ritual");
				return;
			}

			getAllNetworks().removeRitualFromNetwork(ritual.getNetwork(), ritual.getCenter());
			getRituals().remove(ritual.getCenter());
			ritualConfig().set("Rituals." + ritual.getConfigNumber(), null);
			saveRitualConfig();
			sendMessage(p, "Removed-Ritual");
			ritualList(ritual, "remove");

		} else if (action.equals("create")) {
			getRituals().put(ritual.getCenter(), ritual);
			ritualConfig().set("Rituals." + ritual.getConfigNumber() + ".Location", ritual.getCenter());
			ritualConfig().set("Rituals." + ritual.getConfigNumber() + ".Owner", ritual.getOwner().getUniqueId().toString());
			saveRitualConfig();
			ritualList(ritual, "add");
		}
	}

	void ritualList(Ritual r, String action) {
		if (action.equals("add")) {
			if (playerRitual().containsKey(r.getOwner().getUniqueId())) {
				playerRitual().get(r.getOwner().getUniqueId()).addRitual(r);
				return;
			}

			PlayerRitual player = new PlayerRitual();
			player.addRitual(r);
			playerRitual().put(r.getOwner().getUniqueId(), player);

		} else if (action.equals("remove")) {
			if (playerRitual().containsKey(r.getOwner().getUniqueId())) {
				playerRitual().get(r.getOwner().getUniqueId()).removeRitual(r);
				return;
			}
		}
	}

	void startTimer(Player p, Ritual ritual, Long time) {
		new BukkitRunnable() {
			public void run() {

				if (!(getRituals().containsKey(ritual.getCenter()))) {
					this.cancel();
					return;
				}

				if (!(inUse().containsKey(p)) || !(ritual.isActive()) || (ritual.getUser() != p && ritual.inUse())
						|| ((TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - time)) == (Main.getInstance()
								.getConfig().getLong("Ritual-Timeout")))) {

					// cancel timer and remove from inUse array if ritual is no longer active
					// (breaks or blows out) or after the timeout time has passed

					if (inUse().containsKey(p) && (inUse().get(p).getZ() == ritual.getCenter().getZ())
							&& (inUse().get(p).getX() == ritual.getCenter().getX()))
						inUse().remove(p);

					ritual.clearStands();
					ritual.setActive(false);
					ritual.setUser(null);
					ritual.clearParticles();

					this.cancel();
					return;
				}

				locationManager.startRitualParticles(ritual);

				if (ritual.inUse())
					locationManager.startLocationParticles(ritual);
			}
		}.runTaskTimer(Main.getInstance(), 0L, 1L);
	}

	void teleportEntities(Location newLoc, Location difference, Player p) {
		double yaw = Math.toRadians(p.getLocation().getYaw());
		double cosYaw = Math.cos(yaw);
		double sinYaw = Math.sin(yaw);

		for (Entity entity : p.getNearbyEntities(2.5, 2.5, 2.5)) {
			if (entity instanceof LivingEntity && p.hasLineOfSight(entity)) {
				Vector entityDiff = entity.getLocation().clone().subtract(p.getLocation()).toVector();

				double newX = entityDiff.getX() * cosYaw + entityDiff.getZ() * sinYaw;
				double newZ = entityDiff.getZ() * cosYaw - entityDiff.getX() * sinYaw;
				double newY = entityDiff.getY();
				entityDiff.setX(newX);
				entityDiff.setY(newY);
				entityDiff.setZ(newZ);

				Location newEntityLoc = newLoc.clone().add(entityDiff);
				while (newEntityLoc.getBlock().getType() != Material.AIR)
					newEntityLoc.add(0, 1, 0);

				entity.teleport(newEntityLoc);
			}
		}
		p.teleport(newLoc);
	}

	NetworkManager getAllNetworks() {
		return Main.getInstance().networks;
	}

	HashMap<Location, Ritual> getRituals() {
		return Main.getInstance().rituals;
	}

	HashMap<Player, Location> inUse() {
		return Main.getInstance().inUse;
	}

	HashMap<UUID, PlayerRitual> playerRitual() {
		return Main.getInstance().owner;
	}

	FileConfiguration ritualConfig() {
		return Main.getInstance().ritualConfig;
	}

	void saveRitualConfig() {
		Main.getInstance().saveCustomYml(Main.getInstance().ritualConfig, Main.getInstance().ritualYml);
	}

	void sendMessage(Player p, String configPath) {
		Main.getInstance().sendMessage(p, configPath);
	}
}

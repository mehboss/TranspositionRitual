package me.mehboss.ritual;

import java.util.ArrayList;
import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.cryptomorin.xseries.XSound;

public class UseRitual implements Listener {

	private RitualManager plugin;

	public UseRitual(RitualManager plugin) {
		this.plugin = plugin;
	}

	public boolean checkLocation(Player p) {
		if (inUse().get(p) == null)
			return true;

		if ((inUse().get(p).getZ() != Math.floor(p.getLocation().getZ()))
				|| (inUse().get(p).getX() != Math.floor(p.getLocation().getX())))
			return true;

		return false;
	}

	@EventHandler
	public void dressArmorStand(PlayerArmorStandManipulateEvent e) {
		for (Ritual r : getRituals().values())
			if (r.hasArmorStand(e.getRightClicked().getLocation())) {
				e.setCancelled(true);
				break;
			}
	}

	@EventHandler
	public void modifyRitual(EntityDamageByEntityEvent e) {
		if (!(e.getDamager() instanceof Player))
			return;

		if (e.getEntity() instanceof ArmorStand && inUse().containsKey(e.getDamager()))
			e.setCancelled(true);

		if (e.getEntity() instanceof ItemFrame && getRituals().containsKey(e.getEntity().getLocation())) {
			ItemFrame frame = (ItemFrame) e.getEntity();

			if (frame.getItem() == null)
				e.setCancelled(true);
		}
	}

	@EventHandler
	public void rightClickParticle(PlayerInteractAtEntityEvent e) {

		Player p = e.getPlayer();

		if (!(inUse().containsKey(p)) || awaitingTeleport().contains(p))
			return;

		if (e.getRightClicked() == null || e.getRightClicked().getType() != EntityType.ARMOR_STAND)
			return;

		e.setCancelled(true);
		Ritual ritual = getRituals().get(inUse().get(p));

		if (!(ritual.isActive()) || ritual.getParticles() == null || ritual.getUser().getPlayer() != e.getPlayer()
				|| !(ritual.canTeleport()))
			return;

		if (checkLocation(p)) {
			sendMessage(p, "Not-In-Middle");
			return;
		}

		Location punchLocation = e.getRightClicked().getLocation();

		if (ritual.hasArmorStand(punchLocation)) {
			Location loc = ritual.getParticle(punchLocation.clone().add(0, 1, 0));
			Location newRitualLoc = new Location(loc.getWorld(), Math.floor(loc.getX()), loc.getY(),
					Math.floor(loc.getZ()));
			Location teleportLoc = newRitualLoc.clone().add(.5, 0, .5);
			Location difference = teleportLoc.getWorld().equals(p.getLocation().getWorld())
					? p.getLocation().clone().subtract(teleportLoc)
					: teleportLoc;
			Ritual newRitual = getRituals().get(newRitualLoc);

			if (newRitual.getCenter() == ritual.getCenter())
				return;

			Long delay = Main.getInstance().getConfig().getLong("Teleport-Delay");

			p.playSound(p.getLocation(), XSound.valueOf("BLOCK_NOTE_BLOCK_CHIME").parseSound(), 1, 2);
			p.playSound(p.getLocation(), XSound.valueOf("BLOCK_PORTAL_TRIGGER").parseSound(), 1, 1);

			ArrayList<Player> nearbyPlayers = new ArrayList<>();
			nearbyPlayers.add(p.getPlayer());

			for (Entity entity : p.getNearbyEntities(2.5, 2.5, 2.5)) {
				if (entity instanceof Player && p.hasLineOfSight(entity)) {
					nearbyPlayers.add((Player) entity);
				}
			}

			p.sendMessage(ChatColor.translateAlternateColorCodes('&',
					Main.getInstance().getConfig().getString("Teleporting").replaceAll("%seconds%",
							String.valueOf(Main.getInstance().getConfig().getInt("Teleport-Delay")))));

			spawnEnchantmentParticles(nearbyPlayers, teleportLoc, () -> {
				plugin.teleportEntities(teleportLoc, difference, p);
				runLater(newRitual, p, 1L);

				ritual.setActive(false);
				ritual.setUser(null);

				inUse().remove(p);
				inUse().put(p, newRitualLoc);

				newRitual.setActive(true);
				newRitual.setUser(p);

				// get Line of sight
				// match it with particle location
				// teleport player to new ritual location
				// remove from inuseArray.
				// deactivate old ritual
				// clear particle locations

				if (awaitingTeleport().contains(p))
					awaitingTeleport().remove(p);

				plugin.startTimer(p, newRitual, System.currentTimeMillis());
			}, delay);
		}
	}

	@EventHandler
	public void useRitual(PlayerMoveEvent e) {
		// implement checks for when player moves in the circle particle
		// check to make sure ritual is active
		// make sure player has permission to use a ritual
		// apply new particles with specific ritual locations

		Player p = e.getPlayer();

		// not using a ritual
		if (!(inUse().containsKey(p)))
			return;

		// didn't move locations
		if (e.getFrom().getZ() == e.getTo().getZ() || e.getFrom().getX() == e.getTo().getX())
			return;

		// ritual got broken/no longer exists
		if (!(getRituals().containsKey(inUse().get(p)))) {
			inUse().remove(p);
			return;
		}

		Ritual ritual = getRituals().get(inUse().get(p));

		// ritual isn't even active (somehow got here even though they are inUse
		// arrayList, probably pointless to have this here)

		if (!(ritual.isActive()) || !p.getLocation().getWorld().getUID().equals(ritual.getCenter().getWorld().getUID())
				|| p.getLocation().distance(ritual.getCenter().clone().add(.5, 0, .5)) >= 2.5) {
			if (ritual.inUse()) {
				ritual.clearStands();
				ritual.setUser(null);
				p.playSound(p.getLocation(), XSound.valueOf("BLOCK_NOTE_BLOCK_BIT").parseSound(), 1, 2);
			}
			return;
		}
		// inUse but not in circle, so remove.
		if (ritual.inUse() && p.getLocation().distance(ritual.getCenter().clone().add(.5, 0, .5)) <= 2.5) {
			return;
		}

		p.playSound(p.getLocation(), XSound.valueOf("BLOCK_NOTE_BLOCK_CHIME").parseSound(), 1, 2);
		if (!(ritual.inUse())) {
			p.playSound(p.getLocation(), XSound.valueOf("BLOCK_NOTE_BLOCK_CHIME").parseSound(), 1, 2);
			ritual.setUser(p);
		}
	}

	public void faceDirection(Player player, Location target) {
		Vector dir = target.clone().subtract(player.getEyeLocation()).toVector();
		Location loc = player.getLocation().setDirection(dir);
		player.teleport(loc);
	}

	public void runLater(Ritual r, Player p, Long l) {
		Bukkit.getScheduler().runTaskLater(Main.getInstance(), new Runnable() {
			@Override
			public void run() {
				for (Location standLoc : r.getStands().keySet()) {
					if (getRituals().get(r.getParticle(standLoc.clone().add(0, 1, 0))).getCenter() != r.getCenter()) {
						faceDirection(p, standLoc);
						break;
					}
				}
			}
		}, l);
	}

	public void spawnEnchantmentParticles(ArrayList<Player> player, Location center, Runnable callback,
			long delayInSeconds) {
		int particlesPerIteration = 20;
		int iterations = 30;
		long collapseDuration = delayInSeconds; // in seconds

		new BukkitRunnable() {
			double radius = 2.0;
			int currentIteration = 0;
			long particlesStartTimestamp = System.currentTimeMillis();

			@Override
			public void run() {
				for (Player p : player)
					if (!(awaitingTeleport().contains(p))) {
						awaitingTeleport().add(p);
					}

				if (currentIteration >= iterations) {
					// Collapse particles towards center
					long timeSinceParticlesStart = System.currentTimeMillis() - particlesStartTimestamp;
					double collapseProgress = Math.min(1.0,
							(double) timeSinceParticlesStart / (collapseDuration * 1000));
					radius = (1.0 - collapseProgress) * 2.0;

					for (int i = 0; i < radius * 2; i++) {
						for (int j = 0; j < radius * 2; j++) {
							Location loc = new Location(center.getWorld(), center.getX() - radius + i + 0.5,
									center.getY(), center.getZ() - radius + j + 0.5);

							for (Player p : player)
								p.spawnParticle(Particle.valueOf(Main.getInstance().particleManager("ENCHANT")), loc,
										1);
						}
					}

					// Run callback and cancel the task when collapse is complete
					if (collapseProgress >= 1.0) {

						// Schedule the callback with the delay
						callback.run();
						this.cancel();
						return;
					}
				}

				// Spawn particles around player
				Location loc = player.get(0).getLocation().add(0, 1, 0);
				double angleStep = Math.PI * 2 / particlesPerIteration;
				for (int i = 0; i < particlesPerIteration; i++) {
					double angle = i * angleStep;
					double distance = radius * (1 - (double) currentIteration / iterations);
					double x = loc.getX() + distance * Math.cos(angle);
					double z = loc.getZ() + distance * Math.sin(angle);
					Location particleLoc = new Location(center.getWorld(), x, loc.getY(), z);

					for (Player p : player)
						p.spawnParticle(Particle.valueOf(Main.getInstance().particleManager("ENCHANT")), particleLoc,
								1);
				}

				currentIteration++;
			}
		}.runTaskTimer(Main.getInstance(), 0, 2L);
	}

	public void sendMessage(Player p, String configPath) {
		if (Main.getInstance().getConfig().getString(configPath) != null) {
			p.sendMessage(ChatColor.translateAlternateColorCodes('&', Main.getInstance().getConfig().getString(configPath)));
		}
	}

	HashMap<Location, Ritual> getRituals() {
		return Main.getInstance().rituals;
	}

	HashMap<Player, Location> inUse() {
		return Main.getInstance().inUse;
	}

	ArrayList<Player> awaitingTeleport() {
		return Main.getInstance().awaitingTeleport;
	}
}

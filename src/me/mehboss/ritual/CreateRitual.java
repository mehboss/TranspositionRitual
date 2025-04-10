package me.mehboss.ritual;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;

public class CreateRitual implements Listener {

	ArrayList<ItemStack> createItem = new ArrayList<>();
	ArrayList<ItemStack> activateItem = new ArrayList<>();

	enum Action {
		CREATE, ACTIVATE, NONE
	}

	private RitualManager plugin;

	public CreateRitual(RitualManager plugin) {
		this.plugin = plugin;
	}

	@EventHandler
	void createRitual(PlayerDropItemEvent p) {
		Item item = p.getItemDrop();
		// get item dropped

		if (item == null)
			return;

		boolean foundItem = false;
		Action rawAction = Action.NONE;
		ItemStack itemStack = item.getItemStack();
		Material type = itemStack.getType();

		if (Main.getInstance().createItem.contains(type)) {
			rawAction = Action.CREATE;
			foundItem = true;
		}

		if (Main.getInstance().activateItem.contains(type)) {
			rawAction = Action.ACTIVATE;
			foundItem = true;
		}

		if (!foundItem)
			return;

		if (Main.getInstance().getConfig().isConfigurationSection("Seperate-Drop-Items")
				&& Main.getInstance().getConfig().getBoolean("Seperate-Drop-Items") == false) {
			rawAction = Action.NONE;
		}

		final Action actionItem = rawAction;

		plugin.checkGround(item, (landedLocation) -> {
			handleLandedLocation(p, landedLocation, item, actionItem);
		});
	}

	int checkPerms(Player p) {

		if (p.isOp() || p.hasPermission("tr.create.*"))
			return 0;

		for (int x = 100; x < 1; x--) {
			if (!(p.hasPermission("tr.create." + String.valueOf(x)))) {
				continue;
			}

			return (x);
		}
		return -1;
	}

	boolean checkCreationLimit(Player p, PlayerDropItemEvent e) {
		int limit = checkPerms(p);
		if (limit != 0 && playerRitual().containsKey(p.getUniqueId()))
			if (playerRitual().get(p.getUniqueId()).getRitualCenters().size() == limit) {
				sendMessage(p, "Hit-Creation-Limit");
				e.setCancelled(true);
				return false;
			}

		if (limit == -1) {
			sendMessage(p, "No-Create-Perms");
			return false;
		}

		return true;
	}

	void handleLandedLocation(PlayerDropItemEvent p, Location center, Item item, Action actionItem) {
		Location loc = new Location(center.getWorld(), Math.floor(center.getX()), center.getY(),
				Math.floor(center.getZ()));
		Ritual ritual = null;

		if (getRituals().containsKey(loc)) {
			// ritual already exists, continue down code to activate
			ritual = getRituals().get(loc);

		} else {
			// ritual needs to be created
			if (actionItem == Action.CREATE) {
				if (checkCreationLimit(p.getPlayer(), p)) {
					ritual = saveRitual(loc, p.getPlayer());
					
					if (ritual != null)
						p.getItemDrop().remove();
				}
				return;
			}
		}

		if (ritual == null) {
			
			if (actionItem == Action.ACTIVATE) {
				Ritual checkRitual = new Ritual(center);
				checkRitual.setLocations();

				if (checkRitual.hasCandles()) {
					// send needs to be created ritual
					sendMessage(p.getPlayer(), "Not-Created");
					p.setCancelled(true);
				}
			}
			return;
		}

		if (!(ritual.areAllLit()))
			return;

		if (ritual.getOwner().getUniqueId() == p.getPlayer().getUniqueId()
				&& !(p.getPlayer().hasPermission("tr.use"))) {
			sendMessage(p.getPlayer(), "No-Use-Perms");
			p.setCancelled(true);
			return;
		}
		// player does not have permission to use other people' rituals
		if (ritual.getOwner().getUniqueId() != p.getPlayer().getUniqueId()
				&& !(p.getPlayer().hasPermission("tr.use.others"))) {
			sendMessage(p.getPlayer(), "No-Use-Other-Perms");
			p.setCancelled(true);
			return;
		}

		if (actionItem == Action.CREATE) {
			// send message that ritual has already been created
			sendMessage(p.getPlayer(), "Already-Created");
			p.setCancelled(true);
			return;
		}

		// passed all checks, now activate if not activated
		if (ritual.isActive()) {
			sendMessage(p.getPlayer(), "Already-Active");
			p.setCancelled(true);
			return;
		}

		if (inUse().containsKey(p.getPlayer())) {
			sendMessage(p.getPlayer(), "Already-Using");
			p.setCancelled(true);
			return;
		}

		if (playerRitual().get(ritual.getOwner().getUniqueId()) == null
				|| playerRitual().get(ritual.getOwner().getUniqueId()).getRitualCenters().size() < 2
				|| (ritual.hasNetwork() && getAllNetworks().getRitualsFromNetwork(ritual.getNetwork()).size() <= 1)) {
			sendMessage(p.getPlayer(), "No-Ritual-Travel");
			p.setCancelled(true);
			return;
		}

		sendMessage(p.getPlayer(), "Activate-Ritual");
		ritual.setActive(true);
		ritual.setUser(p.getPlayer());

		item.remove();

		inUse().put(p.getPlayer(), ritual.getCenter());
		plugin.startTimer(p.getPlayer(), ritual, System.currentTimeMillis());
	}

	Ritual saveRitual(Location center, Player owner) {
		String configNumber = "[" + (getRituals().size() + 1) + "]";
		Ritual ritual = new Ritual(center);
		ritual.setLocations();
		ritual.setActive(false);
		ritual.setOwner(owner);
		ritual.setColor(Color.WHITE);
		ritual.setConfigNumber(configNumber);

		if (ritual.hasCandles() && ritual.hasAmounts()) {
			getRituals().put(ritual.getCenter(), ritual);
			ritualConfig().set("Rituals." + ritual.getConfigNumber() + ".Location", ritual.getCenter());
			ritualConfig().set("Rituals." + ritual.getConfigNumber() + ".Owner",
					ritual.getOwner().getUniqueId().toString());
			saveRitualConfig();

			plugin.ritualList(ritual, "add");
			sendMessage(owner.getPlayer(), "Ritual-Created");
			
			return ritual;
		}

		return null;
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

	NetworkManager getAllNetworks() {
		return Main.getInstance().networks;
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

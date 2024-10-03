package me.mehboss.ritual;

import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;

public class CreateRitual implements Listener {

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

		ItemStack itemStack = item.getItemStack();
		Material type = itemStack.getType();
		Material newMaterial = null;
		// convert to itemstack

		for (String material : Main.getInstance().getConfig().getStringList("Drop-Item")) {
			Optional<XMaterial> rawMaterial = XMaterial.matchXMaterial(material);

			if (!rawMaterial.isPresent())
				continue;

			if (type == rawMaterial.get().parseMaterial()) {
				newMaterial = rawMaterial.get().parseMaterial();
				break;
			}
		}
		// get material

		if (newMaterial == null)
			return;
		
		plugin.checkGround(item, (landedLocation) -> {
			handleLandedLocation(p, landedLocation, item);
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

	void handleLandedLocation(PlayerDropItemEvent p, Location center, Item item) {
		Location loc = new Location(center.getWorld(), Math.floor(center.getX()), center.getY(),
				Math.floor(center.getZ()));
		Ritual ritual = null;

		if (getRituals().containsKey(loc)) {
			ritual = getRituals().get(loc);
		} else {
			if (checkCreationLimit(p.getPlayer(), p))
				ritual = saveRitual(loc, p.getPlayer());
		}

		if (ritual == null || !(ritual.areAllLit())) {
			return;
		}

		if (ritual.getOwner().getUniqueId() == p.getPlayer().getUniqueId()
				&& !(p.getPlayer().hasPermission("tr.use"))) {
			sendMessage(p.getPlayer(), "No-Use-Perms");
			return;
		}
		// player does not have permission to use other people' rituals
		if (ritual.getOwner().getUniqueId() != p.getPlayer().getUniqueId()
				&& !(p.getPlayer().hasPermission("tr.use.others"))) {
			sendMessage(p.getPlayer(), "No-Use-Other-Perms");
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

		if (playerRitual().get(ritual.getOwner().getUniqueId()) == null || playerRitual().get(ritual.getOwner().getUniqueId()).getRitualCenters().isEmpty()
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

		getRituals().put(center, ritual);

		if (ritual.hasCandles() && ritual.hasAmounts()) {
			plugin.findRitual(ritual, null, owner, "create");
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

	void sendMessage(Player p, String configPath) {
		Main.getInstance().sendMessage(p, configPath);
	}
}

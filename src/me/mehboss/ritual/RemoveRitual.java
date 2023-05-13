package me.mehboss.ritual;

import java.util.HashMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.data.type.Candle;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPhysicsEvent;

public class RemoveRitual implements Listener {

	private RitualManager plugin;

	public RemoveRitual(RitualManager plugin) {
		this.plugin = plugin;
	}

	@EventHandler
	void candleBlow(BlockPhysicsEvent e) {
		if (e.getBlock().getType() != Material.CANDLE || !(e.getBlock().getBlockData() instanceof Candle))
			return;

		Candle c = (Candle) e.getBlock().getBlockData();

		if (c.isLit())
			return;

		for (Ritual ritual : getRituals().values()) {
			if (ritual.containsLocation(e.getBlock().getLocation())) {
				ritual.setActive(false);
				ritual.setUser(null);
				ritual.clearParticles();
				ritual.clearStands();
				break;
			}
		}
	}

	@EventHandler
	void removeRitual(BlockBreakEvent e) {
		if (e.getBlock().getType() != Material.CANDLE)
			return;

		plugin.findRitual(null, e.getBlock().getLocation(), e.getPlayer(), "remove");
	}

	HashMap<Location, Ritual> getRituals() {
		return Main.getInstance().rituals;
	}
}

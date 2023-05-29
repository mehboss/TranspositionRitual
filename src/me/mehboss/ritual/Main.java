package me.mehboss.ritual;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import me.mehboss.recipe.CustomRecipes;

public class Main extends JavaPlugin {

	private static Main instance;
	boolean debug = false;

	NetworkManager networks = new NetworkManager();
	HashMap<Location, Ritual> rituals = new HashMap<Location, Ritual>();
	HashMap<UUID, PlayerRitual> owner = new HashMap<UUID, PlayerRitual>();
	HashMap<Player, Location> inUse = new HashMap<Player, Location>();
	ArrayList<Player> awaitingTeleport = new ArrayList<>();

	File ritualYml = new File(getDataFolder() + "/rituals.yml");
	FileConfiguration ritualConfig = null;
	File debugFile = null;

	@Override
	public void onEnable() {
		RitualManager rm = new RitualManager(new LocationManager());
		instance = this;

		Bukkit.getPluginManager().registerEvents(rm, this);
		Bukkit.getPluginManager().registerEvents(new UseRitual(rm), this);
		Bukkit.getPluginManager().registerEvents(new CreateRitual(rm), this);
		Bukkit.getPluginManager().registerEvents(new RemoveRitual(rm), this);
		Bukkit.getPluginManager().registerEvents(new PlayerTeleportation(rm), this);

		saveDefaultConfig();
		reloadConfig();
		saveCustomYml(ritualConfig, ritualYml);
		initCustomYml();

		importRituals();
		checkDebug();
		// loads in rituals (center of block) from rituals config.
		// sets to active state
		// sets owner

		int pluginId = 18301;
		Metrics metrics = new Metrics(this, pluginId);
		metrics.addCustomChart(new Metrics.MultiLineChart("players_and_servers", new Callable<Map<String, Integer>>() {

			@Override
			public Map<String, Integer> call() throws Exception {
				Map<String, Integer> valueMap = new HashMap<>();
				valueMap.put("servers", 1);
				valueMap.put("players", Bukkit.getOnlinePlayers().size());
				return valueMap;
			}
		}));
	}

	@Override
	public void onDisable() {
		saveRituals();
	}

	public void saveCustomYml(FileConfiguration ymlConfig, File ymlFile) {
		if (!ritualYml.exists()) {
			saveResource("rituals.yml", false);
		}

		if (ymlFile.exists() && ymlConfig != null) {
			try {
				ymlConfig.save(ymlFile);
			} catch (IOException e) {
				return;

			}
		}
	}

	public HashMap<String, ItemStack> getCustomItem(String identifier) {
		return CustomRecipes.getCustomItem();
	}

	public void initCustomYml() {
		ritualConfig = YamlConfiguration.loadConfiguration(ritualYml);

	}

	public static Main getInstance() {
		return instance;
	}

	public void saveRituals() {
		if (ritualConfig.getConfigurationSection("Rituals") == null)
			return;

		for (Ritual r : rituals.values()) {
			r.clearStands();
			if (ritualConfig.getLocation("Rituals." + r.getConfigNumber() + ".Location") != null
					&& ritualConfig.getLocation("Rituals." + r.getConfigNumber() + ".Location") == r.getCenter()) {

				ritualConfig.set("Rituals." + r.getConfigNumber() + ".Location", r.getCenter());
				ritualConfig.set("Rituals." + r.getConfigNumber() + ".Owner", r.getOwner());
				ritualConfig.set("Rituals." + r.getConfigNumber() + ".Color", r.getColor());
				ritualConfig.set("Rituals." + r.getConfigNumber() + ".Network", r.getNetwork());
				ritualConfig.set("Rituals." + r.getConfigNumber() + ".Name", r.getName());
				ritualConfig.set("Rituals." + r.getConfigNumber() + ".Show-Name", r.hasShowName());

				saveCustomYml(ritualConfig, ritualYml);
			}
		}
	}

	public void importRituals() {

		debug = getConfig().getBoolean("Debug");

		if (ritualConfig.getConfigurationSection("Rituals") == null)
			return;

		for (String loc : ritualConfig.getConfigurationSection("Rituals").getKeys(false)) {
			Location oldl = ritualConfig.getLocation("Rituals." + loc + ".Location");
			Location l = new Location(oldl.getWorld(), Math.floor(oldl.getX()), Math.floor(oldl.getY()),
					Math.floor(oldl.getZ()));

			Ritual ritual = new Ritual(l);
			PlayerRitual player = new PlayerRitual();

			Color color = ritualConfig.getColor("Rituals." + loc + ".Color");
			String ritualName = ritualConfig.getString("Rituals." + loc + ".Name");
			Boolean showName = ritualConfig.getBoolean("Rituals." + loc + ".Show-Name");
			String networkName = ritualConfig.getString("Rituals." + loc + ".Network");

			OfflinePlayer ownerName = ritualConfig.getOfflinePlayer("Rituals." + loc + ".Owner");

			ritual.setOwner(ownerName);
			ritual.setLocations();
			ritual.setColor(color);
			ritual.setActive(false);
			ritual.setConfigNumber(loc);
			ritual.setName(ritualName);
			ritual.setNameShow(showName);

			if (networkName != null) {
				ritual.setNetwork(networkName);
				networks.createNetwork(networkName);
				networks.addRitualToNetwork(networkName, ritual.getCenter());
			}

			rituals.put(l, ritual);

			if (owner.containsKey(ritual.getOwner().getUniqueId()))
				player = owner.get(ritual.getOwner().getUniqueId());

			player.addRitual(ritual);
			owner.putIfAbsent(ritual.getOwner().getUniqueId(), player);
		}
	}

	public void logDebug(String string) {
		if (debug) {
			try {
				PrintWriter writer = new PrintWriter(new FileWriter(debugFile, true), true);
				if (string.equals("")) {
					writer.write(System.getProperty("line.separator"));
				} else {
					Date dt = new Date();
					SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
					String time = df.format(dt);
					writer.write(time + " [ColorMe Debug] " + string);
					writer.write(System.getProperty("line.separator"));
				}
				writer.close();
			} catch (IOException e) {
				Bukkit.getServer().getLogger().warning("An error occurred while writing to the log! IOException");
			}
		}
	}

	public void checkDebug() {
		if (debug) {
			debugFile = new File(Bukkit.getServer().getPluginManager().getPlugin("ColorMe").getDataFolder(),
					"debug.log");
			if (!debugFile.exists()) {
				try {
					debugFile.createNewFile();
				} catch (IOException e) {
					Bukkit.getServer().getLogger().warning("Failed to create the debug.log! IOException");
				}
			}
		}
	}

	public void sendMessage(Player p, String configPath) {
		if (getConfig().getString(configPath) != null) {
			String message = ChatColor.translateAlternateColorCodes('&', getConfig().getString(configPath));
			p.sendMessage(message);
		}
	}
}

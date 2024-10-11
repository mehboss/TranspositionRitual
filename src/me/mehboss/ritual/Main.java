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
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import com.cryptomorin.xseries.XMaterial;

public class Main extends JavaPlugin implements Listener {

	private static Main instance;
	boolean debug = false;

	Boolean uptodate = true;
	String newupdate = null;

	String defaultTitle = ChatColor.translateAlternateColorCodes('&', "(here)");
	
	NetworkManager networks = new NetworkManager();
	HashMap<Location, Ritual> rituals = new HashMap<Location, Ritual>();
	HashMap<UUID, PlayerRitual> owner = new HashMap<UUID, PlayerRitual>();
	HashMap<Player, Location> inUse = new HashMap<Player, Location>();
	ArrayList<Player> awaitingTeleport = new ArrayList<>();

	ArrayList<Material> createItem = new ArrayList<>();
	ArrayList<Material> activateItem = new ArrayList<>();

	File ritualYml = new File(getDataFolder() + "/rituals.yml");
	FileConfiguration ritualConfig = null;
	File debugFile = null;

	@Override
	public void onEnable() {
		RitualManager rm = new RitualManager(new LocationManager());
		instance = this;

		try {
			if (getConfig().getStringList("Create-Item") != null)
				for (String material : getConfig().getStringList("Create-Item")) {
					Optional<XMaterial> rawMaterial = XMaterial.matchXMaterial(material);

					if (!rawMaterial.isPresent()) {
						printException(material, "Create-Item");
						continue;
					}

					createItem.add(rawMaterial.get().parseMaterial());
				}

			for (String material : getConfig().getStringList("Activate-Item")) {
				Optional<XMaterial> rawMaterial = XMaterial.matchXMaterial(material);

				if (!rawMaterial.isPresent()) {
					printException(material, "Activate-Item");
					continue;
				}

				activateItem.add(rawMaterial.get().parseMaterial());
			}
		} catch (Exception e) {
			getLogger().log(Level.SEVERE,
					"Failed to register activation items! Missing important configuration section 'Activate-Item'");
		}
		
		if (getConfig().getString("Using-Title") != null)
			defaultTitle = getConfig().getString("Using-Title");

		if (activateItem.isEmpty() && createItem.isEmpty())
			getLogger().log(Level.SEVERE,
					"No drop items were found! Please make sure you have placed items in the 'Activate-Item' and/or the 'Create-Item' sections!");

		Bukkit.getPluginManager().registerEvents(this, this);
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

		new UpdateChecker(this, 110584).getVersion(version -> {

			newupdate = version;

			if (getDescription().getVersion().equals(version)) {
				getLogger().log(Level.INFO, "Checking for updates..");
				getLogger().log(Level.INFO,
						"We are all up to date with the latest version. Thank you for using teleportation rituals :)");
			} else {
				getLogger().log(Level.INFO, "Checking for updates..");
				getLogger().log(Level.WARNING,
						"An update has been found! This could be bug fixes or additional features. Please update TP-Rituals at https://www.spigotmc.org/resources/%E2%96%BA%E2%96%BA-teleportation-rituals-1-8-x-1-20-x-teleporting-rituals-networking-system-%E2%97%84%E2%97%84.110584/");
				uptodate = false;

			}
		});
	}

	@Override
	public void onDisable() {
		saveRituals();
	}

	public void printException(String material, String action) {
		getLogger().log(Level.SEVERE,
				"Material " + material.toUpperCase() + " in config section " + action + " not found. Skipping..");
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

	public void initCustomYml() {
		ritualConfig = YamlConfiguration.loadConfiguration(ritualYml);

	}

	public static Main getInstance() {
		return instance;
	}

	public void saveRituals() {
		if (ritualConfig == null)
			return;

		if (ritualConfig.getConfigurationSection("Rituals") == null)
			return;

		for (Ritual r : rituals.values()) {
			r.clearStands();
			if (ritualConfig.getLocation("Rituals." + r.getConfigNumber() + ".Location") != null
					&& ritualConfig.getLocation("Rituals." + r.getConfigNumber() + ".Location") == r.getCenter()) {

				ritualConfig.set("Rituals." + r.getConfigNumber() + ".Location", r.getCenter());
				ritualConfig.set("Rituals." + r.getConfigNumber() + ".Owner", r.getOwner().getUniqueId().toString());
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

			Color color = ritualConfig.isSet("Rituals." + loc + ".Color")
					? ritualConfig.getColor("Rituals." + loc + ".Color")
					: Color.WHITE;
			String ritualName = ritualConfig.getString("Rituals." + loc + ".Name");
			Boolean showName = ritualConfig.isSet("Rituals." + loc + ".Show-Name")
					? ritualConfig.getBoolean("Rituals." + loc + ".Show-Name")
					: false;
			String networkName = ritualConfig.getString("Rituals." + loc + ".Network");

			OfflinePlayer ownerName = Bukkit
					.getOfflinePlayer(UUID.fromString(ritualConfig.getString("Rituals." + loc + ".Owner")));

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

	public void sendMessage(Player p, String configPath) {
		if (getConfig().getString(configPath) == null) {
			getLogger().log(Level.SEVERE,
					"Failed to send message to " + p.getName() + ". Missing config path " + configPath);
			return;
		}

		String message = ChatColor.translateAlternateColorCodes('&', getConfig().getString(configPath));
		p.sendMessage(message);
	}

	@EventHandler
	public void update(PlayerJoinEvent e) {
		if (getConfig().isSet("Update-Check") && getConfig().getBoolean("Update-Check") == true && e.getPlayer().isOp()
				&& !getDescription().getVersion().equals(newupdate)) {
			e.getPlayer()
					.sendMessage(ChatColor.translateAlternateColorCodes('&',
							"&cTeleportation Rituals: &fAn update has been found. Please download version&c "
									+ newupdate + ", &fyou are on version&c " + getDescription().getVersion() + "!"));
		}
	}

	public String particleManager(String st) {
		String version = Bukkit.getServer().getBukkitVersion();
		// Example version: 1.14-R0.1-SNAPSHOT
		int minorVersion = -1;

		// Split by "-" to get the version part before the hyphen
		String[] parts = version.split("-");
		if (parts.length > 0) {
			String[] version_parts = parts[0].split("\\.");
			try {
				if (version_parts.length > 1)
					minorVersion = Integer.parseInt(version_parts[1]);
			} catch (NumberFormatException e) {
				Main.getInstance().getLogger().log(Level.WARNING,
						"Error parsing version numbers from server: " + version);
			}

			if (minorVersion != -1 && minorVersion >= 20) {
				if (st.equals("DUST"))
					return "REDSTONE";

				if (st.equals("ENCHANT"))
					return "ENCHANTMENT_TABLE";
			}
		}

		return st;
	}
}

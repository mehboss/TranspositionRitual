package me.mehboss.ritual;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Location;

public class NetworkManager {
	private static Map<String, Network> networks = new HashMap<>();

	public void createNetwork(String name) {
		if (networks.containsKey(name)) {
			return;
		}

		Network network = new Network(name);
		networks.put(name, network);
	}

	public void addRitualToNetwork(String name, Location member) {
		if (!networks.containsKey(name)) {
			return;
		}

		Network network = networks.get(name);
		if (!network.contains(member))
			network.addMember(member);
	}

	public void removeRitualFromNetwork(String name, Location member) {
		if (!networks.containsKey(name)) {
			return;
		}
		Network network = networks.get(name);
		if (network.contains(member))
			network.removeMember(member);

		if (network.members.isEmpty())
			networks.remove(name);
	}

	public List<Location> getRitualsFromNetwork(String name) {
		if (!networks.containsKey(name))
			return null;

		Network network = networks.get(name);
		return network.getMembers();
	}

	public boolean isMemberOfNetwork(String name, Location location) {
		if (!networks.containsKey(name)) {
			return false;
		}
		
		Network network = networks.get(name);
		return network.contains(location);
	}

	private class Network {
		private ArrayList<Location> members;

		public Network(String name) {
			this.members = new ArrayList<>();
		}

		public boolean contains(Location location) {
			if (members.contains(location))
				return true;
			return false;
		}

		public void addMember(Location location) {
			if (members.contains(location))
				return;
			members.add(location);
		}

		public void removeMember(Location location) {
			if (!(members.contains(location)))
				return;
			members.remove(location);
		}

		public List<Location> getMembers() {
			return members;
		}
	}
}

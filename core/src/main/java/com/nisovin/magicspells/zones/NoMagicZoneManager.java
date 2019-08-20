package com.nisovin.magicspells.zones;

import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.HashMap;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.zones.NoMagicZone.ZoneCheckResult;

public class NoMagicZoneManager {

	private Map<String, Class<? extends NoMagicZone>> zoneTypes;
	private Map<String, NoMagicZone> zones;
	private Set<NoMagicZone> zonesOrdered;

	public NoMagicZoneManager() {
		// Create zone types
		zoneTypes = new HashMap<>();
		zoneTypes.put("cuboid", NoMagicZoneCuboid.class);
		zoneTypes.put("worldguard", NoMagicZoneWorldGuard.class);
	}

	// DEBUG INFO: level 3, loaded no magic zone, zonename
	// DEBUG INFO: level 1, no magic zones loaded #
	public void load(MagicConfig config) {
		// Get zones
		zones = new HashMap<>();
		zonesOrdered = new TreeSet<>();

		Set<String> zoneNodes = config.getKeys("no-magic-zones");
		if (zoneNodes != null) {
			for (String node : zoneNodes) {
				ConfigurationSection zoneConfig = config.getSection("no-magic-zones." + node);

				// Check enabled
				if (!zoneConfig.getBoolean("enabled", true)) continue;

				// Get zone type
				String type = zoneConfig.getString("type", "");
				if (type.isEmpty()) {
					MagicSpells.error("Invalid no-magic zone type '" + type + "' on zone '" + node + '\'');
					continue;
				}

				Class<? extends NoMagicZone> clazz = zoneTypes.get(type);
				if (clazz == null) {
					MagicSpells.error("Invalid no-magic zone type '" + type + "' on zone '" + node + '\'');
					continue;
				}

				// Create zone
				NoMagicZone zone;
				try {
					zone = clazz.newInstance();
				} catch (Exception e) {
					MagicSpells.error("Failed to create no-magic zone '" + node + '\'');
					e.printStackTrace();
					continue;
				}
				zone.create(node, zoneConfig);
				zones.put(node, zone);
				zonesOrdered.add(zone);
				MagicSpells.debug(3, "Loaded no-magic zone: " + node);
			}
		}

		MagicSpells.debug(1, "No-magic zones loaded: " + zones.size());
	}

	public boolean willFizzle(Player player, Spell spell) {
		return willFizzle(player.getLocation(), spell);
	}

	public boolean willFizzle(Location location, Spell spell) {
		if (zonesOrdered == null || zonesOrdered.isEmpty()) return false;
		for (NoMagicZone zone : zonesOrdered) {
			if (zone == null) return false;
			ZoneCheckResult result = zone.check(location, spell);
			if (result == ZoneCheckResult.DENY) return true;
			if (result == ZoneCheckResult.ALLOW) return false;
		}
		return false;
	}

	public boolean inZone(Player player, String zoneName) {
		return inZone(player.getLocation(), zoneName);
	}

	public boolean inZone(Location loc, String zoneName) {
		NoMagicZone zone = zones.get(zoneName);
		return zone != null && zone.inZone(loc);
	}

	public void sendNoMagicMessage(Player player, Spell spell) {
		for (NoMagicZone zone : zonesOrdered) {
			ZoneCheckResult result = zone.check(player.getLocation(), spell);
			if (result != ZoneCheckResult.DENY) continue;
			MagicSpells.sendMessage(zone.getMessage(), player, null);
			return;
		}
	}

	public int zoneCount() {
		return zones.size();
	}

	public void addZoneType(String name, Class<? extends NoMagicZone> clazz) {
		zoneTypes.put(name, clazz);
	}

	public void turnOff() {
		zoneTypes.clear();
		zones.clear();
		zoneTypes = null;
		zones = null;
	}

}

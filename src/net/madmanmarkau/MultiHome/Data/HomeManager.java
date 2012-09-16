package net.madmanmarkau.MultiHome.Data;

import java.util.ArrayList;
import java.util.Iterator;

import net.madmanmarkau.MultiHome.Messaging;
import net.madmanmarkau.MultiHome.MultiHome;
import net.madmanmarkau.MultiHome.Settings;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import com.palmergames.bukkit.towny.object.Coord;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownyUniverse;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.Towny;

/**
 * Base class for home location database objects.
 * @author MadManMarkAu
 */
public abstract class HomeManager {
	protected final MultiHome plugin;
	
	/**
	 * @param plugin The plug-in.
	 */
	public HomeManager(MultiHome plugin) {
		this.plugin = plugin;
	}

	/**
	 * Deletes all homes from the database.
	 */
	abstract public void clearHomes();

	/**
	 * Returns a HomeEntry object for the specified home. If home is not found, returns null. 
	 * @param player Owner of the home.
	 * @param name Name of the owner's home location.
	 */
	public final HomeEntry getHome(Player player, String name) {
		return this.getHome(player.getName(), name);
	}

	/**
	 * Returns a HomeEntry object for the specified home. If home is not found, returns null. 
	 * @param player Owner of the home.
	 * @param name Name of the owner's home location.
	 */
	abstract public HomeEntry getHome(String player, String name);

	/**
	 * Adds the home location for the specified player. If home location already exists, updates the location.
	 * @param player Owner of the home.
	 * @param name Name of the owner's home.
	 * @param location Location the home.
	 */
	public final void addHome(Player player, String name, Location location) {
		this.addHome(player.getName(), name, location);
	}
	
	/**
	 * Adds the home location for the specified player. If home location already exists, updates the location.
	 * @param player Owner of the home.
	 * @param name Name of the owner's home.
	 * @param location Location the home.
	 */
	abstract public void addHome(String player, String name, Location location);

	/**
	 * Remove an existing home.
	 * @param player Owner of the home.
	 * @param name Name of the owner's home location.
	 */
	public final void removeHome(Player player, String name) {
		this.removeHome(player.getName(), name);
	}
	
	/**
	 * Remove an existing home.
	 * @param player Owner of the home.
	 * @param name Name of the owner's home location.
	 */
	abstract public void removeHome(String player, String name);
	
	/**
	 * Check the home database for a player.
	 * @param player Player to check database for.
	 * @return boolean True if player exists in database, otherwise false.
	 */
	public final boolean getUserExists(Player player) {
		return this.getUserExists(player.getName());
	}
	
	/**
	 * Check the home database for a player.
	 * @param player Player to check database for.
	 * @return boolean True if player exists in database, otherwise false.
	 */
	abstract public boolean getUserExists(String player);

	/**
	 * Get the number of homes a player has set.
	 * @param player Player to check home list for.
	 * @return int Number of home locations set.
	 */
	public final int getUserHomeCount(Player player) {
		return this.getUserHomeCount(player.getName());
	}

	/**
	 * Get the number of homes a player has set.
	 * @param player Player to check home list for.
	 * @return int Number of home locations set.
	 */
	abstract public int getUserHomeCount(String player);
	
	/**
	 * Retrieve a list of player home locations from the database. If player not found, returns a blank list.
	 * @param player Player to retrieve home list for.
	 * @return ArrayList<HomeEntry> List of home locations.
	 */
	public final ArrayList<HomeEntry> listUserHomes(Player player) {
		return this.listUserHomes(player.getName());
	}
	
	/**
	 * Retrieve a list of player home locations from the database. If player not found, returns a blank list.
	 * @param player Player to retrieve home list for.
	 * @return ArrayList<HomeEntry> List of home locations.
	 */
	abstract public ArrayList<HomeEntry> listUserHomes(String player);
	
	/**
	 * Imports the list of home locations passed. Does not overwrite existing home locations.
	 * @param homes List of players and homes to import.
	 * @param overwrite True to overwrite existing entries.
	 */
	abstract public void importHomes(ArrayList<HomeEntry> homes, boolean overwrite);
	

	/*
	 * JOREN
	 */

	/**
	 * If the home is inside of a denied region, returns false
	 */

	public boolean validHomeRegion(Player player, String name) {
		HomeEntry home = getHome(player, name);

		if (home == null)
			return false;

		World world = plugin.getServer().getWorld(home.getWorld());
		if (world == null)
			return false;

		WorldGuardPlugin wg = plugin.getWorldGuard();
		if (wg != null)
		{
			Location loc = home.getHomeLocation(plugin.getServer()); // This also takes a location
			LocalPlayer lp = wg.wrapPlayer(player);

			RegionManager regionManager = wg.getRegionManager(world);
			ApplicableRegionSet set = regionManager.getApplicableRegions(loc);
			if (set.isMemberOfAll(lp))
				return true; //if they are members of a region, they can use a /home to go there and ignore this check.
			for (Iterator<ProtectedRegion> i = set.iterator(); i.hasNext();)
			{
				ProtectedRegion pr = i.next();
				if (Settings.isRegionBlocked(home.getWorld(), pr.getId()))
				{
					Messaging.logInfo("Player's home " + name + " was rejected; it is inside of denied region " + pr.getId(), plugin);
					return false;
				}
			}
		}
		return true;
	}

	public boolean validHomeTowny(Player player, String name) {
		HomeEntry home = getHome(player, name);

		if (home == null)
			return false;

		Towny towny = plugin.getTowny();

		if (towny != null)
		{
			Location loc = home.getHomeLocation(plugin.getServer());
			Coord c = Coord.parseCoord(loc);
			try{
				towny.getTownyUniverse();
				WorldCoord wc = new WorldCoord(TownyUniverse.getWorld(player.getWorld().getName()), c);
				TownBlock tb = wc.getTownBlock();
				Town t = tb.getTown();
				if (t.hasResident(player.getName()))
					return true; // Members of a town can home to any plot in the town
				else
				{
					Resident owner;
					try{
						owner = tb.getResident();
					} catch (Exception e)
					{//Actually NotRegisteredException, but can't have the dependency or missing Towny jar will kill this plugin
						owner = t.getMayor(); // If plot is not sold, treat the mayor as owner
					}
					Resident homer = towny.getTownyUniverse().getResident(player.getName());
					if (owner.hasFriend(homer)||owner.equals(homer))
						return true; // Either the person is friends with or IS the owner. Yes, an owner should always be a member of the town, but they may choose to change that someday?
					Messaging.logInfo("Player's home " + name + " was rejected; they are not a resident of " + t.getName() + " nor are they friends with plot owner " + owner.getName(), plugin);
					return false;
				}
			}
			catch (Exception e)
			{//Actually NotRegisteredException, but can't have the dependency or missing Towny jar will kill this plugin
				return true; // Assuming plot is not part of Towny
			}
		}
		return true;
	}

	/*
	 * /JOREN
	 */
}

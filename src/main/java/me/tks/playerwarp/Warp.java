package me.tks.playerwarp;

import com.google.gson.Gson;
import me.tks.messages.Messages;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The type Warp.
 */
public class Warp implements Serializable {

    private String name;
    private Location loc;
    private boolean isPrivate;
    private final List<String> trustedPlayers;
    private ItemStack guiItem;
    private final String owner;
    private final ArrayList<String> lore;
    private int visitors;

    /**
     * Creates a new warp object.
     *
     * @param name  The name of the warp
     * @param loc   The location of the warp
     * @param owner The owner
     */
    public Warp(String name, Location loc, Player owner) {

        this.name = name;
        this.loc = loc;
        this.isPrivate = false;
        this.trustedPlayers = new ArrayList<>();
        this.guiItem = new ItemStack(Material.CONDUIT, 1);

        name = name.substring(0, 1).toUpperCase() + name.substring(1);

        ItemMeta meta = guiItem.getItemMeta();
        meta.setDisplayName(name);
        guiItem.setItemMeta(meta);

        this.owner = owner.getUniqueId().toString();
        this.lore = new ArrayList<>();
        this.visitors = 0;
    }

    public Warp(String name, Location loc, boolean isPrivate, List<String> trustedPlayers, ItemStack guiItem, String owner, ArrayList<String> lore, int visitors) {
        this.name = name;
        this.loc = loc;
        this.isPrivate = isPrivate;
        this.trustedPlayers = trustedPlayers;
        this.guiItem = guiItem;

        ItemMeta meta = guiItem.getItemMeta();
        meta.setDisplayName(name);
        guiItem.setItemMeta(meta);

        this.owner = owner;
        this.lore = lore;
        this.visitors = visitors;
    }

    /**
     * Creates a new warp.
     *
     * @param player The owner
     * @param name   The name of the warp
     * @param wL     The current warp list
     */
    public static void setWarp(Player player, String name, WarpList wL) {

        // TO-DO:
        // - GriefPrevention
        // - Blacklist
        // - Item/warpPrice
        // - WarpLimit

        if (wL.warpExists(name)) {
            player.sendMessage(ChatColor.RED + Messages.NAME_IN_USE.getMessage().replaceAll("PWARPNAMEP", name.toLowerCase()));
            return;
        }

        if (wL.ownsTooMany(player)) {
            player.sendMessage(ChatColor.RED + Messages.LIMIT_REACHED.getMessage().replaceAll("PLIMITP", "" + WarpList.getPersonalLimit(player)));
            return;
        }

        name = name.toLowerCase();

        wL.addWarp(new Warp(name, player.getLocation(), player));

        player.sendMessage(ChatColor.GREEN + Messages.CREATED_FREE_WARP.getMessage());
    }

    /**
     * Getter for the warp name.
     *
     * @return Warp name
     */
    public String getName() {
        return this.name;
    }

    /**
     * Change name.
     *
     * @param name the name
     */
    public void changeName(String name) {

        // Check if owner, warp exists etc


        this.name = name;
    }

    /**
     * Getter for visitors
     *
     * @return the current amount of visitors
     */
    public int getVisitors() {
        return this.visitors;
    }

    /**
     * Moves a warp to a new location
     * @param player player executing the command
     */
    public void move(Player player) {

        if (!isOwnerWithMessage(player)) return;

        this.loc = player.getLocation();
        player.sendMessage(ChatColor.GREEN + Messages.MOVED_WARP.getMessage());
    }

    /**
     * Sets the privacy state of a warp.
     *
     * @param isPrivate True private, false public
     */
    public void setPrivacyState(Player owner, boolean isPrivate) {

        if (!isOwnerWithMessage(owner)) return;

        this.isPrivate = isPrivate;

        if (isPrivate) {
            owner.sendMessage(ChatColor.GREEN + Messages.MADE_PRIVATE.getMessage());
        }
        else {
            owner.sendMessage(ChatColor.GREEN + Messages.MADE_PUBLIC.getMessage());
        }
    }

    /**
     * Adds a trusted player
     * @param owner Player who wants to trust
     * @param trusted Player to be trusted
     */
    public void addTrustedPlayer(Player owner, OfflinePlayer trusted) {

        if (!isOwnerWithMessage(owner)) return;

        if (trustedPlayers.contains(trusted.getUniqueId().toString())) {
            owner.sendMessage(ChatColor.RED + Messages.PLAYER_ALREADY_TRUSTED.getMessage());
            return;
        }

        this.trustedPlayers.add(trusted.getUniqueId().toString());
        owner.sendMessage(ChatColor.GREEN + Messages.PLAYER_TRUSTED.getMessage());
    }

    /**
     * Removes a trusted player.
     * @param owner Player who wants to untrust
     * @param untrusted Player to be untrusted
     */
    public void removeTrustedPlayer(Player owner, OfflinePlayer untrusted) {

        if (isOwnerWithMessage(owner)) return;

        if (!trustedPlayers.contains(untrusted.getUniqueId().toString())) {
            owner.sendMessage(ChatColor.RED + Messages.PLAYER_NOT_TRUSTED.getMessage());
            return;
        }

        this.trustedPlayers.remove(untrusted.getUniqueId().toString());
        owner.sendMessage(ChatColor.GREEN + Messages.PLAYER_UNTRUSTED.getMessage());
    }

    /**
     * Changes the lore of the current warp.
     *
     * @param line New line for the lore
     * @param row  Row of the line (1/3)
     */
    public void setLore(String line, int row) {

        if (row > 3 || row <= 0) {
            throw new IllegalArgumentException("Error: lore array out of bounds. Please notify the developer.");
        }

        row--;

        this.lore.set(row, line);

        //TO-DO Update itemStack
    }

    /**
     * Teleport a player to the current warp.
     *
     * @param player the player
     */
    public void goTo(Player player) {
        if (isTrusted(player)) {

            // Only increase visitors if it's not the warp owner
            if (!this.owner.equals(player.getUniqueId().toString())) {
                this.visitors++;
                // TO DO: UPDATE ITEMSTACK
            }

            player.teleport(this.loc);
            player.sendMessage(ChatColor.GREEN + Messages.TELEPORTED.getMessage());
        }
        else {
            player.sendMessage(ChatColor.RED + Messages.NOT_TRUSTED.getMessage());
        }
    }

    /**
     * Check if a player is trusted to a warp.
     *
     * @param player the player
     * @return the boolean
     */
    public boolean isTrusted(Player player) {
        return this.trustedPlayers.contains(player) || isOwner(player);
    }

    /**
     * Check if player is owner or has an override permission.
     *
     * @param player the player
     * @return True if owner/permission, false if not
     */
    public boolean isOwner(Player player) {
        // Player is owner or has an override permission
        if (this.owner.equals(player.getUniqueId().toString()) || player.hasPermission("pwarp.manage")) {
            return true;
        }

        return false;
    }

    /**
     * Check if player is owner or has an override permission. Sends a message if player is not an owner.
     *
     * @param player the player
     * @return True if owner/permission, false if not
     */
    public boolean isOwnerWithMessage(Player player) {
        // Player is owner or has override permission
        if (this.owner.equals(player.getUniqueId().toString()) || player.hasPermission("pwarp.manage")) {
            return true;
        }

        // Notify player
        player.sendMessage(ChatColor.RED + Messages.NOT_AN_OWNER.getMessage());
        return false;
    }

    /**
     * Getter for warp ItemStack.
     *
     * @return the ItemStack
     */
    public ItemStack getItemStack() {
        return this.guiItem;
    }

    /**
     * Changes the ItemStack of the current warp to the one held by the player.
     * @param player the player
     */
    public void setItemStack(Player player) {

        if (!isOwnerWithMessage(player)) return;


        ItemStack newItem = player.getInventory().getItemInMainHand();
        player.getInventory().setItemInMainHand(newItem);

        if (newItem.getType().equals(Material.AIR)) {
            player.sendMessage(ChatColor.RED + Messages.HOLD_ITEM.getMessage());
            return;
        }

        this.getItemStack().setType(newItem.getType());

        PWarp.gC.updateItem(this);

        player.sendMessage(ChatColor.GREEN + Messages.CHANGED_WARP_ICON.getMessage());
    }

    /**
     * Converts the Warp object into json format to save.
     * @return A string containing the formatted object
     */
    public String toJson() {

        HashMap<String, Object> properties = new HashMap<>();

        Gson gson = new Gson();

        properties.put("name", name);
        properties.put("location", gson.toJson(loc.serialize()));
        properties.put("isPrivate", isPrivate);
        properties.put("trustedPlayers", trustedPlayers);
        properties.put("guiItem", gson.toJson(guiItem.serialize()));
        properties.put("owner", owner);
        properties.put("lore", lore);
        properties.put("visitors", visitors);

        return gson.toJson(properties);
    }

    /**
     * Gets a Warp object from a json format.
     * @param properties The string with json format
     * @return A new Warp object
     */
    public static Warp fromJson(String properties) {

        Gson gson = new Gson();

        HashMap<String, Object> map = gson.fromJson(properties, HashMap.class);

        String name = (String) map.get("name");
        Location loc = Location.deserialize(gson.fromJson((String) map.get("location"), Map.class));
        boolean isPrivate = (Boolean) map.get("isPrivate");
        List<String> trustedPlayers = (List<String>) map.get("trustedPlayers");
        ItemStack guiItem = ItemStack.deserialize(gson.fromJson((String) map.get("guiItem"), Map.class));
        String owner = (String) map.get("owner");
        ArrayList<String> lore = (ArrayList<String>) map.get("lore");
        double visitors = (double) map.get("visitors");

        return new Warp(name, loc, isPrivate, trustedPlayers, guiItem, owner, lore, (int) visitors);
    }

}
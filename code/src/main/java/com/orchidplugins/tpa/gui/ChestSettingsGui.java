package com.orchidplugins.tpa.gui;

import com.orchidplugins.managers.ConfigManager;
import com.orchidplugins.managers.TpaManager;
import com.orchidplugins.tpa.Request;
import com.orchidplugins.tpa.TpaMsg;
import com.orchidplugins.tpa.TpaUtil;
import com.orchidplugins.util.Msg;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Chest-inventory fallback for /tpsettings on clients that cannot render Paper dialogs
 * (Bedrock/Geyser or any client below 1.21.2 / protocol 768).
 */
public final class ChestSettingsGui implements Listener {

    private static final int PICKER_SIZE = 54;
    private static final int EDIT_SIZE = 27;
    private static final int PLAYERS_PER_PAGE = 36; // slots 9..44

    private static final String PICKER_TITLE = "Orchid TPA \u203A Select Player #%d";
    private static final String EDIT_PREFIX = "Orchid TPA \u203A Edit: ";
    private static final String FILLER = " ";

    private final MiniMessage mm = MiniMessage.miniMessage();
    private final ConfigManager config;
    private final TpaManager tpaManager;

    private final Map<UUID, List<UUID>> pagePlayers = new HashMap<>();
    private final Map<UUID, Integer> pages = new HashMap<>();
    private final Map<UUID, UUID> editTarget = new HashMap<>();

    public ChestSettingsGui(ConfigManager config, TpaManager tpaManager) {
        this.config = config;
        this.tpaManager = tpaManager;
    }

    /** True when the client can use the native dialog API. */
    public static boolean supportsDialog(Player player) {
        int protocol = player.getProtocolVersion();
        if (protocol < 768) {
            return false;
        }
        String brand = player.getClientBrandName();
        if (brand != null && brand.toLowerCase().contains("geyser")) {
            return false;
        }
        return !isFloodgatePlayer(player);
    }

    private static boolean isFloodgatePlayer(Player player) {
        try {
            Class<?> apiClass = Class.forName("org.geysermc.floodgate.api.FloodgateApi");
            Object api = apiClass.getMethod("getInstance").invoke(null);
            return (boolean) apiClass.getMethod("isFloodgatePlayer", UUID.class)
                    .invoke(api, player.getUniqueId());
        } catch (Throwable ignored) {
            return false;
        }
    }

    public void open(Player player) {
        openPicker(player);
    }

    /** Opens straight to the per-player edit screen for the given target. */
    public void openWithPlayer(Player player, UUID other) {
        openEdit(player, other);
    }

    private void openPicker(Player player) {
        List<Player> online = new ArrayList<>();
        for (Player p : player.getServer().getOnlinePlayers()) {
            if (!p.getUniqueId().equals(player.getUniqueId())) {
                online.add(p);
            }
        }
        List<UUID> uuids = online.stream().map(Player::getUniqueId).toList();

        int maxPage = Math.max(0, (uuids.size() - 1) / PLAYERS_PER_PAGE);
        int page = Math.min(pages.getOrDefault(player.getUniqueId(), 0), maxPage);
        pages.put(player.getUniqueId(), page);
        pagePlayers.put(player.getUniqueId(), uuids);

        Inventory inv = Bukkit.createInventory(null, PICKER_SIZE, PICKER_TITLE.formatted(page + 1));
        for (int i = 0; i < PICKER_SIZE; i++) {
            inv.setItem(i, filler());
        }
        int start = page * PLAYERS_PER_PAGE;
        int end = Math.min(start + PLAYERS_PER_PAGE, uuids.size());
        int slot = 9;
        for (int i = start; i < end; i++, slot++) {
            UUID uuid = uuids.get(i);
            Player target = Bukkit.getPlayer(uuid);
            boolean friend = tpaManager.isFriend(player.getUniqueId(), uuid);
            String name = target != null ? target.getName() : TpaUtil.nameOf(uuid);
            ItemStack head = skull(target);
            ItemMeta meta = head.getItemMeta();
            meta.displayName(mm.deserialize(name));
            List<Component> lore = new ArrayList<>();
            if (friend) {
                lore.add(mm.deserialize("<gold>\u2661 Friend</gold>"));
            }
            lore.add(mm.deserialize("<gray>Click to edit.</gray>"));
            meta.lore(lore);
            head.setItemMeta(meta);
            inv.setItem(slot, head);
        }
        inv.setItem(45, named(Material.ARROW, "<gray>\u2190 Prev", "<gray>Previous page."));
        inv.setItem(49, named(Material.NAME_TAG,
                "<color:#A855F7>\u270E Offline player</color>",
                "<gray>Use <yellow>/tpsettings <name> <gray>to edit an offline player."));
        inv.setItem(52, named(Material.ARROW, "<gray><b>\u2192 Next</b></gray>", "<gray>Next page."));
        inv.setItem(53, named(Material.BARRIER, "<red>Close", "<gray>Close this menu."));
        player.openInventory(inv);
    }

    private void openEdit(Player player, UUID other) {
        editTarget.put(player.getUniqueId(), other);
        TpaManager.PlayerSettings otherSettings = tpaManager.get(other);
        boolean tpa = otherSettings.getTpa() != null
                ? otherSettings.getTpa() : tpaManager.isGlobalOn(Request.Type.TPA);
        boolean tpahere = otherSettings.getTpaHere() != null
                ? otherSettings.getTpaHere() : tpaManager.isGlobalOn(Request.Type.TPAHERE);
        boolean isFriend = tpaManager.isFriend(player.getUniqueId(), other);
        String name = TpaUtil.nameOf(other);

        Inventory inv = Bukkit.createInventory(null, EDIT_SIZE, EDIT_PREFIX + name);
        for (int i = 0; i < EDIT_SIZE; i++) {
            if (i % 9 == 0 || i % 9 == 8 || i / 9 == 0 || i / 9 == 2) {
                inv.setItem(i, filler());
            }
        }
        inv.setItem(10, perPlayerToggle("TPA", tpa, otherSettings.getTpa() != null));
        inv.setItem(12, perPlayerToggle("TPAHERE", tpahere, otherSettings.getTpaHere() != null));
        inv.setItem(14, isFriend
                ? named(Material.HEART_OF_THE_SEA, "<gold><b>\u2665 Remove friend</b></gold>",
                        "<gray>Remove " + name + " from your friends.")
                : named(Material.NAME_TAG, "<color:#A855F7>\u2605 Add friend</color>",
                        "<gray>Use <yellow>/friend add " + name + "<gray> to send a friend request."));
        inv.setItem(22, named(Material.BARRIER, "<red>Back", "<gray>Back to player list."));
        player.openInventory(inv);
    }

    // ---------- Click handling ----------

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        int raw = event.getRawSlot();
        int topSize = event.getView().getTopInventory().getSize();
        if (raw < 0 || raw >= topSize) {
            return;
        }
        event.setCancelled(true);
        String title = event.getView().getTitle();
        if (isPickerTitle(title)) {
            handlePickerClick(player, raw);
        } else if (title.startsWith(EDIT_PREFIX)) {
            handleEditClick(player, raw);
        }
    }

    private void handlePickerClick(Player player, int raw) {
        if (raw == 45) {
            int page = Math.max(0, pages.getOrDefault(player.getUniqueId(), 0) - 1);
            pages.put(player.getUniqueId(), page);
            openPicker(player);
            return;
        }
        if (raw == 52) {
            List<UUID> uuids = pagePlayers.getOrDefault(player.getUniqueId(), List.of());
            int maxPage = Math.max(0, (uuids.size() - 1) / PLAYERS_PER_PAGE);
            int page = Math.min(pages.getOrDefault(player.getUniqueId(), 0) + 1, maxPage);
            pages.put(player.getUniqueId(), page);
            openPicker(player);
            return;
        }
        if (raw == 53) {
            player.closeInventory();
            return;
        }
        if (raw >= 9 && raw < 45) {
            int idx = raw - 9;
            List<UUID> uuids = pagePlayers.getOrDefault(player.getUniqueId(), List.of());
            int absIdx = pages.getOrDefault(player.getUniqueId(), 0) * PLAYERS_PER_PAGE + idx;
            if (absIdx >= 0 && absIdx < uuids.size()) {
                openEdit(player, uuids.get(absIdx));
            }
        }
    }

    private void handleEditClick(Player player, int raw) {
        UUID other = editTarget.get(player.getUniqueId());
        if (other == null) {
            openPicker(player);
            return;
        }
        String name = TpaUtil.nameOf(other);
        TpaManager.PlayerSettings settings = tpaManager.get(other);
        switch (raw) {
            case 10 -> {
                boolean next = !(settings.getTpa() != null
                        ? settings.getTpa() : tpaManager.isGlobalOn(Request.Type.TPA));
                settings.setTpa(next);
                TpaMsg.send(config, player, "<green>TPA for <yellow>" + name
                        + " <green>set to <bold>" + (next ? "<green>ON" : "<red>OFF") + "</bold><green>.");
                openEdit(player, other);
            }
            case 12 -> {
                boolean next = !(settings.getTpaHere() != null
                        ? settings.getTpaHere() : tpaManager.isGlobalOn(Request.Type.TPAHERE));
                settings.setTpaHere(next);
                TpaMsg.send(config, player, "<green>TPAHERE for <yellow>" + name
                        + " <green>set to <bold>" + (next ? "<green>ON" : "<red>OFF") + "</bold><green>.");
                openEdit(player, other);
            }
            case 14 -> {
                if (tpaManager.isFriend(player.getUniqueId(), other)) {
                    tpaManager.removeFriendship(player.getUniqueId(), other);
                    TpaMsg.sendFriend(config, player, "<green>Removed <yellow>" + name
                            + " <green>from your friends.");
                } else {
                    TpaMsg.sendFriend(config, player, "<red>Use <yellow>/friend add " + name
                            + " <red>to send a friend request.");
                }
                openEdit(player, other);
            }
            case 22 -> openPicker(player);
            default -> {
            }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        pagePlayers.remove(uuid);
        pages.remove(uuid);
        editTarget.remove(uuid);
    }

    // ---------- Item helpers ----------

    private ItemStack toggleItem(String label, boolean on) {
        return on
                ? named(Material.EMERALD, "<green><b>" + label + " \u2714 ON</b></green>",
                        "<gray>Click to turn <red>OFF<gray>.")
                : named(Material.REDSTONE_BLOCK, "<red><b>" + label + " \u2716 OFF</b></red>",
                        "<gray>Click to turn <green>ON<gray>.");
    }

    private ItemStack perPlayerToggle(String label, boolean effective, boolean hasOverride) {
        ItemStack item = toggleItem(label, effective);
        ItemMeta meta = item.getItemMeta();
        List<Component> lore = new ArrayList<>();
        if (meta.lore() != null) {
            lore.addAll(meta.lore());
        }
        lore.add(mm.deserialize(hasOverride
                ? "<gray>(stored override)"
                : "<gray>(inherits global setting)"));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack skull(Player target) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (target != null) {
            try {
                meta.setPlayerProfile(target.getPlayerProfile());
            } catch (Throwable ignored) {
            }
        }
        head.setItemMeta(meta);
        return head;
    }

    private ItemStack named(Material material, String name, String lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(mm.deserialize(name));
        meta.lore(List.of(mm.deserialize(lore)));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack filler() {
        return named(Material.GRAY_STAINED_GLASS_PANE, FILLER, FILLER);
    }

    private boolean isPickerTitle(String title) {
        int hash = title.lastIndexOf('#');
        if (hash < 0 || !title.startsWith("Orchid TPA \u203A Select Player ")) {
            return false;
        }
        try {
            Integer.parseInt(title.substring(hash + 1));
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
package com.orchidplugins.tpa.gui;

import com.orchidplugins.managers.ConfigManager;
import com.orchidplugins.managers.TpaManager;
import com.orchidplugins.tpa.Request;
import com.orchidplugins.tpa.TpaMsg;
import com.orchidplugins.tpa.TpaUtil;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SettingsGui {

    private static final Duration CALLBACK_LIFETIME = Duration.ofHours(1L);
    private static final String PURPLE = "<color:#A855F7>";

    private final MiniMessage mm = MiniMessage.miniMessage();
    private final ConfigManager config;
    private final TpaManager tpaManager;

    public SettingsGui(ConfigManager config, TpaManager tpaManager) {
        this.config = config;
        this.tpaManager = tpaManager;
    }

    public void open(Player player) {
        player.showDialog(hubDialog(player));
    }

    /** Opens straight to the per-player edit dialog for the given target. */
    public void openWithPlayer(Player player, UUID other, String name) {
        player.showDialog(editDialog(player, other, name));
    }

    private ClickCallback.Options callbackOptions() {
        return ClickCallback.Options.builder()
                .uses(1)
                .lifetime(CALLBACK_LIFETIME)
                .build();
    }

    private Component mm(String mini) {
        return mm.deserialize(mini);
    }

    // ---------- Hub ----------

    private Dialog hubDialog(Player player) {
        TpaManager.PlayerSettings settings = tpaManager.get(player.getUniqueId());
        boolean globalTpa = tpaManager.isGlobalOn(Request.Type.TPA);
        boolean globalTpaHere = tpaManager.isGlobalOn(Request.Type.TPAHERE);

        java.util.Map<String, String> colors = new java.util.HashMap<>();
        colors.put("purple", PURPLE);

        List<ActionButton> buttons = new ArrayList<>();
        buttons.add(ActionButton.create(
                mm("<green><b>✔ Save</b></green>"), mm("<gray>Apply the toggles above.</gray>"), 60,
                DialogAction.customClick((view, audience) -> {
                    if (!(audience instanceof Player target)) {
                        return;
                    }
                    Boolean globalTpaVal = view.getBoolean("global_tpa");
                    Boolean globalTpaHereVal = view.getBoolean("global_tpahere");
                    Boolean autoTpa = view.getBoolean("auto_tpa");
                    Boolean autoTpaHere = view.getBoolean("auto_tpahere");
                    TpaManager.PlayerSettings current = tpaManager.get(target.getUniqueId());
                    if (globalTpaVal != null) {
                        tpaManager.setGlobal(Request.Type.TPA, globalTpaVal);
                    }
                    if (globalTpaHereVal != null) {
                        tpaManager.setGlobal(Request.Type.TPAHERE, globalTpaHereVal);
                    }
                    if (autoTpa != null) {
                        current.setAutoTpa(autoTpa);
                    }
                    if (autoTpaHere != null) {
                        current.setAutoTpaHere(autoTpaHere);
                    }
                    TpaMsg.send(config, target, "<green>TP settings saved.");
                    open(target);
                }, callbackOptions())));
        buttons.add(ActionButton.create(
                mm("<b><color:#A855F7>[ ✎ Edit a player… ]</color></b>"),
                mm("<gray>Pick someone to change their TPA/TPAHERE permission with you.</gray>"), 90,
                DialogAction.customClick((view, audience) -> {
                    if (audience instanceof Player target) {
                        openPicker(target);
                    }
                }, callbackOptions())));

        return Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(mm("<b>" + PURPLE + "TPA SETTINGS</b>"))
                        .inputs(List.of(
                                DialogInput.bool("global_tpa", mm("Accept Incoming TPA (Global)"))
                                        .initial(globalTpa)
                                        .onTrue("✔ ON")
                                        .onFalse("✖ OFF")
                                        .build(),
                                DialogInput.bool("global_tpahere", mm("Accept Incoming TPAHERE (Global)"))
                                        .initial(globalTpaHere)
                                        .onTrue("✔ ON")
                                        .onFalse("✖ OFF")
                                        .build(),
                                DialogInput.bool("auto_tpa", mm("Auto-accept TPA"))
                                        .initial(settings.isAutoTpa())
                                        .onTrue("✔ ON")
                                        .onFalse("✖ OFF")
                                        .build(),
                                DialogInput.bool("auto_tpahere", mm("Auto-accept TPAHERE"))
                                        .initial(settings.isAutoTpaHere())
                                        .onTrue("✔ ON")
                                        .onFalse("✖ OFF")
                                        .build()))
                        .build())
                .type(DialogType.multiAction(buttons)
                        .exitAction(ActionButton.create(mm("<dark_gray>Close</dark_gray>"),
                                mm("<gray>Close this menu.</gray>"), 40, null))
                        .columns(1)
                        .build()));
    }

    // ---------- Player picker (offline textbox on top, online grid below) ----------

    private Dialog pickerDialog(Player player) {
        List<ActionButton> buttons = new ArrayList<>();
        buttons.add(ActionButton.create(
                mm("<b><color:#A855F7>[ ⇨ Pick Offline Name ]</color></b>"),
                mm("<gray>Type a name in the box above to edit an offline player.</gray>"), 90,
                DialogAction.customClick((view, audience) -> {
                    if (!(audience instanceof Player target)) {
                        return;
                    }
                    String name = view.getText("offline_name");
                    if (name == null || name.isBlank()) {
                        TpaMsg.send(config, target, "<red>Type a player name in the box first.");
                        openPicker(target);
                        return;
                    }
                    UUID uuid = TpaUtil.resolve(name.trim());
                    if (uuid == null) {
                        TpaMsg.send(config, target, "<red>Player <yellow>" + name.trim() + " <red>not found.");
                        openPicker(target);
                        return;
                    }
                    openEdit(target, uuid);
                }, callbackOptions())));

        List<Player> online = new ArrayList<>();
        for (Player p : player.getServer().getOnlinePlayers()) {
            if (!p.getUniqueId().equals(player.getUniqueId())) {
                online.add(p);
            }
        }
        for (Player p : online) {
            boolean friend = tpaManager.isFriend(player.getUniqueId(), p.getUniqueId());
            String icon = friend ? "<gold>✦</gold> " : "";
            buttons.add(ActionButton.create(
                    mm((friend ? "<gold><b>" + p.getName() + "</b></gold>" : "<white>" + p.getName())),
                    mm("<gray>Edit TPA/TPAHERE settings with " + p.getName() + ".</gray>"), 90,
                    DialogAction.customClick((view, audience) -> {
                        if (audience instanceof Player target) {
                            openEdit(target, p.getUniqueId());
                        }
                    }, callbackOptions())));
        }

        return Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(mm("<b>" + PURPLE + "SELECT A PLAYER</b>"))
                        .inputs(List.of(DialogInput.text("offline_name", mm("<gray>Type a name… (offline)</gray>"))
                                .width(220)
                                .maxLength(16)
                                .initial("")
                                .build()))
                        .build())
                .type(DialogType.multiAction(buttons)
                        .exitAction(backButton(player))
                        .columns(6)
                        .build()));
    }

    // ---------- Per-player edit ----------

    private Dialog editDialog(Player owner, UUID other) {
        return editDialog(owner, other, TpaUtil.nameOf(other));
    }

    private Dialog editDialog(Player owner, UUID other, String name) {
        TpaManager.PlayerSettings settings = tpaManager.get(owner.getUniqueId());
        boolean tpa = settings.getTpa() != null ? settings.getTpa() : tpaManager.isGlobalOn(Request.Type.TPA);
        boolean tpahere = settings.getTpaHere() != null ? settings.getTpaHere() : tpaManager.isGlobalOn(Request.Type.TPAHERE);
        boolean isFriend = tpaManager.isFriend(owner.getUniqueId(), other);

        List<ActionButton> buttons = new ArrayList<>();
        buttons.add(ActionButton.create(
                mm("<green><b>✔ Save</b></green>"), mm("<gray>Apply and go back.</gray>"), 60,
                DialogAction.customClick((view, audience) -> {
                    if (!(audience instanceof Player target)) {
                        return;
                    }
                    Boolean tpaVal = view.getBoolean("tpa_with");
                    Boolean tpahereVal = view.getBoolean("tpahere_with");
                    TpaManager.PlayerSettings current = tpaManager.get(target.getUniqueId());
                    if (tpaVal != null) {
                        current.setTpa(tpaVal);
                    }
                    if (tpahereVal != null) {
                        current.setTpaHere(tpahereVal);
                    }
                    TpaMsg.send(config, target, "<green>Updated settings with <yellow>" + name + "<green>.");
                    openPicker(target);
                }, callbackOptions())));
        buttons.add(ActionButton.create(
                mm("<b><color:#A855F7>[ ⭐ Add/Remove Friend ]</color></b>"),
                mm("<gray>" + (isFriend ? "Remove" : "Add") + " " + name + " as a friend.</gray>"), 90,
                DialogAction.customClick((view, audience) -> {
                    if (!(audience instanceof Player target)) {
                        return;
                    }
                    UUID targetId = other;
                    if (isFriend) {
                        tpaManager.removeFriendship(target.getUniqueId(), targetId);
                        TpaMsg.sendFriend(config, target, "<green>Removed <yellow>" + name
                                + " <green>from your friends.");
                    } else {
                        TpaMsg.sendFriend(config, target, "<red>Use <yellow>/friend add " + name
                                + " <red>to send a friend request.");
                    }
                    openEdit(target, other);
                }, callbackOptions())));

        return Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(mm("<b>" + PURPLE + "EDIT PLAYER: " + name + "</b>"))
                        .inputs(List.of(
                                DialogInput.bool("tpa_with", mm("TPA with " + name))
                                        .initial(tpa)
                                        .onTrue("✔ ON")
                                        .onFalse("✖ OFF")
                                        .build(),
                                DialogInput.bool("tpahere_with", mm("TPAHERE with " + name))
                                        .initial(tpahere)
                                        .onTrue("✔ ON")
                                        .onFalse("✖ OFF")
                                        .build()))
                        .build())
                .type(DialogType.multiAction(buttons)
                        .exitAction(pickerBackButton(owner))
                        .columns(1)
                        .build()));
    }

    private ActionButton backButton(Player player) {
        return ActionButton.create(mm("<gray>▴ Back</gray>"), mm("<gray>Back.</gray>"), 50,
                DialogAction.customClick((view, audience) -> {
                    if (audience instanceof Player target) {
                        open(target);
                    }
                }, callbackOptions()));
    }

    private ActionButton pickerBackButton(Player player) {
        return ActionButton.create(mm("<gray>▴ Back</gray>"), mm("<gray>Back to player list.</gray>"), 50,
                DialogAction.customClick((view, audience) -> {
                    if (audience instanceof Player target) {
                        openPicker(target);
                    }
                }, callbackOptions()));
    }

    private void openPicker(Player player) {
        player.showDialog(pickerDialog(player));
    }

    private void openEdit(Player player, UUID other) {
        player.showDialog(editDialog(player, other));
    }
}
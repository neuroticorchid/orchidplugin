package com.orchidplugins.commands;

import com.orchidplugins.db.SqliteDatabase;
import com.orchidplugins.managers.WebhookManager;
import com.orchidplugins.util.Msg;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public final class OrchidPluginsCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = Arrays.asList(
            "help", "version", "abuselog", "webhook-test");

    private final Plugin plugin;
    private final SqliteDatabase database;
    private final WebhookManager webhook;

    public OrchidPluginsCommand(Plugin plugin, SqliteDatabase database, WebhookManager webhook) {
        this.plugin = plugin;
        this.database = database;
        this.webhook = webhook;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        String sub = args.length == 0 ? "help" : args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "version":
            case "v":
                Msg.send(sender, "<gold><bold>OrchidPlugins</bold></gold> <gray>v" + plugin.getPluginMeta().getVersion());
                Msg.send(sender, "<gray>All-in-one plugin - rolls, giveaways, death game, announcements, bounty, moderation and admin abuse.");
                return true;
            case "abuselog":
            case "log":
                return handleAbuseLog(sender, args);
            case "webhook-test":
            case "testhook":
                if (!sender.hasPermission("orchid.discord.test")) {
                    Msg.send(sender, "<red>You don't have permission to use this command.");
                    return true;
                }
                webhook.sendTest(sender);
                return true;
            case "help":
            default:
                sendHelp(sender);
                return true;
        }
    }

    private boolean handleAbuseLog(CommandSender sender, String[] args) {
        if (!sender.hasPermission("orchid.adminabuse.log")) {
            Msg.send(sender, "<red>You don't have permission to use this command.");
            return true;
        }
        String filter = args.length >= 2 ? args[1] : null;
        int limit = 15;
        if (args.length >= 3) {
            try {
                limit = Math.max(1, Math.min(50, Integer.parseInt(args[2])));
            } catch (NumberFormatException e) {
                Msg.send(sender, "<red>Invalid limit '<yellow>" + args[2] + "<red>' - use a number (1-50).");
                return true;
            }
        }
        database.queryAbuse(filter, limit, rows -> {
            if (rows.isEmpty()) {
                Msg.send(sender, "<gray>No admin abuse log entries found"
                        + (filter == null ? "." : (" for '<white>" + filter + "<gray>'.")));
                return;
            }
            Msg.send(sender, "<gold><bold>Recent /adminabuse executions"
                    + (filter == null ? "" : (" for <white>" + filter))
                    + " <gray>(latest " + rows.size() + "):</bold></gold>");
            for (String[] row : rows) {
                Msg.send(sender, "<gray>" + row[4] + " <dark_gray>| <yellow>" + row[0]
                        + " <dark_gray>-> <yellow>" + row[1]
                        + " <dark_gray>|<green> " + row[2]
                        + (row[3] == null || row[3].isEmpty() ? "" : " <gray>(" + row[3] + ")"));
            }
        });
        return true;
    }

    private void sendHelp(CommandSender sender) {
        Msg.send(sender, "<gold><bold>OrchidPlugins Help</bold></gold>");
        Msg.send(sender, "<green>- Fun ----------");
        Msg.send(sender, "<yellow>/rolldice <gray>- randomly pick a lucky player (animated titles)");
        Msg.send(sender, "<green>- Giveaways -----");
        Msg.send(sender, "<yellow>/react <phrase> <gray>- start a chat-reaction giveaway");
        Msg.send(sender, "<yellow>/endreact <gray>- end the giveaway and roll a winner");
        Msg.send(sender, "<green>- Polls ---------");
        Msg.send(sender, "<yellow>/poll <question> <gray>- defaults to <aqua>YES<gray>/<red>NO");
        Msg.send(sender, "<yellow>/poll <question> [choice1] [choice2] <gray>- bracket choices");
        Msg.send(sender, "<yellow>/poll <question> | ch1 | ch2 <gray>- pipe choices");
        Msg.send(sender, "<yellow>/endpoll <gray>- end the poll early and show results");
        Msg.send(sender, "<green>- Death Game -----");
        Msg.send(sender, "<yellow>/stopmoving [-die|-gay] <gray>- start red light green light");
        Msg.send(sender, "<gray>    -die = executer dies instantly, -gay = marked gay (near-only glow)");
        Msg.send(sender, "<yellow>/stopmoving stop <gray>- end the game");
        Msg.send(sender, "<yellow>/vote die|gay <gray>- vote in a normal round");
        Msg.send(sender, "<green>- Announcements --");
        Msg.send(sender, "<yellow>/sannounce <msg> <gray>- full-screen announcement");
        Msg.send(sender, "<yellow>/srestart [time|cancel] <gray>- restart countdown");
        Msg.send(sender, "<green>- Bounty ---------");
        Msg.send(sender, "<yellow>/rollbounty <gray>- put a random bounty on a player");
        Msg.send(sender, "<yellow>/checkbounty [player] <gray>- check a bounty");
        Msg.send(sender, "<green>- Moderation -----");
        Msg.send(sender, "<yellow>/warn <player> [duration] [reason]");
        Msg.send(sender, "<yellow>/unwarn <player> [count] [reason]");
        Msg.send(sender, "<yellow>/ban <player> [duration] [reason]");
        Msg.send(sender, "<yellow>/banip <player> [duration] [reason]");
        Msg.send(sender, "<yellow>/unban <name>");
        Msg.send(sender, "<yellow>/suspendstaff <player> [duration] [reason]");
        Msg.send(sender, "<yellow>/unsuspendstaff <player>");
        Msg.send(sender, "<green>- Admin Abuse -----");
        Msg.send(sender, "<yellow>/adminabuse (aa) <bigcharacter|reach|speed|jump|health|reset> [player] [value] [-b]");
        Msg.send(sender, "<gray>    -b = broadcast publicly (default is private). Every use is logged to the DB.");
        Msg.send(sender, "<green>- Plugin Info -----");
        Msg.send(sender, "<yellow>/orchidplugins abuselog [player] [limit] <gray>- browse the /aa audit log");
        Msg.send(sender, "<yellow>/orchidplugins webhook-test <gray>- send a test Discord embed");
        Msg.send(sender, "<yellow>/orchidplugins version <gray>- plugin info");
        Msg.send(sender, "<green>----------------");
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return match(args[0], SUBCOMMANDS);
        }
        return List.of();
    }

    private List<String> match(String input, List<String> options) {
        List<String> result = new ArrayList<>();
        for (String option : options) {
            if (option.startsWith(input.toLowerCase(Locale.ROOT))) {
                result.add(option);
            }
        }
        return result;
    }
}
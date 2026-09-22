package com.orchidplugins;

import com.orchidplugins.commands.AdminAbuseCommand;
import com.orchidplugins.commands.AnnounceCommand;
import com.orchidplugins.commands.BanCommand;
import com.orchidplugins.commands.BanIpCommand;
import com.orchidplugins.commands.CheckBountyCommand;
import com.orchidplugins.commands.DeathGameCommand;
import com.orchidplugins.commands.EndReactCommand;
import com.orchidplugins.commands.EndPollCommand;
import com.orchidplugins.commands.FAcceptCommand;
import com.orchidplugins.commands.FDenyCommand;
import com.orchidplugins.commands.FriendCommand;
import com.orchidplugins.commands.PollCommand;
import com.orchidplugins.commands.ReactCommand;
import com.orchidplugins.commands.RestartCommand;
import com.orchidplugins.commands.RollBountyCommand;
import com.orchidplugins.commands.RollDiceCommand;
import com.orchidplugins.commands.SmpCommand;
import com.orchidplugins.commands.StopMovingCommand;
import com.orchidplugins.commands.SuspendStaffCommand;
import com.orchidplugins.commands.TpAutoCommand;
import com.orchidplugins.commands.TpSettingsCommand;
import com.orchidplugins.commands.TpToggleCommand;
import com.orchidplugins.commands.TpaAcceptCommand;
import com.orchidplugins.commands.TpaCommand;
import com.orchidplugins.commands.TpaDenyCommand;
import com.orchidplugins.commands.TpaHereCommand;
import com.orchidplugins.commands.UnbanCommand;
import com.orchidplugins.commands.UnwarnCommand;
import com.orchidplugins.commands.VoteCommand;
import com.orchidplugins.commands.WarnCommand;
import com.orchidplugins.commands.OrchidPluginsCommand;
import com.orchidplugins.db.SqliteDatabase;
import com.orchidplugins.listeners.ChatListener;
import com.orchidplugins.listeners.DeathGameListener;
import com.orchidplugins.listeners.ModerationListener;
import com.orchidplugins.listeners.SmpListener;
import com.orchidplugins.listeners.TpListener;
import com.orchidplugins.managers.AttributeManager;
import com.orchidplugins.managers.BountyManager;
import com.orchidplugins.managers.ConfigManager;
import com.orchidplugins.managers.DeathGameManager;
import com.orchidplugins.managers.GiveawayManager;
import com.orchidplugins.managers.ModerationManager;
import com.orchidplugins.managers.PollManager;
import com.orchidplugins.managers.RestartManager;
import com.orchidplugins.managers.SmpManager;
import com.orchidplugins.managers.SuspensionManager;
import com.orchidplugins.managers.TpaManager;
import com.orchidplugins.managers.UpdateManager;
import com.orchidplugins.managers.WebhookManager;
import com.orchidplugins.tpa.FriendManager;
import com.orchidplugins.tpa.RequestManager;
import com.orchidplugins.tpa.TeleportService;
import com.orchidplugins.tpa.TrapReportManager;
import com.orchidplugins.tpa.gui.ChestSettingsGui;
import com.orchidplugins.tpa.gui.SettingsGui;
import com.orchidplugins.util.Msg;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public final class OrchidPlugins extends JavaPlugin {

    private static OrchidPlugins instance;

    private GiveawayManager giveawayManager;
    private DeathGameManager deathGameManager;
    private RestartManager restartManager;
    private SmpManager smpManager;
    private BountyManager bountyManager;
    private ModerationManager moderationManager;
    private SuspensionManager suspensionManager;
    private AttributeManager attributeManager;
    private ConfigManager configManager;
    private SqliteDatabase database;
    private WebhookManager webhookManager;
    private PollManager pollManager;
    private UpdateManager updateManager;
    private TpaManager tpaManager;
    private RequestManager tpaRequests;
    private FriendManager friendManager;
    private TrapReportManager trapReport;
    private SettingsGui settingsGui;
    private ChestSettingsGui chestSettingsGui;

    @Override
    public void onEnable() {
        instance = this;
        Msg.init(this);

        this.configManager = new ConfigManager(this);
        this.database = new SqliteDatabase(this, configManager.databaseFilename());
        this.webhookManager = new WebhookManager(this, configManager.getRaw());
        this.updateManager = new UpdateManager(this, configManager);
        this.attributeManager = new AttributeManager();
        this.giveawayManager = new GiveawayManager();
        this.deathGameManager = new DeathGameManager(this);
        this.restartManager = new RestartManager(this);
        this.smpManager = new SmpManager(this);
        this.bountyManager = new BountyManager(this, configManager, webhookManager);
        this.moderationManager = new ModerationManager(this);
        this.suspensionManager = new SuspensionManager(this, configManager, webhookManager);
        this.pollManager = new PollManager(this, configManager);
        this.tpaManager = new TpaManager(this);
        this.tpaRequests = new RequestManager(configManager);
        this.friendManager = new FriendManager(configManager.tpaRequestTtlSeconds() * 1000L);
        this.trapReport = new TrapReportManager(this, configManager.tpaTrapWindowSeconds() * 1000L);
        this.settingsGui = new SettingsGui(configManager, tpaManager);
        this.chestSettingsGui = new ChestSettingsGui(configManager, tpaManager);

        registerCommands();
        registerListeners();
        hookVault();
        updateManager.startupCheck();

        getServer().getScheduler().runTaskTimer(this,
                tpaRequests::cleanupExpired, 100L, 100L);
        getServer().getScheduler().runTaskTimer(this,
                trapReport::purgeExpired, 1200L, 600L);
        getServer().getScheduler().runTaskTimer(this,
                () -> friendManager.cleanupExpired((requester, target) ->
                        com.orchidplugins.tpa.TpaMsg.sendFriend(configManager,
                                getServer().getPlayer(requester),
                                "<red>Your friend request to <yellow>"
                                        + com.orchidplugins.tpa.TpaUtil.nameOf(target)
                                        + " <red>expired.")),
                100L, 100L);

        getLogger().info("OrchidPlugins enabled.");
    }

    @Override
    public void onDisable() {
        if (moderationManager != null) {
            moderationManager.flushNow();
        }
        if (database != null) {
            database.close();
        }
        if (pollManager != null) {
            pollManager.end();
        }
        if (bountyManager != null) {
            bountyManager.shutdownScheduler();
        }
        if (tpaManager != null) {
            tpaManager.save();
        }
        getLogger().info("OrchidPlugins disabled.");
    }

    private void registerCommands() {
        getCommand("rolldice").setExecutor(new RollDiceCommand(this));
        getCommand("react").setExecutor(new ReactCommand(giveawayManager));
        getCommand("endreact").setExecutor(new EndReactCommand(giveawayManager));
        getCommand("poll").setExecutor(new PollCommand(pollManager, configManager));
        getCommand("endpoll").setExecutor(new EndPollCommand(pollManager));
        getCommand("deathgame").setExecutor(new DeathGameCommand(deathGameManager));
        getCommand("stopmoving").setExecutor(new StopMovingCommand(deathGameManager));
        getCommand("vote").setExecutor(new VoteCommand(deathGameManager));
        getCommand("sannounce").setExecutor(new AnnounceCommand());
        getCommand("srestart").setExecutor(new RestartCommand(restartManager, configManager));
        SmpCommand smp = new SmpCommand(smpManager, configManager);
        getCommand("smp").setExecutor(smp);
        getCommand("smp").setTabCompleter(smp);
        getCommand("rollbounty").setExecutor(new RollBountyCommand(this, bountyManager));
        getCommand("checkbounty").setExecutor(new CheckBountyCommand(bountyManager));
        getCommand("warn").setExecutor(new WarnCommand(moderationManager, configManager, webhookManager));
        getCommand("unwarn").setExecutor(new UnwarnCommand(moderationManager, configManager, webhookManager));
        getCommand("ban").setExecutor(new BanCommand(moderationManager, configManager, webhookManager));
        getCommand("banip").setExecutor(new BanIpCommand(moderationManager, configManager, webhookManager));
        getCommand("unban").setExecutor(new UnbanCommand(moderationManager, configManager, webhookManager));
        getCommand("suspendstaff").setExecutor(new SuspendStaffCommand(this));
        getCommand("unsuspendstaff").setExecutor(new SuspendStaffCommand(this));
        OrchidPluginsCommand orchid = new OrchidPluginsCommand(this, configManager, database, webhookManager, updateManager);
        getCommand("orchidplugins").setExecutor(orchid);
        getCommand("orchidplugins").setTabCompleter(orchid);
        AdminAbuseCommand adminAbuse = new AdminAbuseCommand(attributeManager, configManager, this, database, webhookManager);
        getCommand("adminabuse").setExecutor(adminAbuse);
        getCommand("adminabuse").setTabCompleter(adminAbuse);
        TeleportService teleportService = new TeleportService(this, configManager, trapReport);
        TpaCommand tpa = new TpaCommand(configManager, tpaRequests, tpaManager, teleportService);
        TpaHereCommand tpaHere = new TpaHereCommand(configManager, tpaRequests, tpaManager, teleportService);
        getCommand("tpa").setExecutor(tpa);
        getCommand("tpa").setTabCompleter(tpa);
        getCommand("tpahere").setExecutor(tpaHere);
        getCommand("tpahere").setTabCompleter(tpaHere);
        TpaAcceptCommand tpaAccept = new TpaAcceptCommand(configManager, tpaRequests, teleportService);
        getCommand("tpaaccept").setExecutor(tpaAccept);
        getCommand("tpaaccept").setTabCompleter(tpaAccept);
        TpaDenyCommand tpaDeny = new TpaDenyCommand(configManager, tpaRequests);
        getCommand("tpadeny").setExecutor(tpaDeny);
        getCommand("tpadeny").setTabCompleter(tpaDeny);
        TpToggleCommand tpToggle = new TpToggleCommand(configManager, tpaManager);
        getCommand("tptoggle").setExecutor(tpToggle);
        getCommand("tptoggle").setTabCompleter(tpToggle);
        TpAutoCommand tpAuto = new TpAutoCommand(configManager, tpaManager);
        getCommand("tpauto").setExecutor(tpAuto);
        getCommand("tpauto").setTabCompleter(tpAuto);
        TpSettingsCommand tpSettings = new TpSettingsCommand(configManager, settingsGui, chestSettingsGui);
        getCommand("tpsettings").setExecutor(tpSettings);
        getCommand("tpsettings").setTabCompleter(tpSettings);
        FriendCommand friend = new FriendCommand(configManager, tpaManager, friendManager);
        getCommand("friend").setExecutor(friend);
        getCommand("friend").setTabCompleter(friend);
        FAcceptCommand faccept = new FAcceptCommand(configManager, tpaManager, friendManager);
        getCommand("faccept").setExecutor(faccept);
        getCommand("faccept").setTabCompleter(faccept);
        FDenyCommand fdeny = new FDenyCommand(configManager, friendManager);
        getCommand("fdeny").setExecutor(fdeny);
        getCommand("fdeny").setTabCompleter(fdeny);
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new ChatListener(giveawayManager, pollManager), this);
        getServer().getPluginManager().registerEvents(new DeathGameListener(deathGameManager), this);
        getServer().getPluginManager().registerEvents(new ModerationListener(moderationManager), this);
        getServer().getPluginManager().registerEvents(bountyManager, this);
        getServer().getPluginManager().registerEvents(new TpListener(tpaRequests, trapReport), this);
        getServer().getPluginManager().registerEvents(chestSettingsGui, this);
        getServer().getPluginManager().registerEvents(new SmpListener(smpManager), this);
    }

    private void hookVault() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            getLogger().warning("Vault not found - bounty economy payouts disabled.");
            return;
        }
        try {
            RegisteredServiceProvider<net.milkbowl.vault.economy.Economy> provider =
                    getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
            if (provider != null) {
                net.milkbowl.vault.economy.Economy economy = provider.getProvider();
                bountyManager.setEconomy(economy);
                getLogger().info("Vault economy hooked: " + economy.getName());
            }
        } catch (Throwable t) {
            getLogger().warning("Failed to hook Vault: " + t.getMessage());
        }
    }

    public static OrchidPlugins getInstance() {
        return instance;
    }

    public GiveawayManager getGiveawayManager() {
        return giveawayManager;
    }

    public DeathGameManager getDeathGameManager() {
        return deathGameManager;
    }

    public RestartManager getRestartManager() {
        return restartManager;
    }

    public SmpManager getSmpManager() {
        return smpManager;
    }

    public BountyManager getBountyManager() {
        return bountyManager;
    }

    public ModerationManager getModerationManager() {
        return moderationManager;
    }

    public SuspensionManager getSuspensionManager() {
        return suspensionManager;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public SqliteDatabase getDatabase() {
        return database;
    }

    public WebhookManager getWebhookManager() {
        return webhookManager;
    }

    public PollManager getPollManager() {
        return pollManager;
    }

    public UpdateManager getUpdateManager() {
        return updateManager;
    }

    public AttributeManager getAttributeManager() {
        return attributeManager;
    }

    public TpaManager getTpaManager() {
        return tpaManager;
    }

    public RequestManager getTpaRequests() {
        return tpaRequests;
    }

    public TrapReportManager getTrapReport() {
        return trapReport;
    }
}
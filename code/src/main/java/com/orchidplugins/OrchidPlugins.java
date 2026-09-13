package com.orchidplugins;

import com.orchidplugins.commands.AdminAbuseCommand;
import com.orchidplugins.commands.AnnounceCommand;
import com.orchidplugins.commands.BanCommand;
import com.orchidplugins.commands.BanIpCommand;
import com.orchidplugins.commands.CheckBountyCommand;
import com.orchidplugins.commands.DeathGameCommand;
import com.orchidplugins.commands.EndReactCommand;
import com.orchidplugins.commands.EndPollCommand;
import com.orchidplugins.commands.PollCommand;
import com.orchidplugins.commands.ReactCommand;
import com.orchidplugins.commands.RestartCommand;
import com.orchidplugins.commands.RollBountyCommand;
import com.orchidplugins.commands.RollDiceCommand;
import com.orchidplugins.commands.StopMovingCommand;
import com.orchidplugins.commands.SuspendStaffCommand;
import com.orchidplugins.commands.UnbanCommand;
import com.orchidplugins.commands.UnwarnCommand;
import com.orchidplugins.commands.VoteCommand;
import com.orchidplugins.commands.WarnCommand;
import com.orchidplugins.commands.OrchidPluginsCommand;
import com.orchidplugins.db.SqliteDatabase;
import com.orchidplugins.listeners.ChatListener;
import com.orchidplugins.listeners.DeathGameListener;
import com.orchidplugins.listeners.ModerationListener;
import com.orchidplugins.managers.AttributeManager;
import com.orchidplugins.managers.BountyManager;
import com.orchidplugins.managers.ConfigManager;
import com.orchidplugins.managers.DeathGameManager;
import com.orchidplugins.managers.GiveawayManager;
import com.orchidplugins.managers.ModerationManager;
import com.orchidplugins.managers.PollManager;
import com.orchidplugins.managers.RestartManager;
import com.orchidplugins.managers.SuspensionManager;
import com.orchidplugins.managers.WebhookManager;
import com.orchidplugins.util.Msg;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public final class OrchidPlugins extends JavaPlugin {

    private static OrchidPlugins instance;

    private GiveawayManager giveawayManager;
    private DeathGameManager deathGameManager;
    private RestartManager restartManager;
    private BountyManager bountyManager;
    private ModerationManager moderationManager;
    private SuspensionManager suspensionManager;
    private AttributeManager attributeManager;
    private ConfigManager configManager;
    private SqliteDatabase database;
    private WebhookManager webhookManager;
    private PollManager pollManager;

    @Override
    public void onEnable() {
        instance = this;
        Msg.init(this);

        this.configManager = new ConfigManager(this);
        this.database = new SqliteDatabase(this, configManager.databaseFilename());
        this.webhookManager = new WebhookManager(this, configManager.getRaw());
        this.attributeManager = new AttributeManager();
        this.giveawayManager = new GiveawayManager();
        this.deathGameManager = new DeathGameManager(this);
        this.restartManager = new RestartManager(this);
        this.bountyManager = new BountyManager(this, configManager, webhookManager);
        this.moderationManager = new ModerationManager(this);
        this.suspensionManager = new SuspensionManager(this, configManager, webhookManager);
        this.pollManager = new PollManager(this, configManager);

        registerCommands();
        registerListeners();
        hookVault();

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
        getCommand("rollbounty").setExecutor(new RollBountyCommand(this, bountyManager));
        getCommand("checkbounty").setExecutor(new CheckBountyCommand(bountyManager));
        getCommand("warn").setExecutor(new WarnCommand(moderationManager, configManager, webhookManager));
        getCommand("unwarn").setExecutor(new UnwarnCommand(moderationManager, configManager, webhookManager));
        getCommand("ban").setExecutor(new BanCommand(moderationManager, configManager, webhookManager));
        getCommand("banip").setExecutor(new BanIpCommand(moderationManager, configManager, webhookManager));
        getCommand("unban").setExecutor(new UnbanCommand(moderationManager, configManager, webhookManager));
        getCommand("suspendstaff").setExecutor(new SuspendStaffCommand(this));
        getCommand("unsuspendstaff").setExecutor(new SuspendStaffCommand(this));
        OrchidPluginsCommand orchid = new OrchidPluginsCommand(this, database, webhookManager);
        getCommand("orchidplugins").setExecutor(orchid);
        getCommand("orchidplugins").setTabCompleter(orchid);
        AdminAbuseCommand adminAbuse = new AdminAbuseCommand(attributeManager, configManager, this, database, webhookManager);
        getCommand("adminabuse").setExecutor(adminAbuse);
        getCommand("adminabuse").setTabCompleter(adminAbuse);
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new ChatListener(giveawayManager, pollManager), this);
        getServer().getPluginManager().registerEvents(new DeathGameListener(deathGameManager), this);
        getServer().getPluginManager().registerEvents(new ModerationListener(moderationManager), this);
        getServer().getPluginManager().registerEvents(bountyManager, this);
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

    public AttributeManager getAttributeManager() {
        return attributeManager;
    }
}
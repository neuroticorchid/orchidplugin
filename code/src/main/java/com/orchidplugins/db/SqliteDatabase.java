package com.orchidplugins.db;

import com.orchidplugins.util.TimeParser;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * SQLite-backed audit store for admin abuse executions.
 * All work runs on a single dedicated thread so the main thread never blocks.
 */
public final class SqliteDatabase {

    private final Plugin plugin;
    private final ExecutorService dbExecutor;
    private volatile Connection connection;

    public SqliteDatabase(Plugin plugin, String filename) {
        this.plugin = plugin;
        this.dbExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r, "OrchidPlugins-DB");
            thread.setDaemon(true);
            return thread;
        });
        plugin.getDataFolder().mkdirs();
        File dbFile = new File(plugin.getDataFolder(), filename);
        String url = "jdbc:sqlite:" + dbFile.getAbsolutePath();
        dbExecutor.execute(() -> {
            try {
                Class.forName("org.sqlite.JDBC");
                connection = DriverManager.getConnection(url);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to open SQLite database: " + e.getMessage());
                return;
            }
            try (PreparedStatement ps = connection.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS abuse_log ("
                            + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                            + "executor TEXT NOT NULL,"
                            + "target TEXT NOT NULL,"
                            + "subcommand TEXT NOT NULL,"
                            + "value TEXT,"
                            + "server_time BIGINT NOT NULL)")) {
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().warning("Failed to create abuse_log table: " + e.getMessage());
            }
            try (PreparedStatement ps = connection.prepareStatement(
                    "CREATE INDEX IF NOT EXISTS idx_abuse_executor ON abuse_log(executor)")) {
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().warning("Failed to create abuse_log index: " + e.getMessage());
            }
            try (PreparedStatement ps = connection.prepareStatement(
                    "CREATE INDEX IF NOT EXISTS idx_abuse_target ON abuse_log(target)")) {
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().warning("Failed to create abuse_log index: " + e.getMessage());
            }
            try (PreparedStatement ps = connection.prepareStatement(
                    "CREATE INDEX IF NOT EXISTS idx_abuse_time ON abuse_log(server_time)")) {
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().warning("Failed to create abuse_log index: " + e.getMessage());
            }
        });
    }

    /** Fire-and-forget insert of an admin-abuse execution. */
    public void logAbuse(String executor, String target, String subcommand, String value) {
        dbExecutor.execute(() -> {
            Connection conn = connection;
            if (conn == null) {
                return;
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO abuse_log (executor,target,subcommand,value,server_time) VALUES (?,?,?,?,?)")) {
                ps.setString(1, executor);
                ps.setString(2, target);
                ps.setString(3, subcommand);
                ps.setString(4, value);
                ps.setLong(5, System.currentTimeMillis());
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().warning("Failed to log admin abuse: " + e.getMessage());
            }
        });
    }

    /**
     * Queries recent entries (optionally filtered by executor/target name).
     * Runs off the main thread; the callback is invoked on the main thread with
     * rows of [executor, target, subcommand, value, "X ago"].
     */
    public void queryAbuse(String playerFilter, int limit, Consumer<List<String[]>> callback) {
        dbExecutor.execute(() -> {
            Connection conn = connection;
            if (conn == null) {
                plugin.getServer().getScheduler().runTask(plugin, () -> callback.accept(List.of()));
                return;
            }
            List<String[]> rows = new ArrayList<>();
            StringBuilder sql = new StringBuilder(
                    "SELECT executor,target,subcommand,value,server_time FROM abuse_log");
            boolean filtered = playerFilter != null && !playerFilter.isBlank();
            if (filtered) {
                sql.append(" WHERE executor LIKE ? OR target LIKE ?");
            }
            sql.append(" ORDER BY server_time DESC LIMIT ?");
            try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
                int idx = 1;
                if (filtered) {
                    ps.setString(idx++, "%" + playerFilter + "%");
                    ps.setString(idx++, "%" + playerFilter + "%");
                }
                ps.setInt(idx, limit);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        long elapsedSecs = Math.max(0,
                                (System.currentTimeMillis() - rs.getLong("server_time")) / 1000L);
                        rows.add(new String[]{
                                rs.getString("executor"),
                                rs.getString("target"),
                                rs.getString("subcommand"),
                                rs.getString("value"),
                                TimeParser.format(elapsedSecs) + " ago"
                        });
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("Failed to query abuse log: " + e.getMessage());
            }
            final List<String[]> result = rows;
            plugin.getServer().getScheduler().runTask(plugin, () -> callback.accept(result));
        });
    }

    public void close() {
        dbExecutor.execute(() -> {
            try {
                if (connection != null && !connection.isClosed()) {
                    connection.close();
                }
            } catch (SQLException ignored) {
            }
        });
        dbExecutor.shutdown();
    }
}
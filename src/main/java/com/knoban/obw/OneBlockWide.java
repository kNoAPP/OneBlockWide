package com.knoban.obw;

import com.knoban.atlas.data.local.DataHandler;
import com.knoban.atlas.world.Coordinate;
import com.knoban.obw.commands.CommandHandler;
import com.knoban.obw.game.Game;
import com.knoban.obw.general.GeneralListener;
import com.knoban.obw.map.GameMapGenerator;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldInitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.logging.Level;

public class OneBlockWide extends JavaPlugin implements Listener {

    private static OneBlockWide instance;

    private DataHandler.YML config;
    private DataHandler.JSON spawn;
    private Location spawnLoc;

    private final SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private File winLog;

    @Nullable
    private PrintWriter winLogWriter;

    @Override
    public void onEnable() {
        long tStart = System.currentTimeMillis();
        instance = this;
        super.onEnable();

        // TODO
        config = new DataHandler.YML(this, "/config.yml");
        spawn = new DataHandler.JSON(this, "/spawn.json");
        if(spawn.wasCreated()) {
            spawn.saveJSON(new Coordinate(Bukkit.getWorlds().get(0).getSpawnLocation()));
        }
        spawnLoc = spawn.getCachedJSON(Coordinate.class).getLocation();

        // Create the win log file if it doesn't exist
        winLog = new File(getDataFolder(), "winners.log");
        if(!winLog.exists()){
            try {
                winLog.createNewFile();
                getLogger().log(Level.WARNING, "Created winners log file!");
            } catch (IOException e) {
                getLogger().log(Level.WARNING, "Couldn't create winners log file!");
                e.printStackTrace();
            }
        }

        // Create a new buffered writer to write to
        try {
            // Open the file with a file writer with append: true
            // so we don't overwrite any existing data
            winLogWriter = new PrintWriter(new FileWriter(winLog, true));
        } catch (IOException e) {
            e.printStackTrace();
            getLogger().log(Level.WARNING, "Couldn't create winners log writer!");
        }

        new CommandHandler(this);
        getServer().getPluginManager().registerEvents(new GeneralListener(), this);

        GameMapGenerator.getInstance();
        Game.getInstance(); // Rev up those fryers!

        long tEnd = System.currentTimeMillis();
        long startupTime = tEnd - tStart;

        getLogger().info("Successfully Enabled! (" + startupTime + " ms)");
    }

    @Override
    public void onDisable() {
        long tStart = System.currentTimeMillis();
        super.onDisable();

        GameMapGenerator.getInstance().safeShutdown();
        Game.getInstance().getGameMap().unload();
        spawn.saveJSON(new Coordinate(spawnLoc));

        long tEnd = System.currentTimeMillis();
        long shutdownTime = tEnd - tStart;

        // Close the winners log writer
        if(winLogWriter != null){
            winLogWriter.flush();
            winLogWriter.close();
        }

        getLogger().info("Successfully Disabled! (" + shutdownTime + " ms)");
    }

    @EventHandler
    public void onWorldInit(WorldInitEvent event) {
        event.getWorld().setKeepSpawnInMemory(false);
    }

    /**
     * Log the winner to a file
     *
     * @param winner - the winner
     */
    public void logWinner (String winner) {

        if(winLogWriter == null){
            getLogger().log(Level.WARNING, "Winners log writer is null!");
            return;
        }

        winLogWriter.println(winner + " has won a game at " + getCurrentTimeStamp());
        winLogWriter.flush();
    }

    /**
     * Get the current date formatted using the simple date format
     *
     * @return - the current date formatted
     */
    private String getCurrentTimeStamp() {
        return sdfDate.format(new java.util.Date());
    }

    @NotNull
    public static OneBlockWide getInstance() {
        return instance;
    }

    @NotNull
    public DataHandler.YML getFileConfig() {
        return config;
    }

    @NotNull
    public Location getSpawnLocation() {
        return spawnLoc.clone();
    }

    public void setSpawnLocation(@NotNull Location spawnLoc) {
        this.spawnLoc = spawnLoc;
    }
}

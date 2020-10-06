package com.knoban.obw;

import com.knoban.atlas.data.local.DataHandler;
import com.knoban.atlas.world.Coordinate;
import com.knoban.obw.commands.CommandHandler;
import com.knoban.obw.game.Game;
import com.knoban.obw.game.GamePhase;
import com.knoban.obw.general.GeneralListener;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public class OneBlockWide extends JavaPlugin {

    private static OneBlockWide instance;

    private DataHandler.YML config;
    private DataHandler.JSON spawn;
    private Location spawnLoc;

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

        new CommandHandler(this);
        getServer().getPluginManager().registerEvents(new GeneralListener(), this);

        Game.getInstance(); // Rev up those fryers!

        long tEnd = System.currentTimeMillis();
        long startupTime = tEnd - tStart;

        getLogger().info("Successfully Enabled! (" + startupTime + " ms)");
    }

    @Override
    public void onDisable() {
        long tStart = System.currentTimeMillis();
        super.onDisable();

        // TODO
        Game.getInstance().setGamePhase(GamePhase.LOBBY);
        spawn.saveJSON(new Coordinate(spawnLoc));

        long tEnd = System.currentTimeMillis();
        long shutdownTime = tEnd - tStart;

        getLogger().info("Successfully Disabled! (" + shutdownTime + " ms)");
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

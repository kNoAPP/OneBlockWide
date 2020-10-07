package com.knoban.obw.map;

import com.knoban.obw.OneBlockWide;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedList;
import java.util.Queue;

public final class GameMapGenerator {

    private static GameMapGenerator instance;

    private final int preGenCap, smallMapSize, mediumMapSize, largeMapSize, minMedium, minLarge;

    private Queue<GameMap> smallMaps = new LinkedList<>();
    private Queue<GameMap> mediumMaps = new LinkedList<>();
    private Queue<GameMap> largeMaps = new LinkedList<>();

    private boolean sleeping;

    public static final int SMALL_MAP = 0, MEDIUM_MAP = 1, LARGE_MAP = 2;

    private GameMapGenerator() {
        FileConfiguration fc = OneBlockWide.getInstance().getFileConfig().getCachedYML();
        this.preGenCap = fc.getInt("max-pregenerated-chunks-per-type", 3);

        this.smallMapSize = fc.getInt("size-of-small-maps", 200);
        this.mediumMapSize = fc.getInt("size-of-medium-maps", 400);
        this.largeMapSize = fc.getInt("size-of-large-maps", 800);

        this.minMedium = fc.getInt("min-players-for-medium", 40);
        this.minLarge = fc.getInt("min-players-for-large", 60);

        this.sleeping = false;
        generateNewMap();
    }

    public void wake() {
        if(!sleeping)
            return;

        sleeping = false;
        generateNewMap();
    }

    private void generateNewMap() { // Generate smaller maps first, takes less time, less annoyance.
        GameMap map;
        if(smallMaps.size() < preGenCap) {
            map = GameMap.generateMap(smallMapSize);
            map.generateWorld();
            smallMaps.offer(map);
        } else if(mediumMaps.size() < preGenCap) {
            map = GameMap.generateMap(mediumMapSize);
            map.generateWorld();
            mediumMaps.offer(map);
        } else if(largeMaps.size() < preGenCap) {
            map = GameMap.generateMap(largeMapSize);
            map.generateWorld();
            largeMaps.offer(map);
        } else {
            sleeping = true;
            return;
        }

        map.setOnGenerationComplete(this::generateNewMap);
    }

    public boolean isSleeping() {
        return sleeping;
    }

    @Nullable
    public GameMap pollMap() {
        int players = Bukkit.getOnlinePlayers().size();
        if(players >= minLarge)
            return pollMap(LARGE_MAP);
        else if(players >= minMedium)
            return pollMap(MEDIUM_MAP);
        else
            return pollMap(SMALL_MAP);
    }

    @Nullable
    public GameMap pollMap(int type) { // Use what's available, then generate if we must.
        switch(type) {
            case SMALL_MAP:
                if(!smallMaps.isEmpty())
                    return smallMaps.poll();
            case MEDIUM_MAP:
                if(!mediumMaps.isEmpty())
                    return mediumMaps.poll();
            case LARGE_MAP:
                if(!largeMaps.isEmpty())
                    return largeMaps.poll();
            default: // Somehow we need more maps than we have, emergency generate a map.
                int players = Bukkit.getOnlinePlayers().size();
                GameMap map;
                if(players >= minLarge)
                    map = GameMap.generateMap(largeMapSize);
                else if(players >= minMedium)
                    map = GameMap.generateMap(mediumMapSize);
                else
                    map = GameMap.generateMap(smallMapSize);
                map.generateWorld();
                return map;
        }
    }

    public void safeShutdown() {
        while(!smallMaps.isEmpty()) {
            GameMap map = smallMaps.poll();
            map.unload();
        }

        while(!mediumMaps.isEmpty()) {
            GameMap map = mediumMaps.poll();
            map.unload();
        }

        while(!largeMaps.isEmpty()) {
            GameMap map = largeMaps.poll();
            map.unload();
        }
    }

    public static GameMapGenerator getInstance() {
        if(instance == null) {
            instance = new GameMapGenerator();
        }

        return instance;
    }
}

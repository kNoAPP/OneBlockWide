package com.knoban.obw.map;

import com.knoban.obw.OneBlockWide;
import org.apache.commons.io.FileUtils;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

public class GameMap implements Listener {

    private static int worldCount = 0;

    private World world;
    private final String name;
    private final int length;
    private final long maxMillisPerTickOnGeneration;
    private int completedGenerationStages, lowestStableXRel;
    private final boolean water, ore;
    private Location origin, center;

    private Runnable onGenerationComplete;

    private GameMap(@NotNull String name, int length) {
        this.name = name;
        this.length = length;
        this.completedGenerationStages = 0;

        FileConfiguration fc = OneBlockWide.getInstance().getFileConfig().getCachedYML();
        this.maxMillisPerTickOnGeneration = fc.getLong("max-millis-per-tick-generating", 10);
        this.water = fc.getBoolean("enable-water", true);
        this.ore = fc.getBoolean("extra-ore", true);
    }

    public void generateWorld() {
        if(completedGenerationStages > 0)
            return;
        incCompletion();
        this.world = new WorldCreator(name).environment(World.Environment.NORMAL).type(WorldType.NORMAL).generateStructures(false)
                /*.generator(new ChunkGenerator() {
                    @Override
                    public @NotNull ChunkData generateChunkData(@NotNull World world, @NotNull Random random, int chunkX, int chunkZ, @NotNull BiomeGrid biome) {
                        if(chunkX < 0 || chunkZ != 0)
                            return super.createChunkData(world); // Void

                        ChunkData data = super.createVanillaChunkData(world, chunkX, chunkZ); // Remove water
                        for(int x=0; x<16; x++) {
                            for(int y=0; y<world.getMaxHeight(); y++) {
                                for(int z=0; z<16; z++) {
                                    if(data.getBlockData(x, y, z).getMaterial() == Material.WATER) {
                                        data.setBlock(x, y, z, Material.AIR);
                                    }
                                }
                            }
                        }

                        return data;
                    }

                    @Override
                    public @NotNull List<BlockPopulator> getDefaultPopulators(@NotNull World world) {
                        List<BlockPopulator> populators = super.getDefaultPopulators(world);
                        populators.add(new BlockPopulator() {
                            @Override
                            public void populate(@NotNull World world, @NotNull Random random, @NotNull Chunk chunk) {
                                if(chunk.getX() > 0 && chunk.getZ() == 0) {
                                    for(int x=0; x<16; x++) {
                                        if(random.nextInt(8) == 1) {
                                            world.generateTree(chunk.getBlock(x, world.getHighestBlockYAt(x, 0), 0).getLocation(), TreeType.TREE);

                                            x++;
                                        }
                                    }
                                }
                            }
                        });
                        return populators;
                    }
                })*/.createWorld();
        world.setAutoSave(false);

        world.setAmbientSpawnLimit(0);
        world.setAnimalSpawnLimit(0);
        world.setMonsterSpawnLimit(0);
        world.setWaterAmbientSpawnLimit(0);
        world.setWaterAnimalSpawnLimit(0);

        world.setDifficulty(Difficulty.NORMAL);
        world.setViewDistance(8);

        world.setGameRule(GameRule.DO_IMMEDIATE_RESPAWN, true);
        world.setGameRule(GameRule.DO_TRADER_SPAWNING, false);
        world.setGameRule(GameRule.DO_PATROL_SPAWNING, false);
        world.setGameRule(GameRule.DO_MOB_SPAWNING, false);
        world.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);
        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        world.setGameRule(GameRule.SPECTATORS_GENERATE_CHUNKS, false);
        world.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
        world.setGameRule(GameRule.DO_IMMEDIATE_RESPAWN, true);

        world.setThundering(false);
        world.setStorm(false);
        world.setTime(6000);

        this.origin = world.getSpawnLocation();

        int oX = origin.getBlockX();
        int oZ = origin.getBlockZ();

        OneBlockWide instance = OneBlockWide.getInstance();
        Bukkit.getServer().getPluginManager().registerEvents(this, instance);

        FileConfiguration cached = instance.getFileConfig().getCachedYML();
        long worldGenDelay = cached.getInt("world-generation-delay", 30) * 20L;
        int padding = Math.max(2, cached.getInt("padding", 125) + 2);
        int maxBuild = Math.max(0, Math.min(255, cached.getInt("max-build-height", 255)));
        new BukkitRunnable() {
            public void run() {
                Bukkit.getLogger().info("Generating map " + name + "...");
                if(getGenerationStatus() == GenerationStatus.UNLOADED) {
                    this.cancel();
                    return;
                }

                GameMap.this.lowestStableXRel = 0;
                GameMap.this.center = new Location(world, oX+length/2, 80, oZ);

                // Barrier
                lowTickBlockChange(world, oX, oX+length, maxBuild,  maxBuild, oZ, oZ, Material.BARRIER, () -> incCompletion());
                lowTickBlockChange(world, oX-1, oX-1, 0, world.getMaxHeight()-1, oZ, oZ, Material.BARRIER, () -> incCompletion());
                lowTickBlockChange(world, oX+length, oX+length, 0, world.getMaxHeight()-1, oZ, oZ, Material.BARRIER, () -> incCompletion());
                lowTickBlockChange(world, oX-1, oX+length, 0, world.getMaxHeight()-1, oZ+1, oZ+1, Material.BARRIER, () -> incCompletion());
                lowTickBlockChange(world, oX-1, oX+length, 0, world.getMaxHeight()-1, oZ-1, oZ-1, Material.BARRIER, () -> incCompletion());

                // Air
                lowTickBlockChange(world, oX, oX+length, maxBuild+1,  world.getMaxHeight()-1, oZ, oZ, Material.AIR, () -> incCompletion());
                if(padding > 2) {
                    lowTickBlockChange(world, oX - padding, oX - 2, 0, world.getMaxHeight() - 1, oZ - 1, oZ + 1, Material.AIR, () -> incCompletion());
                    lowTickBlockChange(world, oX + length + 1, oX + length + padding, 0, world.getMaxHeight() - 1, oZ - 1, oZ + 1, Material.AIR, () -> incCompletion());
                    lowTickBlockChange(world, oX - padding, oX + length + padding, 0, world.getMaxHeight() - 1, oZ - padding, oZ - 2, Material.AIR, () -> incCompletion());
                    lowTickBlockChange(world, oX - padding, oX + length + padding, 0, world.getMaxHeight() - 1, oZ + 2, oZ + padding, Material.AIR, () -> incCompletion());
                } else {
                    incCompletion();
                    incCompletion();
                    incCompletion();
                    incCompletion();
                }

                // Block Replacing
                lowTickBlockReplace(world, oX, oX+length-1, 0, world.getMaxHeight()-1, oZ, oZ, () -> incCompletion());
            }
        }.runTaskLater(OneBlockWide.getInstance(), worldGenDelay); // Give world chance to warm up. Hate how there's no call to check progress
    }

    public int getCompletedGenerationStages() {
        return completedGenerationStages;
    }

    @NotNull
    public GenerationStatus getGenerationStatus() {
        if(completedGenerationStages >= 12)
            return GenerationStatus.COMPLETE;
        if(completedGenerationStages > 0)
            return GenerationStatus.GENERATING;
        if(completedGenerationStages == 0)
            return GenerationStatus.NOT_STARTED;

        return GenerationStatus.UNLOADED;
    }

    @Nullable
    private BukkitTask lowTickBlockChange(@NotNull World world, int xFrom, int xTo, int yFrom, int yTo, int zFrom, int zTo, @NotNull Material type, @Nullable Runnable callWhenFinished) {
        if(xFrom > xTo || yFrom > yTo || zFrom > zTo) {
            if(callWhenFinished != null)
                callWhenFinished.run();
            return null;
        }

        return new BukkitRunnable() {
            private int x = xFrom, y = yFrom, z = zFrom;
            private int opsPerSecond = 50;
            private long lastRun = 0L;
            public void run() {
                if(getGenerationStatus() == GenerationStatus.UNLOADED) {
                    this.cancel();
                    return;
                }

                long start = System.currentTimeMillis();

                opsPerSecond = lastRun > maxMillisPerTickOnGeneration ? (int) (opsPerSecond*0.875) : opsPerSecond+10;
                for(int i=0; i<opsPerSecond; i++) {
                    world.getBlockAt(x, y, z).setType(type);

                    x++;
                    if(x > xTo) {
                        x = xFrom;
                        y++;

                        if(y > yTo) {
                            y = yFrom;
                            z++;

                            if(z > zTo) {
                                if(callWhenFinished != null)
                                    callWhenFinished.run();
                                this.cancel();
                                return;
                            }
                        }
                    }
                }

                lastRun = System.currentTimeMillis() - start;
            }
        }.runTaskTimer(OneBlockWide.getInstance(), 0L, 1L);
    }

    @Nullable
    private BukkitTask lowTickBlockReplace(@NotNull World world, int xFrom, int xTo, int yFrom, int yTo, int zFrom, int zTo, @Nullable Runnable callWhenFinished) {
        if(xFrom > xTo || yFrom > yTo || zFrom > zTo) {
            if(callWhenFinished != null)
                callWhenFinished.run();
            return null;
        }

        return new BukkitRunnable() {
            private int x = xFrom, y = yFrom, z = zFrom;
            private int opsPerSecond = 50;
            private long lastRun = 0L;
            public void run() {
                if(getGenerationStatus() == GenerationStatus.UNLOADED) {
                    this.cancel();
                    return;
                }

                long start = System.currentTimeMillis();

                opsPerSecond = lastRun > maxMillisPerTickOnGeneration ? (int) (opsPerSecond*0.875) : opsPerSecond+10;
                for(int i=0; i<opsPerSecond; i++) {
                    Block b = world.getBlockAt(x, y, z);

                    switch(b.getType()) {
                        case LAPIS_ORE:
                        case REDSTONE_ORE:
                        case EMERALD_ORE:
                        case GOLD_ORE:
                            if(ore)
                                b.setType(Material.DIAMOND_ORE);
                            break;
                        case DIORITE:
                        case COAL_ORE:
                            if(ore)
                                b.setType(Material.IRON_ORE);
                            break;
                        case ANDESITE:
                        case GRANITE:
                            if(ore)
                                b.setType(Material.COAL_ORE);
                            break;
                        case WATER:
                            if(!water)
                                b.setType(Material.AIR);
                            break;
                        default:
                            break;
                    }

                    x++;
                    if(x > xTo) {
                        x = xFrom;
                        y++;

                        if(y > yTo) {
                            y = yFrom;
                            z++;

                            if(z > zTo) {
                                if(callWhenFinished != null)
                                    callWhenFinished.run();
                                this.cancel();
                                return;
                            }
                        }
                    }
                }

                lastRun = System.currentTimeMillis() - start;
            }
        }.runTaskTimer(OneBlockWide.getInstance(), 0L, 1L);
    }

    public void decayMap(float percent) {
        if(getGenerationStatus() != GenerationStatus.COMPLETE)
            return;

        if(percent > 1f)
            percent = 1f;
        else if(percent < 0f)
            percent = 0f;

        int decayAmount = (int) ((length / 2F)*percent);

        if(decayAmount > lowestStableXRel) {
            for(int i=lowestStableXRel; i<decayAmount; i++) {
                int x = origin.getBlockX() + i;
                for(int y=0; y<=world.getHighestBlockYAt(x, origin.getBlockZ()); y++) {
                    Block b = world.getBlockAt(x, y, origin.getBlockZ());
                    if(y%3==0)
                        b.getWorld().playEffect(b.getLocation(), Effect.STEP_SOUND, b.getType());
                    world.spawnFallingBlock(b.getLocation().clone().add(0.5, 0.5, 0.5), b.getBlockData()).setVelocity(new Vector(0, 0, 0));
                    b.setType(Material.AIR);
                }
            }

            for(int i=lowestStableXRel; i<decayAmount; i++) {
                int x = origin.getBlockX() + (length-1) - i;
                for(int y=0; y<=world.getHighestBlockYAt(x, origin.getBlockZ()); y++) {
                    Block b = world.getBlockAt(x, y, origin.getBlockZ());
                    if(y%3==0)
                        b.getWorld().playEffect(b.getLocation(), Effect.STEP_SOUND, b.getType());
                    world.spawnFallingBlock(b.getLocation().clone().add(0.5, 0.5, 0.5), b.getBlockData()).setVelocity(new Vector(0, 0, 0));
                    b.setType(Material.AIR);
                }
            }

            lowestStableXRel = decayAmount;
        }
    }

    @Nullable
    public World getWorld() {
        return world;
    }

    @NotNull
    public String getName() {
        return name;
    }

    @Nullable
    public Location getOrigin() {
        return origin.clone();
    }

    @Nullable
    public Location getCenter() {
        Location loc = center.clone();
        loc.setY(ignoreBarriersMaxYAt(world, center.getBlockX(), center.getBlockZ()));
        return loc;
    }

    @Nullable
    public Location getPlayerSpawnLocation(int index, int playerCount) {
        if(world == null)
            return null;

        float spacing = (float) length / (float) playerCount;
        int offset = (int) (index * spacing);

        return new Location(world, origin.getBlockX()+0.5+offset, ignoreBarriersMaxYAt(world, origin.getBlockX()+offset, origin.getBlockZ())+0.5, origin.getBlockZ()+0.5);
    }

    public int ignoreBarriersMaxYAt(@NotNull World world, int x, int z) {
        int y;
        for(y=world.getMaxHeight()-1; world.getBlockAt(x, y, z).getType() == Material.AIR
                || world.getBlockAt(x, y, z).getType() == Material.BARRIER; --y);
        return y;
    }

    private void incCompletion() {
        ++completedGenerationStages;
        Bukkit.getLogger().info("Map " + name + " stage: " + completedGenerationStages);
        if(completedGenerationStages == 12) {
            Bukkit.getLogger().info("Map " + name + " is baked!");
            if(onGenerationComplete != null)
                onGenerationComplete.run();
        }
    }

    public int getLength() {
        return length;
    }

    public void setOnGenerationComplete(@Nullable Runnable onGenerationComplete) {
        this.onGenerationComplete = onGenerationComplete;
    }

    public void unload() {
        if(getGenerationStatus() == GenerationStatus.UNLOADED)
            return;

        completedGenerationStages = -1;
        HandlerList.unregisterAll(this);
        for(Player pl : world.getPlayers())
            pl.teleport(Bukkit.getWorlds().get(0).getSpawnLocation());
        Bukkit.unloadWorld(world, false);

        try {
            FileUtils.deleteDirectory(world.getWorldFolder());
        } catch(IOException e) {
            OneBlockWide.getInstance().getLogger().warning("Failed to unload map: " + name);
        }
    }

    @EventHandler
    public void waterPhysics(BlockFromToEvent e) {
        Block b = e.getToBlock();
        if(!b.getWorld().equals(world))
            return;

        if(getGenerationStatus() != GenerationStatus.COMPLETE) {
            e.setCancelled(true);
            return;
        }

        if(!(origin.getX()+lowestStableXRel <= b.getX() && b.getX() < origin.getX() + length - lowestStableXRel && b.getZ() == origin.getZ())) {
            e.setCancelled(true);
            return;
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onBlockPlace(BlockPlaceEvent e) {
        Block b = e.getBlockPlaced();
        if(!b.getWorld().equals(world))
            return;

        if(getGenerationStatus() != GenerationStatus.COMPLETE) {
            e.setCancelled(true);
            return;
        }

        if(!(origin.getX()+lowestStableXRel <= b.getX() && b.getX() < origin.getX() + length - lowestStableXRel && b.getZ() == origin.getZ())) {
            e.setCancelled(true);
            return;
        }
    }

    @NotNull
    public static GameMap generateMap(int length) {
        String name = null;
        do {
            name = "game" + worldCount++;
        } while(Bukkit.getWorld(name) != null);

        GameMap map = new GameMap(name, length);
        return map;
    }

    public enum GenerationStatus {
        NOT_STARTED, GENERATING, COMPLETE, UNLOADED
    }
}

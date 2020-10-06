package com.knoban.obw.game;

import com.knoban.atlas.utils.Tools;
import com.knoban.obw.OneBlockWide;
import com.knoban.obw.general.CC;
import com.knoban.obw.map.GameMap;
import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class Game implements Listener {

    private static Game instance;

    private final OneBlockWide oneBlockWide;
    private final int requiredPlayers, initialLobbyCountdown, initialPvPCountdown, initialGameCountdown;
    private GameMap gameMap;
    private GamePhase phase;
    private int ticker, countdown;

    private String lastWinner;

    private Game() {
        this.oneBlockWide = OneBlockWide.getInstance();
        FileConfiguration cached = oneBlockWide.getFileConfig().getCachedYML();
        this.requiredPlayers = cached.getInt("min-players", 2);
        this.initialLobbyCountdown = cached.getInt("then-start-after-seconds", 30);
        this.initialPvPCountdown = cached.getInt("no-pvp-for", 60);
        this.initialGameCountdown = cached.getInt("then-game-lasts", 1200);

        Bukkit.getServer().getPluginManager().registerEvents(this, oneBlockWide);

        this.gameMap = GameMap.generateMap(getSuggestedMapLength());
        this.phase = GamePhase.LOBBY;
        this.lastWinner = ChatColor.RED + "No One";

        setGamePhase(GamePhase.LOBBY);

        Bukkit.getScheduler().runTaskTimer(oneBlockWide, this::run, 0L, 20L);
    }

    public void setGamePhase(@NotNull GamePhase phase) {
        switch(phase) {
            case LOBBY:
                for(Player pl : Bukkit.getOnlinePlayers()) {
                    pl.teleport(oneBlockWide.getSpawnLocation());
                    pl.sendMessage(CC.NEON_BLUE + "Game> " + CC.EGGSHELL + "Welcome to the lobby! A new game begins soon.");
                    pl.playSound(pl.getLocation(), Sound.ENTITY_ENDER_EYE_DEATH, 1F, 0.2F);
                    Tools.clearFullInv(pl);
                    pl.setGameMode(GameMode.ADVENTURE);
                    pl.setHealth(20);
                    pl.setFoodLevel(20);
                    pl.setInvulnerable(true);
                    pl.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, 100000, 10, true, false, false));
                    pl.setCollidable(true);
                }

                if(gameMap.getGenerationStatus() != GameMap.GenerationStatus.NOT_STARTED) {
                    gameMap.unload();
                    gameMap = GameMap.generateMap(getSuggestedMapLength());
                }

                countdown = initialLobbyCountdown;
                break;
            case PRECOMBAT:
                gameMap.getWorld().setPVP(false);

                int index = 0, playerCount = Bukkit.getOnlinePlayers().size();
                for(Player pl : Bukkit.getOnlinePlayers()) {
                    pl.teleport(gameMap.getPlayerSpawnLocation(index++, playerCount));
                    pl.sendMessage(CC.NEON_BLUE + "Game> " + CC.MEDIUM_PURPLE + "Welcome to combat! PVP will be enabled soon.");
                    pl.sendMessage(CC.NEON_BLUE + "Game> " + CC.EGGSHELL + "We've given you some basic supplies, enjoy.");
                    pl.playSound(pl.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1F, 0.5F);
                    pl.playSound(pl.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1F, 0.8F);
                    Tools.clearFullInv(pl);
                    pl.getInventory().addItem(new ItemStack(Material.OAK_LOG, 3), new ItemStack(Material.APPLE), new ItemStack(Material.SPONGE));
                    pl.setGameMode(GameMode.SURVIVAL);
                    pl.removePotionEffect(PotionEffectType.SATURATION);
                    pl.setSaturation(6F);
                    pl.setInvulnerable(false);
                    pl.setCollidable(false);
                }

                countdown = initialPvPCountdown;
                break;
            case COMBAT:
                gameMap.getWorld().setPVP(true);
                for(Player pl : Bukkit.getOnlinePlayers()) {
                    pl.sendMessage(CC.NEON_BLUE + "Game> " + CC.EGGSHELL + "PVP Enabled! Good luck.");
                    pl.sendMessage(CC.NEON_BLUE + "Game> " + CC.EGGSHELL + "Killing a player will drop additional food.");
                    pl.playSound(pl.getLocation(), Sound.BLOCK_NOTE_BLOCK_IRON_XYLOPHONE, 1F, 0.8F);
                    pl.playSound(pl.getLocation(), Sound.ENTITY_IRON_GOLEM_DAMAGE, 0.6F, 1.5F);
                }

                countdown = initialGameCountdown;
                break;
            case WINNER:
                gameMap.getWorld().setPVP(false);
                List<String> winnerQuotes = Arrays.asList(
                        " takes the cake!",
                        " got the W!",
                        " won the game!",
                        " destroyed the competition!",
                        " showed their true power!",
                        " eliminated the competition!",
                        " is the champion!",
                        " goes down in history!"
                );
                String winnerQuote = winnerQuotes.get(ThreadLocalRandom.current().nextInt(winnerQuotes.size()));
                for(Player pl : Bukkit.getOnlinePlayers()) {
                    pl.sendMessage(CC.NEON_BLUE + "Game> " + CC.MEDIUM_PURPLE + lastWinner + CC.EGGSHELL + winnerQuote);
                    pl.playSound(pl.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1F, 1.1F);
                }

                countdown = 15;
                break;
        }

        this.ticker = 0;
        this.phase = phase;
    }

    // Runs once a second.
    public void run() {
        switch(phase) {
            case LOBBY:
                if(ticker == 5) // Let's players teleport to lobby before heavy work begins.
                    gameMap.generateWorld();

                for(Player pl : Bukkit.getOnlinePlayers()) {
                    if(gameMap.getGenerationStatus() != GameMap.GenerationStatus.COMPLETE)
                        pl.sendActionBar(getMapStatus());
                    else if(Bukkit.getOnlinePlayers().size() < requiredPlayers)
                        pl.sendActionBar(getPlayerStatus());
                    else
                        pl.sendActionBar(getCountdownStatus("Game Starting"));
                }

                if(gameMap.getGenerationStatus() == GameMap.GenerationStatus.COMPLETE && Bukkit.getOnlinePlayers().size() >= requiredPlayers) {
                    if(countdown == initialLobbyCountdown) {
                        for(Player pl : Bukkit.getOnlinePlayers()) {
                            pl.sendMessage(CC.NEON_BLUE + "Game> " + CC.EGGSHELL + "We begin in " + Tools.millisToDHMSWithSpacing(countdown*1000) + "...");
                            pl.playSound(pl.getLocation(), Sound.ENTITY_EVOKER_PREPARE_SUMMON, 1F, 0.8F);
                            // pl.playSound(pl.getLocation(), Sound.ENTITY_VILLAGER_WORK_FLETCHER, 1F, 0.8F);
                            // pl.playSound(pl.getLocation(), Sound.ITEM_BUCKET_EMPTY_LAVA, 1F, 0.9F);
                        }
                    } else if(countdown != 0 && (countdown % 15 == 0 || countdown <= 5)) {
                        for(Player pl : Bukkit.getOnlinePlayers()) {
                            pl.sendMessage(CC.NEON_BLUE + "Game> " + CC.EGGSHELL + "We begin in " + Tools.millisToDHMSWithSpacing(countdown*1000) + "...");
                            pl.playSound(pl.getLocation(), Sound.BLOCK_STONE_BUTTON_CLICK_ON, 1F, 1.5F);
                        }
                    } else if(countdown == 0)
                        setGamePhase(GamePhase.PRECOMBAT);

                    countdown--;
                } else
                    countdown = initialLobbyCountdown;
                break;
            case PRECOMBAT:
                for(Player pl : Bukkit.getOnlinePlayers()) {
                    pl.sendActionBar(getCountdownStatus("PvP Disabled"));
                }

                if(countdown-- == 0)
                    setGamePhase(GamePhase.COMBAT);
                break;
            case COMBAT:
                for(Player pl : Bukkit.getOnlinePlayers()) {
                    if(countdown >= 0)
                        pl.sendActionBar(getCountdownStatus("Survival"));
                }

                gameMap.decayMap(getPercentComplete());
                countdown--;
                break;
            case WINNER:
                for(Player pl : Bukkit.getOnlinePlayers()) {
                    pl.sendActionBar(getCountdownStatus(lastWinner + " won!"));
                }

                for(int i=0; i<5; i++) {
                    Location launch = gameMap.getPlayerSpawnLocation(i, 5);
                    Tools.launchFirework(launch.clone().add(0, 0, 3), Color.GREEN, 1);
                    Tools.launchFirework(launch.clone().add(0, 0, -3), Color.GREEN, 1);
                }

                if(countdown-- == 0)
                    setGamePhase(GamePhase.LOBBY);
                break;
        }

        ticker++;
    }

    @NotNull
    private String getMapStatus() {
        StringBuilder sb = new StringBuilder();
        sb.append(CC.SALMON);
        sb.append("Generating ");
        sb.append("1x");
        sb.append(gameMap.getLength());
        sb.append(" Map");

        sb.append(" ");
        sb.append(CC.VIVID_BLUE_SKY);
        sb.append("(");
        sb.append(gameMap.getCompletedGenerationStages());
        sb.append("/9)");

        return sb.toString();
    }

    @NotNull
    private String getPlayerStatus() {
        StringBuilder sb = new StringBuilder();
        sb.append(CC.SALMON);
        sb.append("Waiting for Players");

        sb.append(" ");
        sb.append(CC.VIVID_BLUE_SKY);
        sb.append("(");
        sb.append(Bukkit.getOnlinePlayers().size());
        sb.append("/");
        sb.append(requiredPlayers);
        sb.append(")");

        return sb.toString();
    }

    @NotNull
    private String getCountdownStatus(String message) {
        StringBuilder sb = new StringBuilder();
        sb.append(CC.SALMON);
        sb.append(message);

        sb.append(" ");
        sb.append(CC.VIVID_BLUE_SKY);
        sb.append("(");
        sb.append(Tools.millisToDHMSWithSpacing(countdown*1000));
        sb.append(")");

        return sb.toString();
    }

    public float getPercentComplete() {
        if(phase != GamePhase.COMBAT)
            return 0;

        return Math.min(1F, (float)ticker / (float)initialGameCountdown);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        Player p = e.getEntity();
        e.setCancelled(true);
        p.setHealth(20);

        switch(phase) {
            case LOBBY:
                p.teleport(oneBlockWide.getSpawnLocation());
                break;
            case PRECOMBAT:
            case COMBAT:
                p.sendMessage(CC.NEON_BLUE + "Game> " + CC.EGGSHELL + "You died! But don't worry, you can still spectate.");
                p.getWorld().playSound(p.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.4F, 1F);
                p.getWorld().playSound(p.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 1.2F, 0.2F);

                e.getDrops().add(new ItemStack(Material.BONE, 1));
                e.getDrops().add(new ItemStack(Material.BEEF, 3));
                for(ItemStack is : e.getDrops())
                    p.getWorld().dropItemNaturally(p.getLocation(), is);

                p.setGameMode(GameMode.SPECTATOR);
                if(p.getLocation().getY() < 0)
                    p.teleport(gameMap.getCenter());

                String lastAlive = getLastAlive(p);
                if(lastAlive != null) {
                    this.lastWinner = lastAlive;
                    setGamePhase(GamePhase.WINNER);
                }
                break;
            case WINNER:
                p.setGameMode(GameMode.SPECTATOR);
                p.teleport(gameMap.getCenter());
                p.setFoodLevel(20);
                break;
        }
    }

    @Nullable
    private String getLastAlive(Player ignore) {
        String lastAlive = null;
        for(Player pl : Bukkit.getOnlinePlayers()) {
            if(pl.equals(ignore))
                continue;

            if(lastAlive == null && pl.getGameMode() != GameMode.SPECTATOR)
                lastAlive = pl.getName();
            else if(lastAlive != null && pl.getGameMode() != GameMode.SPECTATOR)
                return null;
        }

        // No one is alive, last players died on same tick.
        if(lastAlive == null) {
            return ChatColor.RED + "No one";
        }

        return lastAlive;
    }

    @EventHandler
    public void onDamage(EntityDamageEvent e) {
        if(e.getEntity() instanceof Player) {
            Player p = (Player) e.getEntity();
            switch(phase) {
                case LOBBY:
                    e.setCancelled(true);
                    break;
                case PRECOMBAT:
                    break;
                case COMBAT:
                    break;
                case WINNER:
                    break;
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        e.setJoinMessage(ChatColor.GREEN + "" + ChatColor.BOLD + "+ " + ChatColor.GRAY + p.getName());
        switch(phase) {
            case LOBBY:
                p.teleport(oneBlockWide.getSpawnLocation());
                p.sendMessage(CC.NEON_BLUE + "Game> " + CC.EGGSHELL + "Welcome to the lobby! A new game begins soon.");
                for(Player pl : Bukkit.getOnlinePlayers()) {
                    pl.playSound(pl.getLocation(), Sound.BLOCK_COMPOSTER_EMPTY, 1F, 1.6F);
                }
                Tools.clearFullInv(p);
                p.setGameMode(GameMode.ADVENTURE);
                p.setHealth(20);
                p.setFoodLevel(20);
                break;
            case PRECOMBAT:
            case COMBAT:
                p.teleport(gameMap.getCenter());
                p.sendMessage(CC.NEON_BLUE + "Game> " + CC.EGGSHELL + "There's a game in progress! You are now spectating.");
                p.playSound(p.getLocation(), Sound.ENTITY_MOOSHROOM_CONVERT, 1F, 1.1F);
                Tools.clearFullInv(p);
                p.setGameMode(GameMode.SPECTATOR);
                p.setHealth(20);
                p.setFoodLevel(20);
                break;
            case WINNER:
                p.teleport(gameMap.getCenter());
                p.sendMessage(CC.NEON_BLUE + "Game> " + CC.EGGSHELL + "The game just ended, great timing! "
                        + CC.MEDIUM_PURPLE + lastWinner + CC.EGGSHELL + " won.");
                p.playSound(p.getLocation(), Sound.ENTITY_MOOSHROOM_CONVERT, 1F, 1.1F);
                Tools.clearFullInv(p);
                p.setGameMode(GameMode.SPECTATOR);
                p.setHealth(20);
                p.setFoodLevel(20);
                break;
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onQuit(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        e.setQuitMessage(ChatColor.RED + "" + ChatColor.BOLD + "- " + ChatColor.GRAY + p.getName());
        switch(phase) {
            case LOBBY:
                break;
            case PRECOMBAT:
            case COMBAT:
                p.setGameMode(GameMode.SPECTATOR);

                String lastAlive = getLastAlive(p);
                if(lastAlive != null) {
                    this.lastWinner = lastAlive;
                    setGamePhase(GamePhase.WINNER);
                }
                break;
            case WINNER:
                break;
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onGamemodeChange(PlayerGameModeChangeEvent e) {
        Player p = e.getPlayer();

        switch(phase) {
            case LOBBY:
                break;
            case PRECOMBAT:
            case COMBAT:
                    String lastAlive = getLastAlive(p);
                    if(lastAlive != null) {
                        Game.this.lastWinner = lastAlive;
                        setGamePhase(GamePhase.WINNER);
                    }
                break;
            case WINNER:
                break;
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent e) {
        Player p = e.getPlayer();
        if(p.hasPermission("obw.admin"))
            return;

        switch(phase) {
            case LOBBY:
                e.setCancelled(true);
                break;
            case PRECOMBAT:
                break;
            case COMBAT:
                break;
            case WINNER:
                break;
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent e) {
        Player p = e.getPlayer();
        if(p.hasPermission("obw.admin"))
            return;

        switch(phase) {
            case LOBBY:
                e.setCancelled(true);
                break;
            case PRECOMBAT:
                break;
            case COMBAT:
                break;
            case WINNER:
                break;
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent e) {
        Player p = e.getPlayer();
        e.setCancelled(true);

        switch(phase) {
            case LOBBY:
                if(p.getName().equals(lastWinner)) {
                    for(Player pl : Bukkit.getOnlinePlayers())
                        pl.sendMessage(CC.VIVID_BLUE_SKY + "" + ChatColor.BOLD + "WINNER " + CC.EGGSHELL + p.getName() + ": " + e.getMessage());
                } else {
                    for(Player pl : Bukkit.getOnlinePlayers())
                        pl.sendMessage(ChatColor.GRAY + p.getName() + ": " + e.getMessage());
                }
                break;
            case PRECOMBAT:
            case COMBAT:
                if(p.getGameMode() == GameMode.SPECTATOR) {
                    for(Player pl : Bukkit.getOnlinePlayers()) {
                        if(pl.getGameMode() == GameMode.SPECTATOR)
                            pl.sendMessage(ChatColor.DARK_GRAY + "DEAD " + ChatColor.GRAY + p.getName() + ": " + e.getMessage());
                    }
                } else if(p.getName().equals(lastWinner)) {
                    for(Player pl : Bukkit.getOnlinePlayers())
                        pl.sendMessage(CC.VIVID_BLUE_SKY + "" + ChatColor.BOLD + "WINNER " + CC.EGGSHELL + p.getName() + ": " + e.getMessage());
                } else {
                    for(Player pl : Bukkit.getOnlinePlayers())
                        pl.sendMessage(ChatColor.GRAY + p.getName() + ": " + e.getMessage());
                }

                break;
            case WINNER:
                if(p.getGameMode() == GameMode.SPECTATOR) {
                    for(Player pl : Bukkit.getOnlinePlayers())
                        pl.sendMessage(ChatColor.DARK_GRAY + "DEAD " + ChatColor.GRAY + p.getName() + ": " + e.getMessage());
                } else if(p.getName().equals(lastWinner)) {
                    for(Player pl : Bukkit.getOnlinePlayers())
                        pl.sendMessage(CC.VIVID_BLUE_SKY + "" + ChatColor.BOLD + "WINNER " + CC.EGGSHELL + p.getName() + ": " + e.getMessage());
                }
                break;
        }
    }

    @NotNull
    public GamePhase getGamePhase() {
        return phase;
    }

    /**
     * @return Number of seconds passed in current game phase.
     */
    public int getTicker() {
        return ticker;
    }

    @NotNull
    public String getLastWinner() {
        return lastWinner;
    }

    private int getSuggestedMapLength() {
        int players = Bukkit.getOnlinePlayers().size();
        if(players >= 100)
            return 800;
        if(players >= 50)
            return 600;
        if(players >= 25)
            return 400;

        return 200;
    }

    public static Game getInstance() {
        if(instance == null)
            instance = new Game();

        return instance;
    }
}

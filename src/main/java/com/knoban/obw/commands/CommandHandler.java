package com.knoban.obw.commands;

import com.knoban.atlas.commandsII.ACAPI;
import com.knoban.atlas.commandsII.annotations.AtlasCommand;
import com.knoban.obw.OneBlockWide;
import com.knoban.obw.general.CC;
import org.bukkit.Difficulty;
import org.bukkit.GameRule;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class CommandHandler {

    private final OneBlockWide oneBlockWide;

    public CommandHandler(OneBlockWide oneBlockWide) {
        this.oneBlockWide = oneBlockWide;
        ACAPI.getApi().registerCommandsFromClass(oneBlockWide, CommandHandler.class, this);
    }

    @AtlasCommand(paths = "setlobby", permission = "obw.admin")
    public void cmdSetlobby(Player sender) {
        oneBlockWide.setSpawnLocation(sender.getLocation().clone().add(0, 0.5, 0));
        sender.sendMessage(CC.NEON_BLUE + "Game> " + CC.VIVID_BLUE_SKY + "The spawn location has been updated.");
        sender.playSound(sender.getLocation(), Sound.BLOCK_END_PORTAL_FRAME_FILL, 1F, 1F);
    }

    @AtlasCommand(paths = "setlobby w/fullsetup", permission = "obw.admin")
    public void cmdSetlobbyWithFullSetup(Player sender) {
        cmdSetlobby(sender);

        World world = sender.getWorld();
        world.setAmbientSpawnLimit(0);
        world.setAnimalSpawnLimit(0);
        world.setMonsterSpawnLimit(0);
        world.setWaterAmbientSpawnLimit(0);
        world.setWaterAnimalSpawnLimit(0);

        world.setPVP(false);
        world.setDifficulty(Difficulty.PEACEFUL);
        world.setViewDistance(8);

        world.setGameRule(GameRule.DO_IMMEDIATE_RESPAWN, true);
        world.setGameRule(GameRule.DO_TRADER_SPAWNING, false);
        world.setGameRule(GameRule.DO_PATROL_SPAWNING, false);
        world.setGameRule(GameRule.DO_MOB_SPAWNING, false);
        world.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);
        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        world.setGameRule(GameRule.SPECTATORS_GENERATE_CHUNKS, false);
        world.setGameRule(GameRule.DO_WEATHER_CYCLE, false);

        world.setThundering(false);
        world.setStorm(false);
        world.setTime(6000);
    }
}

package com.knoban.atlas.commandsII.parsables;

import com.knoban.atlas.commandsII.ACParsable;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Alden Bansemer (kNoAPP)
 */
public class PlayerParsable implements ACParsable<Player> {

    @Nullable
    @Override
    public Player parse(@NotNull CommandSender sender, @NotNull String arg) {
        return Bukkit.getPlayerExact(arg);
    }

    @Nullable
    @Override
    public List<String> defaultSuggestions(@NotNull CommandSender sender) {
        List<String> suggestions = new ArrayList<>();
        for(Player pl : Bukkit.getOnlinePlayers()) {
            suggestions.add(pl.getName());
        }
        return suggestions;
    }
}

package com.knoban.atlas.commandsII.parsables;

import com.knoban.atlas.commandsII.ACParsable;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class OfflinePlayerParsable implements ACParsable<OfflinePlayer> {

    @Nullable
    @Override
    public OfflinePlayer parse(@NotNull CommandSender sender, @NotNull String arg) {
        return Bukkit.getOfflinePlayer(arg);
    }

    @Override
    public boolean likelyMatch(@NotNull CommandSender sender, @NotNull String arg, @Nullable String filter) {
        return arg.length() >= 3;
    }
}

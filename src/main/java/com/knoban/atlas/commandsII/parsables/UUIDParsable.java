package com.knoban.atlas.commandsII.parsables;

import com.knoban.atlas.commandsII.ACParsable;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class UUIDParsable implements ACParsable<UUID> {

    @Nullable
    @Override
    public UUID parse(@NotNull CommandSender sender, @NotNull String arg) {
        // Try-catch this to make sure no exceptions get thrown if an invalid uuid gets parsed
        try {
            // Return the parsed uuid.
            return UUID.fromString(arg);
        } catch (Exception e) {
            return null;
        }
    }
}

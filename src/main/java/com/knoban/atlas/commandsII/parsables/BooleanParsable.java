package com.knoban.atlas.commandsII.parsables;

import com.knoban.atlas.commandsII.ACParsable;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * @author Alden Bansemer (kNoAPP)
 */
public class BooleanParsable implements ACParsable<Boolean> {

    @Nullable
    @Override
    public Boolean parse(@NotNull CommandSender sender, @NotNull String arg) {
        if(arg.equalsIgnoreCase("true"))
            return true;
        else if(arg.equalsIgnoreCase("false"))
            return false;
        else
            return null;
    }

    private static final List<String> SUGGESTIONS = Arrays.asList("true", "false");

    @Nullable
    @Override
    public List<String> defaultSuggestions(@NotNull CommandSender sender) {
        return SUGGESTIONS;
    }

    @NotNull
    @Override
    public Optional<String> getOvercastName() {
        return Optional.of("True/False");
    }
}

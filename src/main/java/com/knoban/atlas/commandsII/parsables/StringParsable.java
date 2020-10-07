package com.knoban.atlas.commandsII.parsables;

import com.knoban.atlas.commandsII.ACParsable;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Alden Bansemer (kNoAPP)
 */
public class StringParsable implements ACParsable<String> {

    @Nullable
    @Override
    public String parse(@NotNull CommandSender sender, @NotNull String arg) {
        return arg;
    }

    public static final String REGEX_FILTER = "regex:";

    @Override
    public String filter(@NotNull CommandSender sender, @NotNull String parsed, @NotNull String filter) {
        String[] fields = filter.split(",");
        for(String field : fields) {
            if(field.startsWith(REGEX_FILTER)) {
                String regex = field.substring(REGEX_FILTER.length());
                if(!parsed.matches(regex))
                    return "Must match " + regex + ".";
            }
        }

        return null;
    }
}

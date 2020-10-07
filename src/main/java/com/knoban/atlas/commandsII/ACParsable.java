package com.knoban.atlas.commandsII;

import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * @author Alden Bansemer (kNoAPP)
 */
public interface ACParsable<T> {

    /**
     * Parse a command argument into an instance of an implemented object.
     * Runs on an async thread for command execution.
     * Isn't used for generating suggestions. See {@link #likelyMatch(CommandSender, String, String)}
     *
     * @param sender The sender of the command
     * @param arg The command argument
     * @return The instance of the parsed object or null if the String isn't able to be parsed.
     */
    @Nullable
    T parse(@NotNull CommandSender sender, @NotNull String arg);

    /**
     * Checks if the passed argument is a valid contender for a command.
     * Runs on the game thread when command suggestions are generated.
     * Isn't used for command execution. See {@link #parse(CommandSender, String)}
     *
     * If this returns true, ACv2 will assume there's a chance the argument is valid and further
     * suggestions will be provided to the player.
     *
     * Note that even if this returns true and {@link #parse(CommandSender, String)} returns false for the same argument,
     * command execution will handle it gracefully.
     *
     * @param sender The sender of the command
     * @param arg The command argument
     * @param filter The filter attached to this command (null if no filter is present)
     * @return True, if there's a chance this argument matches.
     */
    default boolean likelyMatch(@NotNull CommandSender sender, @NotNull String arg, @Nullable String filter) {
        T parsed = parse(sender, arg);
        if(parsed == null)
            return false;

        return filter == null || filter(sender, parsed, filter) == null;
    }

    /**
     * Filter the parsed value through the passed filter.
     * Runs on the main thread for suggestion checking.
     * Runs on an async thread for command execution.
     *
     * @param parsed A parsed object passed through {@link #parse(CommandSender, String)}
     * @param filter A filter defined in {@link com.knoban.atlas.commandsII.annotations.AtlasParam}
     * @return null, if the parsed object passes the filter. Otherwise return a string indicating why the parsed value
     * failed the filter.
     */
    @Nullable
    default String filter(@NotNull CommandSender sender, @NotNull T parsed, @NotNull String filter) {
        return null;
    }

    /**
     * This will run on the main game thread.
     * @param sender The sender of the possible command
     * @return A list of suggestions or null if none.
     */
    @Nullable
    default List<String> defaultSuggestions(@NotNull CommandSender sender) {
        return null;
    }

    /**
     * This String is what appears in the generated help menu. By default, it is the name of the class.
     * @return The overcasted type for the current argument
     */
    @Nullable
    default Optional<String> getOvercastName() {
        return Optional.empty();
    }
}

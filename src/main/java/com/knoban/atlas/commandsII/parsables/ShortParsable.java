package com.knoban.atlas.commandsII.parsables;

import com.knoban.atlas.commandsII.ACParsable;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ShortParsable implements ACParsable<Short> {

    @Nullable
    @Override
    public Short parse(@NotNull CommandSender sender, @NotNull String arg) {

        // Try-catch this to prevent any NumberFormatExceptions from parsing the argument!
        try {
            return Short.parseShort(arg);
        } catch (NumberFormatException e) {
            return null;
        }

    }

    public static final String RANGE_FILTER = "range:"; // Inclusive (ex. ...,range:-23to47,...)
    public static final String MIN_FILTER = "min:"; // Inclusive (ex. ...,min:-100,...)
    public static final String MAX_FILTER = "max:"; // Inclusive (ex. ...,max:999,...)

    @Override
    public @Nullable String filter(@NotNull CommandSender sender, @NotNull Short parsed, @NotNull String filter) {
        String[] fields = filter.split(",");
        for (String field : fields) {
            if (field.startsWith(RANGE_FILTER)) {
                String unparsed = field.substring(RANGE_FILTER.length());
                String[] minMax = unparsed.split("to");
                if (minMax.length != 2) {
                    System.out.println("AtlasParam filter contains an unrecognizable range value!");
                    System.out.println(Thread.currentThread().getStackTrace());
                    return "Internal error. See Console.";
                }

                try {
                    Short min = Short.parseShort(minMax[0]);
                    Short max = Short.parseShort(minMax[1]);
                    if (parsed < min || max < parsed)
                        return "Must be between " + min + " and " + max + ".";
                } catch (NumberFormatException e) {
                    System.out.println("AtlasParam filter contains an unrecognizable range value!");
                    e.printStackTrace();
                    return "Internal error. See Console.";
                }
            } else if (field.startsWith(MIN_FILTER)) {
                String unparsed = field.substring(MIN_FILTER.length());
                try {
                    Short min = Short.parseShort(unparsed);
                    if (parsed < min)
                        return "Must be greater than " + min + ".";
                } catch (NumberFormatException e) {
                    System.out.println("AtlasParam filter contains an unrecognizable min value!");
                    e.printStackTrace();
                    return "Internal error. See Console.";
                }
            } else if (field.startsWith(MAX_FILTER)) {
                String unparsed = field.substring(MAX_FILTER.length());
                try {
                    Short max = Short.parseShort(unparsed);
                    if (parsed > max)
                        return "Must be less than " + max + ".";
                } catch (NumberFormatException e) {
                    System.out.println("AtlasParam filter contains an unrecognizable max value!");
                    e.printStackTrace();
                    return "Internal error. See Console.";
                }
            }
        }

        return null;
    }
}

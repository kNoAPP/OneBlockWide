package com.knoban.atlas.commandsII.parsables;

import com.knoban.atlas.commandsII.ACParsable;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class CalendarParsable implements ACParsable<Calendar> {

    // y mo d h m s
    @Nullable
    @Override
    public Calendar parse(@NotNull CommandSender sender, @NotNull String arg) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeZone(TimeZone.getTimeZone("America/New_York")); // Everything on GC runs off EST by default.
        char[] characters = arg.toCharArray();
        int firstIndex = 0, secondIndex = 0;
        for(int i=0; i<characters.length; i++) {
            char current = characters[i];
            if(48 <= current && current <= 57) {
                secondIndex++;
                continue;
            }

            if(firstIndex == secondIndex)
                return null;

            int parsed = Integer.parseInt(arg.substring(firstIndex, secondIndex));

            switch(current) {
                case 's':
                    if(!(0 <= parsed && parsed < 60))
                        return null;
                    calendar.set(Calendar.SECOND, parsed);
                    break;
                case 'm':
                    if(i+1 < characters.length && characters[i+1] == 'o') {
                        ++i;
                        if(!(0 <= parsed && parsed < 12))
                            return null;
                        calendar.set(Calendar.MONTH, parsed);
                    } else {
                        if(!(0 <= parsed && parsed < 60))
                            return null;
                        calendar.set(Calendar.MINUTE, parsed);
                    }
                    break;
                case 'h':
                    if(!(0 <= parsed && parsed < 24))
                        return null;
                    calendar.set(Calendar.HOUR_OF_DAY, parsed);
                    break;
                case 'd':
                    if(!(1 <= parsed && parsed <= 31))
                        return null;
                    calendar.set(Calendar.DAY_OF_MONTH, parsed);
                    break;
                case 'y':
                    calendar.set(Calendar.YEAR, parsed);
                    break;
                default:
                    return null;
            }

            firstIndex = secondIndex = i;
        }

        if(firstIndex != secondIndex)
            return null;

        return calendar;
    }

    @Nullable
    @Override
    public List<String> defaultSuggestions(@NotNull CommandSender sender) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeZone(TimeZone.getTimeZone("America/New_York")); // Everything on GC runs off EST by default.
        StringBuilder sb = new StringBuilder();
        sb.append(calendar.get(Calendar.SECOND));
        sb.append("s");
        sb.append(calendar.get(Calendar.MINUTE));
        sb.append("m");
        sb.append(calendar.get(Calendar.HOUR_OF_DAY));
        sb.append("h");
        sb.append(calendar.get(Calendar.DAY_OF_MONTH));
        sb.append("d");
        sb.append(calendar.get(Calendar.MONTH));
        sb.append("mo");
        sb.append(calendar.get(Calendar.YEAR));
        sb.append("y");
        return Arrays.asList(sb.toString());
    }

    @Nullable
    @Override
    public Optional<String> getOvercastName() {
        return Optional.of("Date");
    }
}

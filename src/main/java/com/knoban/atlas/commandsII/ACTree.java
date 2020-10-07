package com.knoban.atlas.commandsII;

import co.aikar.timings.Timing;
import co.aikar.timings.Timings;
import com.knoban.atlas.commandsII.annotations.AtlasParam;
import com.knoban.atlas.structure.PQEntry;
import com.knoban.atlas.structure.Pair;
import com.knoban.obw.OneBlockWide;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;

/**
 * @author Alden Bansemer (kNoAPP)
 */
public class ACTree {

    private final ACNode<?> root = new ACNode<>("/"); // Root of the command tree

    public void addCommand(JavaPlugin plugin, Method m, Object instance, String permission, String... paths) {
        for(String path : paths) {
            Queue<Object> objQueue = new LinkedList<>();
            int paramIndex = 1; // Param index 0 is reserved for the sender of the command
            for(String arg : path.split(" ")) {
                if(arg.equals("<?>")) {
                    if(paramIndex >= m.getParameters().length)
                        throw new IllegalArgumentException("Could not register command " + m.getName()
                                + "! AtlasCommand 'paths' field contains too many <?> args or the method does not have enough parameters.");
                    Parameter param = m.getParameters()[paramIndex++];
                    objQueue.offer(new Pair<Class<?>, AtlasParam>(param.getType(), param.getAnnotation(AtlasParam.class)));
                } else
                    objQueue.offer(arg);
            }

            for(; paramIndex < m.getParameters().length; paramIndex++) {
                Parameter param = m.getParameters()[paramIndex];
                objQueue.offer(new Pair<Class<?>, AtlasParam>(param.getType(), param.getAnnotation(AtlasParam.class)));
            }

            ACNode<?> current = root;
            while(!objQueue.isEmpty()) {
                Object search = objQueue.poll();
                int index = current.indexOf(search);
                if(index != -1) {
                    current = current.children.get(index);
                    if(search instanceof Pair) {
                        Pair<Class<?>, AtlasParam> pair = (Pair<Class<?>, AtlasParam>) search;
                        assert pair.getKey() != null;
                        if(pair.getValue() != null)
                            current.optionalSuggestions.addAll(Arrays.asList(pair.getValue().suggestions()));
                    }
                } else if(search instanceof String) {
                    ACNode<?> newNode = new ACNode<>((String) search);
                    current.children.add(newNode);
                    current = newNode;
                } else {
                    Pair<Class<?>, AtlasParam> pair = (Pair<Class<?>, AtlasParam>) search;
                    assert pair.getKey() != null;
                    ACNode<?> newNode = new ACNode<>(pair.getKey(), pair.getValue());
                    current.children.add(newNode);
                    current = newNode;
                    if(pair.getValue() != null)
                        current.optionalSuggestions.addAll(Arrays.asList(pair.getValue().suggestions()));
                }
            }

            if(current.fulfillment != null) {
                Bukkit.getLogger().info("Command " + current.fulfillment.getName() + " is being replaced by " + m.getName() + "!");
                // Do not optimize tree since we're about to reuse it.
                removeCommand(current.fulfillment, false, path);
            }

            current.fulfillment = m;
            current.fulfillmentTiming = Timings.of(plugin, "AtlasCommand - execute - " + m.getDeclaringClass().getName() + "#" + m.getName());
            current.instance = instance;
            current.permission = permission;
        }
    }

    public boolean containsCommand(@NotNull Method m, @NotNull String path) {
        Queue<Object> objQueue = new LinkedList();
        int paramIndex = 1; // Param index 0 is reserved for the sender of the command
        for(String arg : path.split(" ")) {
            if(arg.equals("<?>")) {
                if(paramIndex >= m.getParameters().length)
                    throw new IllegalArgumentException("Could not register command " + m.getName()
                            + "! AtlasCommand 'paths' field contains too many <?> args or the method does not have enough parameters.");
                Parameter param = m.getParameters()[paramIndex++];
                objQueue.offer(new Pair<Class<?>, AtlasParam>(param.getType(), param.getAnnotation(AtlasParam.class)));
            } else
                objQueue.offer(arg);
        }

        for(; paramIndex < m.getParameters().length; paramIndex++) {
            Parameter param = m.getParameters()[paramIndex];
            objQueue.offer(new Pair<Class<?>, AtlasParam>(param.getType(), param.getAnnotation(AtlasParam.class)));
        }

        ACNode<?> current = root;
        while(!objQueue.isEmpty()) {
            Object search = objQueue.poll();
            int index = current.indexOf(search);
            if(index != -1) {
                current = current.children.get(index);
            } else
                return false;
        }

        return current.fulfillment != null;
    }

    public void removeCommand(Method m, String... paths) {
        removeCommand(m, true, paths);
    }

    public void removeCommand(Method m, boolean optimizeTree, String... paths) {
        for(String path : paths) {
            Queue<Object> objQueue = new LinkedList();
            int paramIndex = 1; // Param index 0 is reserved for the sender of the command
            for(String arg : path.split(" ")) {
                if(arg.equals("<?>")) {
                    if(paramIndex >= m.getParameters().length)
                        throw new IllegalArgumentException("Could not register command " + m.getName()
                                + "! AtlasCommand 'paths' field contains too many <?> args or the method does not have enough parameters.");
                    Parameter param = m.getParameters()[paramIndex++];
                    objQueue.offer(new Pair<Class<?>, AtlasParam>(param.getType(), param.getAnnotation(AtlasParam.class)));
                } else
                    objQueue.offer(arg);
            }

            for(; paramIndex < m.getParameters().length; paramIndex++) {
                Parameter param = m.getParameters()[paramIndex];
                objQueue.offer(new Pair<Class<?>, AtlasParam>(param.getType(), param.getAnnotation(AtlasParam.class)));
            }

            List<Pair<ArrayList<String>, String[]>> suggestionsToRemove = new ArrayList<>();
            Stack<ACNode<?>> reversal = new Stack<>();
            ACNode<?> current = root;
            reversal.push(current);
            while(!objQueue.isEmpty()) {
                Object search = objQueue.poll();
                int index = current.indexOf(search);
                if(index != -1) {
                    current = current.children.get(index);
                    if(search instanceof Pair) {
                        Pair<Class<?>, AtlasParam> pair = (Pair<Class<?>, AtlasParam>) search;
                        assert pair.getKey() != null;
                        if(pair.getValue() != null)
                            suggestionsToRemove.add(new Pair<>(current.optionalSuggestions, pair.getValue().suggestions()));
                    }
                } else
                    return; // Command does not exist.
                reversal.push(current);
            }

            current.fulfillment = null;
            current.fulfillmentTiming = null;
            current.instance = null;
            current.permission = null;
            for(Pair<ArrayList<String>, String[]> suggestionToRemove : suggestionsToRemove) {
                ArrayList<String> currentSuggestions = suggestionToRemove.getKey();
                assert suggestionToRemove.getValue() != null;
                for(String suggestion : suggestionToRemove.getValue()) {
                    assert currentSuggestions != null;
                    currentSuggestions.remove(suggestion);
                }
            }

            if(optimizeTree) {
                while(reversal.size() >= 2) {
                    ACNode<?> toRemove = reversal.pop();
                    if(toRemove.children.size() > 0)
                        return;

                    ACNode<?> removeFrom = reversal.peek();
                    removeFrom.children.remove(toRemove);
                }
            }
        }
    }

    public boolean execute(CommandSender sender, String label, String... args) {
        int index = root.indexOf(label);
        if(index == -1) { // Special case
            sender.sendMessage("§7Command not found. Try /help for more commands.");
            return false;
        }

        ACNode<?> current = root.children.get(index);

        LinkedList<ACNode<?>> path = new LinkedList<>();
        LinkedList<Object> argsToPass = new LinkedList<>();
        PriorityQueue<PQEntry<ACNode<?>[]>> errors = new PriorityQueue<>(Comparator.reverseOrder());
        argsToPass.addFirst(sender);
        path.addLast(current);
        if(run(sender, path, argsToPass, errors, current, 0, args))
            return true;
        path.removeLast();

        sender.sendMessage("");
        if(errors.size() == 0) {
            sender.sendMessage("§7No help available for this command.");
            return false;
        } else
            sender.sendMessage("§7§oCommand help");

        int maxDepth = -1;
        String lastError = "";
        PriorityQueue<String> errorTrace = new PriorityQueue<>(Comparator.comparingInt(o -> o.split(" ").length));
        while(!errors.isEmpty()) {
            PQEntry<ACNode<?>[]> pqEntry = errors.poll();
            if(maxDepth <= pqEntry.getPriority()) {
                maxDepth = pqEntry.getPriority();

                if(!lastError.equals(pqEntry.getExtra())) {
                    while(!errorTrace.isEmpty()) {
                        sender.sendMessage(errorTrace.poll());
                    }
                    sender.sendMessage("§cWarning: §7" + pqEntry.getExtra());
                    lastError = pqEntry.getExtra();
                }

                ACNode<?>[] pathToError = pqEntry.getValue();
                StringBuilder attemptedCommand = new StringBuilder(pathToError.length > 1 ? "§7/" : "§4/");
                for(int i=0; i<pathToError.length-1; i++) {
                    attemptedCommand.append(pathToError[i].getOvercast());
                    attemptedCommand.append(" ");
                }
                attemptedCommand.append("§4");

                List<String> possibleCommands = generatePossibleCommands(pathToError[pathToError.length - 1]);
                for(int i=0; i<possibleCommands.size(); i++) {
                    String possibleCommand = possibleCommands.get(i).replaceFirst(" ", " §e");
                    StringBuilder combinedCommand = new StringBuilder(attemptedCommand.toString());
                    combinedCommand.append(possibleCommand);
                    errorTrace.offer(combinedCommand.toString());
                }
            } else
                break; // Discard remaining errors... we only want to print the deepest reached commands.
        }

        // Print remaining trace
        while(!errorTrace.isEmpty()) {
            sender.sendMessage(errorTrace.poll());
        }

        return true;
    }

    private boolean run(CommandSender sender, LinkedList<ACNode<?>> path, LinkedList<Object> parsed,
                        PriorityQueue<PQEntry<ACNode<?>[]>> errors, ACNode<?> current, int depth, String... args) {
        if(depth == args.length) {
            if(current.fulfillment != null) {
                if(current.fulfillment.getParameters()[0].getType().isAssignableFrom(sender.getClass())) {
                    if(current.permission == null || current.permission.length() == 0 || sender.hasPermission(current.permission)) {
                        class AtlasCommandExecute extends BukkitRunnable { //using local class to name the scheduled task in timings
                            @Override
                            public void run() {
                                current.fulfillmentTiming.startTiming();
                                try {
                                    current.fulfillment.invoke(current.instance, parsed.toArray(new Object[0]));
                                } catch (IllegalAccessException | InvocationTargetException e) {
                                    System.out.println("Should have been able to execute AC command. Something went wrong?");
                                    e.printStackTrace();
                                    PQEntry<ACNode<?>[]> pqEntry = new PQEntry<>(path.toArray(new ACNode<?>[0]), Integer.MAX_VALUE);
                                    pqEntry.setExtra("kNoAPP's code shit the bed. See Console for details.");
                                    errors.offer(pqEntry);
                                } finally {
                                    current.fulfillmentTiming.stopTiming();
                                }
                            }
                        }
                        new AtlasCommandExecute().runTask(OneBlockWide.getInstance());
                        return true;
                    } else {
                        PQEntry<ACNode<?>[]> pqEntry = new PQEntry<>(path.toArray(new ACNode<?>[0]), Integer.MAX_VALUE - 1);
                        pqEntry.setExtra("No permission.");
                        errors.offer(pqEntry);
                    }
                } else {
                    PQEntry<ACNode<?>[]> pqEntry = new PQEntry<>(path.toArray(new ACNode<?>[0]), Integer.MAX_VALUE - 2);
                    pqEntry.setExtra("Cannot be run by a " + sender.getClass().getSimpleName() + ".");
                    errors.offer(pqEntry);
                }
            } else {
                PQEntry<ACNode<?>[]> pqEntry = new PQEntry<>(path.toArray(new ACNode<?>[0]), Integer.MAX_VALUE - 3);
                pqEntry.setExtra("Not enough arguments.");
                errors.offer(pqEntry);
            }
            return false;
        }

        if(current.fulfillment != null) {
            PQEntry<ACNode<?>[]> pqEntry = new PQEntry<>(path.toArray(new ACNode<?>[0]), depth);
            pqEntry.setExtra("Too many arguments.");
            errors.offer(pqEntry);
        }

        if(current.children.size() <= 0) {
            return false;
        }

        for(ACNode<?> child : current.children) {
            path.addLast(child);
            Pair<String, ?> parsedWithReason = child.parseAndMatchWithReason(sender, args[depth]);
            String reason = parsedWithReason.getKey();
            if(reason == null) {
                Object parsedObj = parsedWithReason.getValue();
                if(parsedObj != null) {
                    parsed.addLast(parsedObj);
                    if(run(sender, path, parsed, errors, child, depth + 1, args))
                        return true;
                    parsed.removeLast();
                } else if(run(sender, path, parsed, errors, child, depth + 1, args)) {
                    return true;
                }
            } else {
                PQEntry<ACNode<?>[]> pqEntry = new PQEntry<>(path.toArray(new ACNode<?>[0]), depth + 1);
                pqEntry.setExtra(reason);
                errors.offer(pqEntry);
            }
            path.removeLast();
        }

        return false;
    }

    public List<String> generatePossibleCommands() {
        return generatePossibleCommands(root);
    }

    private List<String> generatePossibleCommands(ACNode<?> current) {
        List<String> possibleCommands = generatePossibleCommandsRunner(current);
        return possibleCommands;
    }

    private List<String> generatePossibleCommandsRunner(ACNode<?> current) {
        List<String> help = new ArrayList<>();
        if(current.fulfillment != null) {
            help.add(current.getOvercast());
        }

        for(ACNode<?> child : current.children) {
            for(String childRequires : generatePossibleCommandsRunner(child))
                help.add(current.getOvercast() + " " + childRequires);
        }

        return help;
    }

    public List<String> getSuggestions(CommandSender sender, String label, String... partialArgs) {
        int index = root.indexOf(label);
        if(index == -1)
            return new ArrayList<>();

        ACNode<?> current = root.children.get(index);
        List<String> suggestions = gather(sender, current, 0, partialArgs);

        return suggestions;
    }

    private List<String> gather(CommandSender sender, ACNode<?> current, int index, String... partialArgs) {
        List<String> suggestions = new ArrayList<>();
        if(index == partialArgs.length - 1) {
            suggestions.addAll(current.getSuggestions(sender));
            suggestions.removeIf((s) -> !(s.regionMatches(true, 0, partialArgs[index], 0, partialArgs[index].length())
                    || s.startsWith("#")));
            return suggestions;
        }

        for(ACNode<?> child : current.children) {
            if(child.likelyMatches(sender, partialArgs[index]))
                suggestions.addAll(gather(sender, child, index + 1, partialArgs));
        }

        return suggestions;
    }

    private static class ACNode<T> {

        private String requiredArg; // Every ACNode needs either this as non-null...

        private Class<T> requiredClass; // ...or this as non-null. They can never both be null or non-null.
        private String optionalFilter;
        private final ArrayList<String> optionalSuggestions;

        private Method fulfillment;
        private Timing fulfillmentTiming;
        private Object instance;
        private String permission;

        private final ArrayList<ACNode<?>> children;

        private ACNode(@NotNull Class<T> requiredClass, @Nullable AtlasParam atlasParam) {
            if(ACAPI.getApi().getParsable(requiredClass) == null) {
                throw new IllegalArgumentException(requiredClass.getSimpleName() + " does not have an associated ACParsable!");
            }

            this.requiredClass = requiredClass;
            if(atlasParam != null)
                this.optionalFilter = atlasParam.filter();
            this.optionalSuggestions = new ArrayList<>();
            this.children = new ArrayList<>();
        }

        private ACNode(@NotNull String requiredArg) {
            this.requiredArg = requiredArg;
            this.optionalSuggestions = new ArrayList<>();
            this.children = new ArrayList<>();
        }

        public boolean matches(@NotNull CommandSender sender, @NotNull String arg) {
            return matchesWithReason(sender, arg) == null;
        }

        @Nullable
        public String matchesWithReason(@NotNull CommandSender sender, @NotNull String arg) {
            return parseAndMatchWithReason(sender, arg).getKey();
        }

        @NotNull
        public Pair<String, T> parseAndMatchWithReason(@NotNull CommandSender sender, @NotNull String arg) {
            if(arg.equalsIgnoreCase(requiredArg))
                return new Pair<>(null, null);

            if(requiredClass == null)
                return new Pair<>("No match.", null);

            ACParsable<T> parsable = (ACParsable<T>) ACAPI.getApi().getParsable(requiredClass);
            T parsed = parsable.parse(sender, arg);
            if(parsed == null) {
                return new Pair<>("Not a " + parsable.getOvercastName().orElse(requiredClass.getSimpleName()) + ".", null);
            }
            else if(optionalFilter != null) {
                return new Pair<>(parsable.filter(sender, parsed, optionalFilter), parsed);
            } else
                return new Pair<>(null, parsed);
        }

        @Nullable
        public T parse(@NotNull CommandSender sender, @NotNull String arg) {
            if(requiredClass == null)
                return null;

            ACParsable<T> parsable = (ACParsable<T>) ACAPI.getApi().getParsable(requiredClass);
            return parsable.parse(sender, arg);
        }

        /**
         * Doesn't actually try to parse the arg. This call is faster generally than
         * {@link #matches(CommandSender, String)}
         */
        public boolean likelyMatches(@NotNull CommandSender sender, @NotNull String arg) {
            if(arg.equalsIgnoreCase(requiredArg))
                return true;

            if(requiredClass == null)
                return false;

            ACParsable<T> parsable = (ACParsable<T>) ACAPI.getApi().getParsable(requiredClass);
            return parsable.likelyMatch(sender, arg, optionalFilter);
        }

        @NotNull
        public String getOvercast() {
            if(requiredArg != null)
                return requiredArg;
            else {
                StringBuilder sb = new StringBuilder();
                sb.append("<");
                ACParsable<T> parsable = (ACParsable<T>) ACAPI.getApi().getParsable(requiredClass);
                sb.append(parsable.getOvercastName().orElse(requiredClass.getSimpleName()));
                sb.append(">");
                /* Maybe add back later?
                if(optionalFilter != null && optionalFilter.length() > 0) {
                    sb.append("(");
                    sb.append(optionalFilter);
                    sb.append(")");
                }
                */
                return sb.toString();
            }
        }

        @NotNull
        public HashSet<String> getSuggestions(@NotNull CommandSender sender) {
            HashSet<String> suggestions = new HashSet<>();
            for(ACNode<?> child : children) {
                if(child.requiredClass != null) {
                    if(child.optionalSuggestions.size() <= 0) {
                        ACParsable<T> parsable = (ACParsable<T>) ACAPI.getApi().getParsable(child.requiredClass);
                        List<String> defaultSuggestions = parsable.defaultSuggestions(sender);
                        if(defaultSuggestions != null) {
                            //defaultSuggestions.removeIf((s) -> !matches(sender, s)); // Check against filter
                            suggestions.addAll(defaultSuggestions);
                            continue;
                        }
                    } else {
                        suggestions.addAll(child.optionalSuggestions);
                        continue;
                    }
                }
                suggestions.add(child.getOvercast());
            }

            return suggestions;
        }

        /*
        private void findAndReplaceReflection(CommandSender sender, List<String> suggestions) {
            for(int i=0; i<suggestions.size(); i++) {
                String suggestion = suggestions.get(i);
                if(suggestion.startsWith("#") && suggestion.contains(":")) {
                    String[] reflectionLocation = suggestion.substring(1).split(":");
                    try {
                        Class c = Class.forName(reflectionLocation[0]);
                        Method suggestionsHelper = c.getDeclaredMethod(reflectionLocation[1], CommandSender.class);
                        suggestionsHelper.invoke() // Not sure how to get non-statics to work
                    } catch(Exception e) {}
                    suggestions.remove(i);
                }
            }
        }
        */

        // We want ACNodes to match against their stored Strings and Classes/Restrictions.
        // This helps us traverse the tree easier.
        public int indexOf(Object obj) {
            if(!(obj instanceof String || obj instanceof Pair))
                return -1;

            for(int i=0; i<children.size(); i++) {
                ACNode<?> child = children.get(i);
                if(obj instanceof String && child.requiredArg != null && child.requiredArg.equalsIgnoreCase((String) obj))
                    return i;

                if(!(obj instanceof Pair) || child.requiredClass == null)
                    continue;

                Pair<?, ?> pair = (Pair<?, ?>) obj;
                if(!(pair.getKey() instanceof Class<?>) || !(pair.getValue() instanceof AtlasParam))
                    continue;

                Class<?> otherRequiredClass = (Class<?>) pair.getKey();
                AtlasParam tempParam = (AtlasParam) pair.getValue();
                String otherOptionalFilter = tempParam != null ? tempParam.filter() : null;

                if(!child.requiredClass.equals(otherRequiredClass) || (child.optionalFilter == null ^ otherOptionalFilter == null))
                    continue;

                if(child.optionalFilter == null || child.optionalFilter.equalsIgnoreCase(otherOptionalFilter))
                    return i;
            }
            return -1;
        }
    }
}

package com.knoban.atlas.commandsII;

import co.aikar.timings.Timing;
import co.aikar.timings.Timings;
import com.knoban.atlas.commandsII.annotations.AtlasCommand;
import com.knoban.atlas.commandsII.parsables.*;
import com.knoban.atlas.structure.PQEntry;
import com.knoban.obw.OneBlockWide;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * The ACAPI is the *only* way developers should be accessing AtlasCommands v2 (ACv2). Here, you can add/remove commands
 * and add Parsers too for custom Java Objects.
 *
 * ACv2 commands run with different priorities. The higher the command's priority, the more likely it is to run when
 * multiple command executors' arguments have matched. Highest priority is given to commands registered first with
 * ACv2.
 *
 * @author Alden Bansemer (kNoAPP)
 */
public final class ACAPI implements CommandExecutor, TabExecutor {

    private static final ACAPI api = new ACAPI();

    private final HashMap<Class<?>, ACParsable<?>> parsingClasses = new HashMap<>();
    private final ACTree commandTree = new ACTree();
    private final Timing suggestionsTiming = Timings.of(JavaPlugin.getProvidingPlugin(getClass()), "AtlasCommand - suggestions");
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    private ACAPI() {
        // Add default parsing classes
        BooleanParsable booleanParsable = new BooleanParsable();
        addParser(Boolean.class, booleanParsable);
        addParser(boolean.class, booleanParsable);

        DoubleParsable doubleParsable = new DoubleParsable();
        addParser(Double.class, doubleParsable);
        addParser(double.class, doubleParsable);

        FloatParsable floatParsable = new FloatParsable();
        addParser(Float.class, floatParsable);
        addParser(float.class, floatParsable);

        LongParsable longParsable = new LongParsable();
        addParser(Long.class, longParsable);
        addParser(long.class, longParsable);

        IntegerParsable integerParsable = new IntegerParsable();
        addParser(Integer.class, integerParsable);
        addParser(int.class, integerParsable);

        ShortParsable shortParsable = new ShortParsable();
        addParser(Short.class, shortParsable);
        addParser(short.class, shortParsable);

        addParser(Player.class, new PlayerParsable());

        addParser(OfflinePlayer.class, new OfflinePlayerParsable());

        addParser(String.class, new StringParsable());

        addParser(UUID.class, new UUIDParsable());

        addParser(Calendar.class, new CalendarParsable());
    }

    /**
     * Add a way for AC to parse a custom Java Object. Currently by convention, you cannot override a ACParsable
     * once it's been added here. Additionally, added ACParsables may not be removed.
     * @param parseTo Pass the class of the object you're trying to get
     * @param using Pass the ACParsable for ACv2 to use
     * @return True, if the parser was added. False, if a parser for the parseTo parameter already was registered
     */
    public <T> boolean addParser(@NotNull Class<T> parseTo, @NotNull ACParsable<T> using) {
        if(parsingClasses.containsKey(parseTo)) {
            return false;
        }

        parsingClasses.put(parseTo, using);
        return true;
    }

    /**
     * Gets the ACParsable class the passed class uses to parse itself.
     * @param desiredParsedClass Pass the class you seek the parser for
     * @return The ACParsable responsible for delivering the parsed instances
     */
    @Nullable
    public ACParsable<?> getParsable(@NotNull Class<?> desiredParsedClass) {
        return parsingClasses.get(desiredParsedClass);
    }

    /**
     * @return A list of all known commands registered to ACv2
     */
    @NotNull
    public List<String> getAllRegisteredCommands() {
        return commandTree.generatePossibleCommands();
    }

    /**
     * Registers all the commands in the passed class to the passed plugin. Commands are registered in no specific order
     * and that order may change between different runtime environments. If multiple executors match a player's typed
     * arguments, the executor registered first is called. This is not the method you want to use if you care about
     * command priority. Instead, use {@link #registerCommandFromMethod(JavaPlugin, Method)}.
     * <br /><br />
     * If a method's parameters and path match an existing one's, that existing one is overridden with the one you
     * passed.
     * <br /><br />
     * This function will only register static AC methods in the passed class.
     * <br /><br />
     * If you need a specific ordering of priority for the methods within the pass class,
     * use {@link AtlasCommand#classPriority()}. The higher the number, the higher the priority to register in this
     * method.
     *
     * @param plugin An instance of your JavaPlugin
     * @param clazz The class to register all commands in
     */
    public void registerCommandsFromClass(@NotNull JavaPlugin plugin, @NotNull Class<?> clazz) {
        registerCommandsFromClass(plugin, clazz, null);
    }

    /**
     * Registers all the commands in the passed class to the passed plugin. Commands are registered in no specific order
     * and that order may change between different runtime environments. If multiple executors match a player's typed
     * arguments, the executor registered first is called. This is not the method you want to use if you care about
     * command priority. Instead, use {@link #registerCommandFromMethod(JavaPlugin, Method, Object)}.
     * <br /><br />
     * If a method's parameters and path match an existing one's, that existing one is overridden with the one you
     * passed.
     * <br /><br />
     * This function will register both static AC methods and non-statics in the passed class.
     * <br /><br />
     * If you need a specific ordering of priority for the methods within the pass class,
     * use {@link AtlasCommand#classPriority()}. The higher the number, the higher the priority to register in this
     * method.
     *
     * @param plugin An instance of your JavaPlugin
     * @param clazz The class to register all commands in
     * @param instance An instance of clazz to use
     */
    public void registerCommandsFromClass(@NotNull JavaPlugin plugin, @NotNull Class<?> clazz,
                                              @Nullable Object instance) {

        // Account for undetermined behaviour on clazz.getDeclaredMethods()
        PriorityQueue<PQEntry<Method>> methodsToRegister = new PriorityQueue<>(Comparator.reverseOrder());
        for(Method m : clazz.getDeclaredMethods()) {
            AtlasCommand annotation = m.getAnnotation(AtlasCommand.class);
            if(annotation != null) {
                methodsToRegister.offer(new PQEntry<>(m, annotation.classPriority()));
            }
        }

        while(!methodsToRegister.isEmpty()) {
            Method m = methodsToRegister.poll().getValue();
            if(!Modifier.isStatic(m.getModifiers()) && instance != null)
                registerCommandFromMethod(plugin, m, instance);
            else
                registerCommandFromMethod(plugin, m, null);
        }
    }

    /**
     * Registers a single command to ACv2 with the passed plugin.
     * <br /><br />
     * If a method's parameters and path match an existing one's, that existing one is overridden with the one you
     * passed.
     * <br /><br />
     * This function can only register static methods. If you have a non-static, use
     * {@link #registerCommandFromMethod(JavaPlugin, Method, Object)}.
     *
     * @param plugin An instance of your JavaPlugin
     * @param m The method executor to register with
     */
    public void registerCommandFromMethod(@NotNull JavaPlugin plugin, @NotNull Method m) {
        registerCommandFromMethod(plugin, m, null);
    }

    /**
     * Registers a single command to ACv2 with the passed plugin.
     * <br /><br />
     * If a method's parameters and path match an existing one's, that existing one is overridden with the one you
     * passed.
     * <br /><br />
     * This function can only register non-static methods. If you have a static, use
     * {@link #registerCommandFromMethod(JavaPlugin, Method)}.
     *
     * @param plugin An instance of your JavaPlugin
     * @param m The method executor to register with
     * @param instance An instance of m's class
     */
    public void registerCommandFromMethod(@NotNull JavaPlugin plugin, @NotNull Method m, @Nullable Object instance) {
        if(!Modifier.isStatic(m.getModifiers())) {
            if(instance == null) {
                throw new IllegalArgumentException("Could not register command " + m.getName()
                        + "! Non-static command methods must be passed with an instance.");
            }

            if(!m.getDeclaringClass().isInstance(instance)) {
                throw new IllegalArgumentException("Could not register command " + m.getName() + "! "
                        + instance.getClass().getSimpleName() + " is not an instance of " + m.getClass().getSimpleName() + ".");
            }
        } else if(instance != null) {
            throw new IllegalArgumentException("Could not register command " + m.getName()
                    + "! Static command methods should not be passed with an instance.");
        }

        AtlasCommand acAnnotation = m.getAnnotation(AtlasCommand.class);
        if(acAnnotation == null) {
            throw new IllegalArgumentException("Could not register command " + m.getName()
                    + "! Command is missing an AtlasCommand annotation");
        }

        if(acAnnotation.paths().length <= 0) {
            throw new IllegalArgumentException("Could not register command " + m.getName()
                    + "! Command is missing an AtlasCommand 'paths' field.");
        }

        if(m.getParameterTypes().length <= 0 || !CommandSender.class.isAssignableFrom(m.getParameterTypes()[0])) {
            throw new IllegalArgumentException("Could not register command " + m.getName()
                    + "! First parameter of an AtlasCommand must be a CommandSender.");
        }

        for(int i=0; i<m.getParameterTypes().length-1; i++) {
            Class<?> clazz = m.getParameterTypes()[i];
            if(clazz.isArray())
                throw new IllegalArgumentException("Could not register command " + m.getName()
                        + "! Only the final argument of an AtlasCommand can be an array.");
        }

        for(String path : acAnnotation.paths()) {
            if(path.length() <= 0) {
                throw new IllegalArgumentException("Could not register command " + m.getName()
                        + "! AtlasCommand 'paths' field cannot contain empty Strings.");
            }

            String label = path.split(" ")[0];
            if(label.equalsIgnoreCase("<?>"))
                throw new IllegalArgumentException("Could not register command " + m.getName()
                        + "! First arg of the path cannot be a wildcard (<?>).");

            lock.writeLock().lock();
            injectCommand(plugin, acAnnotation, label);
            lock.writeLock().unlock();
        }

        lock.writeLock().lock();
        commandTree.addCommand(plugin, m, instance, acAnnotation.permission(), acAnnotation.paths());
        lock.writeLock().unlock();
    }

    /**
     * Removes all commands in a class from ACv2. They will not be called unless they are re-added. Note that the base
     * of the command will still remain registered, but will give the player a helpful error message if called and
     * no further arguments are still registered.
     * @param clazz The class to remove all commands in
     */
    public void unregisterCommandsFromClass(@NotNull Class<?> clazz) {
        for(Method m : clazz.getDeclaredMethods()) {
            if(m.getAnnotation(AtlasCommand.class) != null) {
                unregisterCommandFromMethod(m);
            }
        }
    }

    /**
     * Removes a command in from ACv2. It will not be called unless it is re-added. Note that the base
     * of the command will still remain registered, but will give the player a helpful error message if called and
     * no further arguments are still registered.
     * @param m A command to remove
     */
    public void unregisterCommandFromMethod(@NotNull Method m) {
        AtlasCommand acAnnotation = m.getAnnotation(AtlasCommand.class);
        if(acAnnotation == null) {
            throw new IllegalArgumentException("Could not unregister command " + m.getName()
                    + "! Command is missing an AtlasCommand annotation");
        }

        if(acAnnotation.paths().length <= 0) {
            throw new IllegalArgumentException("Could not unregister command " + m.getName()
                    + "! Command is missing an AtlasCommand 'paths' field.");
        }

        if(m.getParameterTypes().length <= 0 || !CommandSender.class.isAssignableFrom(m.getParameterTypes()[0])) {
            throw new IllegalArgumentException("Could not unregister command " + m.getName()
                    + "! First parameter of an AtlasCommand must be a CommandSender.");
        }

        for(int i=0; i<m.getParameterTypes().length-1; i++) {
            Class<?> clazz = m.getParameterTypes()[i];
            if(clazz.isArray())
                throw new IllegalArgumentException("Could not unregister command " + m.getName()
                        + "! Only the final argument of an AtlasCommand can be an array.");
        }

        for(String path : acAnnotation.paths()) {
            if(path.length() <= 0) {
                throw new IllegalArgumentException("Could not unregister command " + m.getName()
                        + "! AtlasCommand 'paths' field cannot contain empty Strings.");
            }

            String label = path.split(" ")[0];
            if(label.equalsIgnoreCase("<?>"))
                throw new IllegalArgumentException("Could not unregister command " + m.getName()
                        + "! First arg of the path cannot be a wildcard (<?>).");
        }

        lock.writeLock().lock();
        commandTree.removeCommand(m, true, acAnnotation.paths());
        lock.writeLock().unlock();
    }

    /**
     * Helper method to inject base commands into the Command Map. This way we don't need to declare commands in
     * the plugin.yml file.
     */
    private void injectCommand(@NotNull JavaPlugin plugin, @NotNull AtlasCommand acAnnotation, @NotNull String label) {
        PluginCommand pc = plugin.getCommand(label);
        if(pc == null) {
            try {
                Constructor<PluginCommand> cons = PluginCommand.class.getDeclaredConstructor(String.class, Plugin.class);
                cons.setAccessible(true);
                pc = cons.newInstance(label, plugin);

                pc.setDescription(acAnnotation.description());
                pc.setUsage(acAnnotation.usage());

                Field cmdMap = Bukkit.getPluginManager().getClass().getDeclaredField("commandMap");
                cmdMap.setAccessible(true);
                CommandMap map = (CommandMap) cmdMap.get(Bukkit.getPluginManager());

                map.register(plugin.getName(), pc);
            } catch (NoSuchMethodException | IllegalAccessException |
                    InstantiationException | InvocationTargetException |
                    NoSuchFieldException e) {
                e.printStackTrace();
                plugin.getLogger().warning("Failed to load command: /" + label);
                return;
            }

            plugin.getLogger().info("Successfully loaded command: /" + label);

            pc.setExecutor(this);
            pc.setTabCompleter(this);
        }
    }

    /**
     * Gets the singleton ACAPI for registering commands.
     * @return The ACAPI.
     */
    @NotNull
    public static ACAPI getApi() {
        return api;
    }

    /**
     * Called when a base command we've registered is ran by a CommandSender.
     */
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        Bukkit.getServer().getScheduler().runTaskAsynchronously(OneBlockWide.getInstance(), () -> {
            lock.readLock().lock();
            try {
                commandTree.execute(sender, label, args);
            } finally {
                lock.readLock().unlock();
            }
        });

        return true;
    }

    /**
     * Called when a base command we've registered is typed by a CommandSender.
     */
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        lock.readLock().lock();
        suggestionsTiming.startTiming();
        try {
            return commandTree.getSuggestions(sender, label, args);
        } finally {
            suggestionsTiming.stopTiming();
            lock.readLock().unlock();
        }
    }
}

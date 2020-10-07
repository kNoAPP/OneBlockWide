package com.knoban.atlas.data.local;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.io.FileUtils;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * A UTILITY FOR GENERATING CONFIGURATION FILES FOR A PROGRAM
 * 
 * DataHandler is a simple class that can be used to generated, store,
 * and access configuration for a program. Currently, four file types
 * are supported: .yml, .properties, and .json, and generic files. 
 * Default configuration files are written and placed in src/main/resources. 
 * When the program runs and detects missing configuration files, the 
 * default set are written to a path written below starting from the 
 * program run location.
 * 
 * @author Alden Bansemer (kNoAPP)
 */
public class DataHandler {
	
	protected String subtree, filename, outerPath;
	protected File file;
	protected boolean wasCreated;
	
	public DataHandler(@NotNull Plugin pl, @NotNull String filename) {
		this(pl, "/plugins/" + pl.getName(), filename);
	}
	
	/**
	 * A base constructor for each file type
	 * @param pl A plugin instance to determine where to access data files from
	 * @param subtree Where to drop/access the configuration file (starting from the program run location)
	 * @param filename The name of a configuration file stored in src/main/resources usually preceded by a "/"
	 */
	public DataHandler(@NotNull Plugin pl, @NotNull String subtree, @NotNull String filename) {
		this.subtree = subtree;
		this.filename = filename;
		this.file = new File(System.getProperty("user.dir") + subtree, filename);
		this.outerPath = file.getAbsolutePath();
		
		if(wasCreated = !file.exists()) {
			pl.getLogger().info(this.filename + " not found. Creating...");
			try {
				file.getParentFile().mkdirs();
				exportResource(pl.getClass());
			} catch (Exception e) { e.printStackTrace(); }
		} else pl.getLogger().info(this.filename + " found. Loading...");
	}

	@NotNull
	public File getFile() {
		return file;
	}
	
	/**
	 * @return True, if the default configuration file was generated on startup
	 */
	public boolean wasCreated() {
		return wasCreated;
	}
	
	/**
	 * Export a resource embedded into a Jar file to the local file path
	 */
	private void exportResource(Class resource) throws Exception {
		InputStream stream = null;
		OutputStream resStreamOut = null;

		try {
			stream = resource.getResourceAsStream(filename);
			if(stream == null) throw new Exception("Cannot get resource \"" + filename + "\" from Jar file.");

			int readBytes;
			byte[] buffer = new byte[4096];
			resStreamOut = new FileOutputStream(outerPath);
			while((readBytes = stream.read(buffer)) > 0) resStreamOut.write(buffer, 0, readBytes);
		} finally {
			if(stream != null) stream.close();
			if(resStreamOut != null) resStreamOut.close();
		}
	}
	
	public static class JSON extends DataHandler {
		
		private String cached;
		private Gson gson = new GsonBuilder().setPrettyPrinting().create();
		
		public JSON(@NotNull Plugin pl, @NotNull String filename) {
			super(pl, filename);
		}
		
		/**
		 * A base constructor for each file type
		 * @param pl A plugin instance to determine where to access data files from
		 * @param subtree Where to drop/access the configuration file (starting from the program run location)
		 * @param filename The name of a configuration file stored in src/main/resources usually preceded by a "/"
		 */
		public JSON(@NotNull Plugin pl, @NotNull String subtree, @NotNull String filename) {
			super(pl, subtree, filename);
		}
		
		/**
		 * @param parse The class to parse the Object as
		 * @return The current YML file (will overwrite cached file-- may cause data loss)
		 */
		@NotNull
		public <T> T getJSON(@NotNull Class<T> parse) {
			try {
				BufferedReader br = new BufferedReader(new FileReader(file));
				StringBuilder sb = new StringBuilder();
				for(String line; (line = br.readLine()) != null;)
					sb.append(line);
				br.close();
				cached = sb.toString();
			} catch(IOException e) {
				e.printStackTrace();
			}
			
			return gson.fromJson(cached, parse);
		}
		
		/**
		 * @param parse The class to parse the Object as
		 * @return The cached YML file (saves computation time)
		 */
		@NotNull
		public <T> T getCachedJSON(@NotNull Class<T> parse) {
			if(cached == null) 
				return getJSON(parse);
			
			return gson.fromJson(cached, parse);
		}
		
		/**
		 * Save the given Object as a JSON to the system
		 * @param obj Any JSON-Compatible Object
		 */
		public void saveJSON(@NotNull Object obj) {
			cached = gson.toJson(obj);
			try {
				FileUtils.writeStringToFile(file, cached, StandardCharsets.UTF_8);
			} catch(IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public static class YML extends DataHandler {

		private FileConfiguration cached;
		
		public YML(@NotNull Plugin pl, @NotNull String filename) {
			super(pl, filename);
		}
		
		/**
		 * A base constructor for each file type
		 * @param pl A plugin instance to determine where to access data files from
		 * @param subtree Where to drop/access the configuration file (starting from the program run location)
		 * @param filename The name of a configuration file stored in src/main/resources usually preceded by a "/"
		 */
		public YML(@NotNull Plugin pl, @NotNull String subtree, @NotNull String filename) {
			super(pl, subtree, filename);
		}
		
		/**
		 * @return The current YML file (will overwrite cached file-- may cause data loss)
		 */
		@NotNull
		public FileConfiguration getYML() {
			cached = new YamlConfiguration();
			try {
				cached.load(file);
			} catch (Exception e) { e.printStackTrace(); }
			
			return cached;
		}
		
		/**
		 * @return The cached YML file (saves computation time)
		 */
		@NotNull
		public FileConfiguration getCachedYML() {
			if(cached == null) return getYML();
			return cached;
		}
		
		/**
		 * Save the given YML file to the system
		 */
		public void saveYML() {
			try {
				cached.save(file);
			} catch (IOException e) { e.printStackTrace(); }
		}
	}
	
	public static class PROPS extends DataHandler {
		
		private Properties cached = new Properties();
		
		public PROPS(@NotNull Plugin pl, @NotNull String filename) {
			super(pl, filename);
		}
		
		/**
		 * A base constructor for each file type
		 * @param pl A plugin instance to determine where to access data files from
		 * @param subtree Where to drop/access the configuration file (starting from the program run location)
		 * @param filename The name of a configuration file stored in src/main/resources usually preceded by a "/"
		 */
		public PROPS(@NotNull Plugin pl, @NotNull String subtree, @NotNull String filename) {
			super(pl, subtree, filename);
		}
		
		/**
		 * @return The current properties file (will overwrite cached file-- may cause data loss)
		 */
		@NotNull
		public Properties getProperties() {
			InputStream is;
			try {
				is = new FileInputStream(file);
				cached.load(is);
			} catch(Exception e) { e.printStackTrace(); }
			return cached;
		}
		
		/**
		 * @return The cached properties file (saves computation time)
		 */
		@NotNull
		public Properties getCachedProperties() {
			if(cached.isEmpty()) return getProperties();
			return cached;
		}
		
		/**
		 * Save the given properties file to the system
		 */
		public void saveProperties() {
			try {
				OutputStream out = new FileOutputStream(file);
				cached.store(out, null);
			} catch(Exception e) { e.printStackTrace(); }
		}
	}
}
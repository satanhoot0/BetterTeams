package com.booksaw.betterTeams.extension;

import com.booksaw.betterTeams.Main;
import lombok.Data;
import lombok.Getter;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class ExtensionManager {
	private final Main plugin;
	@Getter
	private final File extensionsDir;
	private final List<RegisteredExtension> registered = new ArrayList<>();

	public ExtensionManager(Main plugin, File extensionsDir) {
		this.plugin = plugin;
		this.extensionsDir = extensionsDir;
	}

	public void registerExtensions() {
		registered.clear();
		if (!extensionsDir.exists()) {
			extensionsDir.mkdirs();
			return;
		}
		File[] jars = extensionsDir.listFiles((file, name) -> name.endsWith(".jar"));
		if (jars == null) return;

		for (File jar : jars) {
			try {
				RegisteredExtension reg = scanJar(jar);
				registered.add(reg);
				plugin.getLogger().info("Registered extension: " + reg.getInfo().getName());
			} catch (Exception e) {
				plugin.getLogger().log(Level.WARNING, "Skipping " + jar.getName() + " – " + e.getMessage(), e);
			}
		}
		plugin.getLogger().info("Registered " + registered.size() + " extensions.");
	}

	private RegisteredExtension scanJar(File jar) throws IOException {
		try (URLClassLoader temp = new URLClassLoader(
				new URL[]{jar.toURI().toURL()}, getClass().getClassLoader())) {

			try (InputStream in = temp.getResourceAsStream("extension.yml")) {
				if (in == null) throw new IOException("extension.yml missing");
				ExtensionInfo info = ExtensionInfo.fromYaml(in);
				return new RegisteredExtension(info, jar);
			}
		}
	}

	public void loadExtensions() {
		// TODO
	}

	 void loadSingleExtension(RegisteredExtension reg) throws Exception {

		File jarFile = reg.getJar();
		ExtensionInfo info = reg.getInfo();
	 	URLClassLoader loader = null;


		 loader = new URLClassLoader(
				 new URL[]{jarFile.toURI().toURL()},
				 getClass().getClassLoader());

		 Class<?> clazz = Class.forName(info.getMainClass(), true, loader);

		 if (!BetterTeamsExtension.class.isAssignableFrom(clazz)) {
			 throw new ClassCastException(info.getMainClass() + " does not extend BetterTeamsExtension");
		 }

		 BetterTeamsExtension instance = (BetterTeamsExtension) clazz.getDeclaredConstructor().newInstance();

		 File dataFolder = new File(extensionsDir, info.getName());

		 instance.init(info, dataFolder, plugin);
		 instance.onLoad();
		 instance.onEnable();

		 // TODO: add to a active extensions list for track

		 try {
			 loader.close();
		 } catch (IOException ignored) {}

		 plugin.getLogger().info("Enabled extension: " + info.getName() + " v" + info.getVersion());


	 }

	public void reloadAllExtensions() {
		// TODO
	}

	public void unloadAllExtensions() {

	}

	public void unloadExtension(String name) {

	}

	private boolean missingPluginDeps(ExtensionInfo info) {
		for (String pluginName : info.getPluginDepend()) {
			if (plugin.getServer().getPluginManager().getPlugin(pluginName) == null) {
				plugin.getLogger().warning("Extension " + info.getName() +
						" requires plugin '" + pluginName + "' – not found, skipping.");
				return true;
			}
		}
		return false;
	}

	private boolean missingExtensionDeps(ExtensionInfo info) {
		// TODO
		return false;
	}



	private List<RegisteredExtension> sort(List<RegisteredExtension> input) {
		// TODO
		return null;
	}

	public List<BetterTeamsExtension> getActiveExtensions() {
		// TODO
		return null;
	}

	List<RegisteredExtension> getRegistered() {
		return new ArrayList<>(registered);
	}

	@Data
	static final class RegisteredExtension {
		private final ExtensionInfo info;
		private final File jar;
	}
}

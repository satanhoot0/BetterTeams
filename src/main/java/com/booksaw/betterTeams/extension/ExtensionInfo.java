package com.booksaw.betterTeams.extension;

import lombok.Data;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Data
public class ExtensionInfo {
	private final String name;
	private final String mainClass;
	private final String version;
	private final String author;
	private final String description;
	private final String website;

	private final List<String> pluginDepend;
	private final List<String> pluginSoftDepend;

	private final List<String> extensionDepend;
	private final List<String> extensionSoftDepend;


	@Override
	public String toString(){
		String descPart = description.isEmpty() ? "" : " - " + description.trim();
		String authorPart = author.isEmpty() ? "" : " (author: " + author.trim() + ")";
		return name.trim() + " v" + version.trim() + descPart + authorPart;
	}

	public static ExtensionInfo fromYaml(InputStream yamlStream) throws IOException {
		if (yamlStream == null) {
			throw new IOException("extension.yml not found");
		}

		YamlConfiguration yml = YamlConfiguration.loadConfiguration(new InputStreamReader(yamlStream, StandardCharsets.UTF_8));
		String name = yml.getString("name", "").trim();
		String main = yml.getString("main", "").trim();
		String ver = yml.getString("version", "1.0").trim();
		String author = yml.getString("author", "").trim();
		String desc = yml.getString("description", "").trim();
		String site = yml.getString("website", "").trim();

		List<String> pHard = yml.getStringList("depend");
		List<String> pSoft  = yml.getStringList("softdepend");
		List<String> eHard  = yml.getStringList("ext-depend");
		List<String> eSoft  = yml.getStringList("ext-softdepend");

		if (main.isEmpty()) {
			throw new IllegalArgumentException("No 'main' specified in extension.yml");
		}
		if (name.isEmpty()) {
			throw new IllegalArgumentException("No 'name' specified in extension.yml");
		}
		return new ExtensionInfo(name, main, ver, author, desc, site, pHard, pSoft, eHard, eSoft);
	}

}


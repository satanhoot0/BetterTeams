package com.booksaw.betterTeams.extension;

import com.booksaw.betterTeams.Main;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ExtensionManagerTest {

	@TempDir
	Path tempDir;

	private Main mockPlugin;
	private File extensionsDir;
	private ExtensionManager extensionManager;
	private Logger mockLogger;

	@BeforeEach
	void setUp() {
		mockPlugin = mock(Main.class);
		mockLogger = mock(Logger.class);
		when(mockPlugin.getLogger()).thenReturn(mockLogger);

		extensionsDir = tempDir.resolve("extensions").toFile();
		extensionsDir.mkdirs();

		extensionManager = new ExtensionManager(mockPlugin, extensionsDir);
	}

	@AfterEach
	void tearDown() {
	}

	@Test
	void testScanJar() throws IOException {
		// Create fake extension.yml content
		String ymlContent = """
                name: TestExtension
                main: com.example.TestMainClass
                version: 1.0.0
                author: TestAuthor
                description: A test extension
                website: https://example.com
                
                depend:
                  - Vault
                  - WorldEdit
                
                softdepend:
                  - Essentials
                  - LuckPerms
                
                ext-depend:
                  - CoreExt
                
                ext-softdepend:
                  - OptionalExt
                """;

		// Create fake JAR file with the yml inside
		File fakeJar = createFakeJar("test-extension.jar", ymlContent);

		// Scan the JAR
		extensionManager.registerExtensions();
		ExtensionManager.RegisteredExtension registered = extensionManager.getRegistered().get(0);

		// Assertions
		ExtensionInfo info = registered.getInfo();
		assertEquals("TestExtension", info.getName());
		assertEquals("com.example.TestMainClass", info.getMainClass());
		assertEquals("1.0.0", info.getVersion());
		assertEquals("TestAuthor", info.getAuthor());
		assertEquals("A test extension", info.getDescription());
		assertEquals("https://example.com", info.getWebsite());

		// Lists
		assertEquals(Arrays.asList("Vault", "WorldEdit"), info.getPluginDepend());
		assertEquals(Arrays.asList("Essentials", "LuckPerms"), info.getPluginSoftDepend());
		assertEquals(List.of("CoreExt"), info.getExtensionDepend());
		assertEquals(List.of("OptionalExt"), info.getExtensionSoftDepend());

		assertEquals(fakeJar, registered.getJar());

//		dumpExtensionInfo(info);
	}

	@Test
	void testScanJar_Miss() throws IOException {
		createFakeJar("invalid.jar", null); // No yml

		extensionManager.registerExtensions();

		assertEquals(0, extensionManager.getRegistered().size());
		verify(mockLogger).log(eq(Level.WARNING),
				argThat(msg -> msg.contains("Skipping invalid.jar") && msg.contains("extension.yml missing")),
				any(IOException.class)
		);
	}
	@Test
	void testScanJar_NoMain() throws IOException {
		String ymlContent = """
                name: InvalidExt
                version: 1.0
                """; // No main
		createFakeJar("invalid-main.jar", ymlContent);
		extensionManager.registerExtensions();
		assertEquals(0, extensionManager.getRegistered().size());
		verify(mockLogger).log(eq(Level.WARNING),
				argThat(msg -> msg.contains("Skipping invalid-main.jar") && msg.contains("No 'main' specified in extension.yml")),
				any(IllegalArgumentException.class)
		);
	}

	// Register Test
	@Test
	void testRegisterExtensions() throws IOException {
		// JAR #1: A valid extension that should be registered successfully
		String validYml1 = """
                name: FirstValidExt
                main: com.example.FirstMain
                version: 1.0
                """;
		createFakeJar("first-valid.jar", validYml1);

		// JAR #2: why 2 valid? idk
		String validYml2 = """
                name: SecondValidExt
                main: com.example.SecondMain
                version: 2.0
                """;
		createFakeJar("second-valid.jar", validYml2);

		// JAR #3: An invalid JAR because it's missing the extension.yml file. This should be skipped
		createFakeJar("no-yml.jar", null);

		// JAR #4: An invalid JAR because its extension.yml is missing the required 'main' field. This should also be skipped
		String incompleteYml = """
                name: NoMainClassExt
                version: 3.0
                """;
		createFakeJar("no-main.jar", incompleteYml);

		extensionManager.registerExtensions();

		List<ExtensionManager.RegisteredExtension> registered = extensionManager.getRegistered();
		assertEquals(2, registered.size(), "Should have registered exactly 2 extensions.");

		// Verify name
		Set<String> registeredNames = registered.stream()
				.map(reg -> reg.getInfo().getName())
				.collect(Collectors.toSet());
		assertTrue(registeredNames.contains("FirstValidExt"), "FirstValidExt should be registered.");
		assertTrue(registeredNames.contains("SecondValidExt"), "SecondValidExt should be registered.");
		assertFalse(registeredNames.contains("NoMainClassExt"), "NoMainClassExt should NOT be registered (skipped).");

		// Verify log message
		verify(mockLogger).info(eq("Registered extension: FirstValidExt"));
		verify(mockLogger).info(eq("Registered extension: SecondValidExt"));

		// Verify total count log
		verify(mockLogger).info(eq("Registered 2 extensions."));

		// warning logs for skipped jar
		verify(mockLogger).log(eq(Level.WARNING),
				argThat(msg -> msg.contains("Skipping no-yml.jar") && msg.contains("extension.yml missing")),
				any(Exception.class));
		verify(mockLogger).log(eq(Level.WARNING),
				argThat(msg -> msg.contains("Skipping no-main.jar") && msg.contains("No 'main' specified")),
				any(Exception.class));


		// debug: un comment this
//		registered.forEach(reg -> {
//			System.out.println("--- Dumping Extension from JAR: " + reg.getJar().getName() + " ---");
//			dumpExtensionInfo(reg.getInfo());
//		});
	}

	@Test
	void testRegisterExtensions_NoJars() {
		// No jar in dir
		extensionManager.registerExtensions();
		verify(mockLogger).info(eq("Registered 0 extensions."));
	}

	private File createFakeJar(String jarName, String ymlContent) throws IOException {
		File jarFile = extensionsDir.toPath().resolve(jarName).toFile();
		try (FileOutputStream fos = new FileOutputStream(jarFile);
			 JarOutputStream jos = new JarOutputStream(fos)) {

			if (ymlContent != null) {
				// Add extension.yml entry
				JarEntry entry = new JarEntry("extension.yml");
				jos.putNextEntry(entry);
				jos.write(ymlContent.getBytes(StandardCharsets.UTF_8));
			}
			jos.closeEntry();

		}
		return jarFile;
	}

	private void dumpExtensionInfo(ExtensionInfo info) {
		System.out.println("=== FULL EXTENSION INFO DUMP ===");
		System.out.println("toString(): " + info.toString());
		System.out.println("Name: " + info.getName());
		System.out.println("Main Class: " + info.getMainClass());
		System.out.println("Version: " + info.getVersion());
		System.out.println("Author: " + info.getAuthor());
		System.out.println("Description: " + info.getDescription());
		System.out.println("Website: " + info.getWebsite());
		System.out.println("Plugin Depend: " + info.getPluginDepend());
		System.out.println("Plugin Softdepend: " + info.getPluginSoftDepend());
		System.out.println("Extension Depend: " + info.getExtensionDepend());
		System.out.println("Extension Softdepend: " + info.getExtensionSoftDepend());
		System.out.println("================================");
	}
}
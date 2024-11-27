package dev.emortal.paperpolar;

import dev.emortal.paperpolar.commands.PolarCommand;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@SuppressWarnings("UnstableApiUsage")
public class Main extends JavaPlugin {

    @Override
    public void onEnable() {
        // Paper commands
        LifecycleEventManager<Plugin> manager = this.getLifecycleManager();
        manager.registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            Commands commands = event.registrar();
            PolarCommand.register(commands);
        });


        getServer().getPluginManager().registerEvents(new PolarListener(), this);

        Path pluginFolder = Path.of(getDataFolder().getAbsolutePath());
        Path configFilePath = pluginFolder.resolve("config.yml");
        File configFile = configFilePath.toFile();
        Path worldsFolder = pluginFolder.resolve("worlds");

        worldsFolder.toFile().mkdirs();

        saveDefaultConfig();

        try (var files = Files.list(worldsFolder)) {
            files.forEach(path -> {
                if (!path.getFileName().toString().endsWith(".polar")) {
                    return;
                }

                String worldName = path.getFileName().toString().split(".polar")[0];

                initWorld(worldName, getConfig());

                byte[] bytes;
                try {
                    getConfig().save(configFile);
                    bytes = Files.readAllBytes(path);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                Config config = Config.readFromConfig(getConfig(), worldName);
                if (config == null) {
                    getLogger().warning("Polar world '" + worldName + "' has an invalid config, skipping.");
                    return;
                }

                if (!config.loadOnStartup()) return;

                getLogger().info("Loading polar world: " + worldName);

                PolarWorld polarWorld = PolarReader.read(bytes);
                Polar.loadWorld(polarWorld, worldName);
            });
        } catch (IOException e) {
            getLogger().warning("Failed to load world on startup");
            getLogger().warning(e.toString());
        }
    }

    public static void initWorld(String worldName, FileConfiguration config) {
        if (config.isSet("worlds." + worldName)) return;

        Config.writeToConfig(config, worldName, Config.DEFAULT);
    }

    @Override
    public void onDisable() {
        // TODO: save worlds that are configured to autosave
    }


    public static Main getPlugin() {
        return Main.getPlugin(Main.class);
    }


}
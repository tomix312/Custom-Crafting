package pl.customcrafting;

import org.bukkit.plugin.java.JavaPlugin;
import pl.customcrafting.commands.CraftingCommand;
import pl.customcrafting.listeners.GUIListener;
import pl.customcrafting.managers.RecipeManager;

public class CustomCrafting extends JavaPlugin {

    private static CustomCrafting instance;
    private RecipeManager recipeManager;

    @Override
    public void onEnable() {
        instance = this;
        
        // Zapisz domyślną konfigurację
        saveDefaultConfig();
        
        // Inicjalizacja managera przepisów
        recipeManager = new RecipeManager(this);
        recipeManager.loadRecipes();
        
        // Rejestracja komend
        CraftingCommand craftingCommand = new CraftingCommand(this);
        getCommand("customcrafting").setExecutor(craftingCommand);
        getCommand("customcrafting").setTabCompleter(craftingCommand);
        
        // Rejestracja listenerów
        getServer().getPluginManager().registerEvents(new GUIListener(this), this);
        
        getLogger().info("CustomCrafting został włączony!");
        getLogger().info("Załadowano " + recipeManager.getRecipeCount() + " przepisów.");
    }

    @Override
    public void onDisable() {
        if (recipeManager != null) {
            recipeManager.saveRecipes();
        }
        getLogger().info("CustomCrafting został wyłączony!");
    }

    public static CustomCrafting getInstance() {
        return instance;
    }

    public RecipeManager getRecipeManager() {
        return recipeManager;
    }

    public String getMessage(String key) {
        String prefix = getConfig().getString("messages.prefix", "&6[CustomCrafting] &r");
        String message = getConfig().getString("messages." + key, "&cBrak wiadomości: " + key);
        return colorize(prefix + message);
    }

    public String colorize(String message) {
        return message.replace("&", "§");
    }

    public void reload() {
        reloadConfig();
        recipeManager.unloadAllRecipes();
        recipeManager.loadRecipes();
    }
}

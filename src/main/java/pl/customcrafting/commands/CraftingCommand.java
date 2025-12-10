package pl.customcrafting.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import pl.customcrafting.CustomCrafting;
import pl.customcrafting.gui.CraftingGUI;
import pl.customcrafting.listeners.GUIListener;
import pl.customcrafting.models.CustomRecipe;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CraftingCommand implements CommandExecutor, TabCompleter {

    private final CustomCrafting plugin;

    public CraftingCommand(CustomCrafting plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("customcrafting.use")) {
            sender.sendMessage(plugin.getMessage("no-permission"));
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "create", "add", "new" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(plugin.colorize("&cTa komenda jest tylko dla graczy!"));
                    return true;
                }
                if (!player.hasPermission("customcrafting.admin")) {
                    player.sendMessage(plugin.getMessage("no-permission"));
                    return true;
                }
                if (args.length < 2) {
                    player.sendMessage(plugin.colorize("&cUżycie: /cc create <nazwa>"));
                    return true;
                }
                String recipeName = args[1].toLowerCase();
                if (plugin.getRecipeManager().recipeExists(recipeName)) {
                    player.sendMessage(plugin.getMessage("recipe-exists"));
                    return true;
                }
                getCraftingGUI().openCreator(player, recipeName);
            }
            
            case "delete", "remove", "del" -> {
                if (!sender.hasPermission("customcrafting.admin")) {
                    sender.sendMessage(plugin.getMessage("no-permission"));
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(plugin.colorize("&cUżycie: /cc delete <nazwa>"));
                    return true;
                }
                String recipeName = args[1].toLowerCase();
                if (!plugin.getRecipeManager().recipeExists(recipeName)) {
                    sender.sendMessage(plugin.getMessage("recipe-not-found").replace("%recipe%", recipeName));
                    return true;
                }
                plugin.getRecipeManager().removeRecipe(recipeName);
                sender.sendMessage(plugin.getMessage("recipe-deleted").replace("%recipe%", recipeName));
            }
            
            case "list" -> {
                if (sender instanceof Player player) {
                    getCraftingGUI().openList(player);
                } else {
                    sender.sendMessage(plugin.colorize("&6Lista przepisów:"));
                    for (CustomRecipe recipe : plugin.getRecipeManager().getAllRecipes()) {
                        sender.sendMessage(plugin.colorize("&7- &f" + recipe.getName() + 
                                " &7(" + recipe.getResult().getType().name() + ")"));
                    }
                }
            }
            
            case "reload" -> {
                if (!sender.hasPermission("customcrafting.reload")) {
                    sender.sendMessage(plugin.getMessage("no-permission"));
                    return true;
                }
                plugin.reload();
                sender.sendMessage(plugin.getMessage("reloaded"));
            }
            
            case "gui" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(plugin.colorize("&cTa komenda jest tylko dla graczy!"));
                    return true;
                }
                getCraftingGUI().openList(player);
            }
            
            default -> sendHelp(sender);
        }

        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(plugin.colorize("&6&l=== CustomCrafting ==="));
        sender.sendMessage(plugin.colorize("&e/cc create <nazwa> &7- Utwórz nowy przepis"));
        sender.sendMessage(plugin.colorize("&e/cc delete <nazwa> &7- Usuń przepis"));
        sender.sendMessage(plugin.colorize("&e/cc list &7- Lista przepisów"));
        sender.sendMessage(plugin.colorize("&e/cc gui &7- Otwórz GUI z przepisami"));
        sender.sendMessage(plugin.colorize("&e/cc reload &7- Przeładuj plugin"));
    }

    private CraftingGUI getCraftingGUI() {
        for (org.bukkit.event.Listener listener : org.bukkit.plugin.java.JavaPlugin.getPlugin(CustomCrafting.class)
                .getServer().getPluginManager().getPlugins()[0].getClass().getClasses()) {
            // Metoda uproszczona
        }
        // Pobierz GUI z listenera
        return ((GUIListener) plugin.getServer().getPluginManager()
                .getRegisteredListeners(plugin).stream()
                .filter(r -> r.getListener() instanceof GUIListener)
                .findFirst()
                .orElseThrow()
                .getListener()).getCraftingGUI();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            completions.addAll(Arrays.asList("create", "delete", "list", "reload", "gui"));
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("delete")) {
                completions.addAll(plugin.getRecipeManager().getRecipeNames());
            }
        }
        
        return completions.stream()
                .filter(s -> s.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
                .collect(Collectors.toList());
    }
}

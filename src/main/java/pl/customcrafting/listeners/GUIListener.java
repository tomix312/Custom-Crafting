package pl.customcrafting.listeners;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import pl.customcrafting.CustomCrafting;
import pl.customcrafting.gui.CraftingGUI;
import pl.customcrafting.models.CustomRecipe;

public class GUIListener implements Listener {

    private final CustomCrafting plugin;
    private final CraftingGUI craftingGUI;

    public GUIListener(CustomCrafting plugin) {
        this.plugin = plugin;
        this.craftingGUI = new CraftingGUI(plugin);
    }

    public CraftingGUI getCraftingGUI() {
        return craftingGUI;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        
        String title = event.getView().getTitle();
        
        if (title.equals(CraftingGUI.GUI_TITLE)) {
            handleCreatorClick(event, player);
        } else if (title.equals(CraftingGUI.LIST_TITLE)) {
            handleListClick(event, player);
        }
    }

    private void handleCreatorClick(InventoryClickEvent event, Player player) {
        int slot = event.getRawSlot();
        Inventory gui = event.getInventory();
        
        // Pozwól na interakcję ze slotami craftingu i wyniku
        if (craftingGUI.isCreatorSlot(slot)) {
            // Usuń placeholder jeśli jest
            ItemStack current = gui.getItem(slot);
            if (current != null && (current.getType() == Material.LIGHT_GRAY_STAINED_GLASS_PANE 
                    || current.getType() == Material.LIME_STAINED_GLASS_PANE)) {
                gui.setItem(slot, null);
            }
            return; // Pozwól na normalne działanie
        }
        
        // Pozwól na interakcję z ekwipunkiem gracza
        if (slot >= 54) {
            return;
        }
        
        event.setCancelled(true);
        
        // Obsługa przycisków
        if (slot == CraftingGUI.CONFIRM_SLOT) {
            handleConfirm(player, gui);
        } else if (slot == CraftingGUI.CANCEL_SLOT) {
            returnItems(player, gui);
            craftingGUI.removePendingRecipe(player.getUniqueId());
            player.closeInventory();
            player.sendMessage(plugin.colorize("&cAnulowano tworzenie przepisu."));
        } else if (slot == CraftingGUI.TOGGLE_TYPE_SLOT) {
            craftingGUI.toggleShapedMode(player.getUniqueId());
            craftingGUI.updateTypeButton(gui, craftingGUI.isShapedMode(player.getUniqueId()));
        }
    }

    private void handleConfirm(Player player, Inventory gui) {
        String recipeName = craftingGUI.getPendingRecipe(player.getUniqueId());
        if (recipeName == null) {
            player.sendMessage(plugin.getMessage("invalid-recipe"));
            return;
        }
        
        // Pobierz składniki
        ItemStack[] ingredients = new ItemStack[9];
        for (int i = 0; i < 9; i++) {
            ItemStack item = gui.getItem(CraftingGUI.CRAFTING_SLOTS[i]);
            if (item != null && item.getType() != Material.LIGHT_GRAY_STAINED_GLASS_PANE 
                    && item.getType() != Material.AIR) {
                ingredients[i] = item.clone();
            }
        }
        
        // Pobierz wynik
        ItemStack result = gui.getItem(CraftingGUI.RESULT_SLOT);
        if (result == null || result.getType() == Material.LIME_STAINED_GLASS_PANE 
                || result.getType() == Material.AIR) {
            player.sendMessage(plugin.getMessage("invalid-recipe"));
            return;
        }
        
        // Sprawdź czy są jakieś składniki
        boolean hasIngredients = false;
        for (ItemStack item : ingredients) {
            if (item != null && item.getType() != Material.AIR) {
                hasIngredients = true;
                break;
            }
        }
        
        if (!hasIngredients) {
            player.sendMessage(plugin.getMessage("invalid-recipe"));
            return;
        }
        
        // Utwórz przepis
        boolean shaped = craftingGUI.isShapedMode(player.getUniqueId());
        CustomRecipe recipe = new CustomRecipe(recipeName, result.clone(), ingredients, shaped);
        
        plugin.getRecipeManager().addRecipe(recipe);
        plugin.getRecipeManager().saveRecipe(recipe);
        
        craftingGUI.removePendingRecipe(player.getUniqueId());
        player.closeInventory();
        player.sendMessage(plugin.getMessage("recipe-created").replace("%recipe%", recipeName));
    }

    private void handleListClick(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;
        
        // Shift + prawy przycisk = usuń
        if (event.isShiftClick() && event.isRightClick()) {
            if (!player.hasPermission("customcrafting.admin")) {
                player.sendMessage(plugin.getMessage("no-permission"));
                return;
            }
            
            // Znajdź nazwę przepisu z lore
            if (clicked.hasItemMeta() && clicked.getItemMeta().hasLore()) {
                for (String line : clicked.getItemMeta().getLore()) {
                    if (line.contains("Nazwa:")) {
                        String recipeName = line.replace("§6Nazwa: §f", "").trim();
                        plugin.getRecipeManager().removeRecipe(recipeName);
                        player.sendMessage(plugin.getMessage("recipe-deleted").replace("%recipe%", recipeName));
                        craftingGUI.openList(player); // Odśwież listę
                        return;
                    }
                }
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        
        if (event.getView().getTitle().equals(CraftingGUI.GUI_TITLE)) {
            // Zwróć przedmioty graczowi
            returnItems(player, event.getInventory());
            craftingGUI.removePendingRecipe(player.getUniqueId());
        }
    }

    private void returnItems(Player player, Inventory gui) {
        // Zwróć przedmioty z slotów craftingu
        for (int slot : CraftingGUI.CRAFTING_SLOTS) {
            ItemStack item = gui.getItem(slot);
            if (item != null && item.getType() != Material.LIGHT_GRAY_STAINED_GLASS_PANE 
                    && item.getType() != Material.AIR) {
                player.getInventory().addItem(item);
            }
        }
        
        // Zwróć przedmiot z wyniku
        ItemStack result = gui.getItem(CraftingGUI.RESULT_SLOT);
        if (result != null && result.getType() != Material.LIME_STAINED_GLASS_PANE 
                && result.getType() != Material.AIR) {
            player.getInventory().addItem(result);
        }
    }
}

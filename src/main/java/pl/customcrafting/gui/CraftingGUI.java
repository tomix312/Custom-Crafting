package pl.customcrafting.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import pl.customcrafting.CustomCrafting;

import java.util.*;

public class CraftingGUI {

    private final CustomCrafting plugin;
    
    public static final String GUI_TITLE = "§6Custom Crafting Creator";
    public static final String LIST_TITLE = "§6Lista Przepisów";
    
    // Sloty dla craftingu 3x3 (w GUI 9x6)
    public static final int[] CRAFTING_SLOTS = {10, 11, 12, 19, 20, 21, 28, 29, 30};
    public static final int RESULT_SLOT = 24;
    public static final int CONFIRM_SLOT = 40;
    public static final int CANCEL_SLOT = 44;
    public static final int TOGGLE_TYPE_SLOT = 36;
    
    private final Map<UUID, String> pendingRecipes = new HashMap<>();
    private final Map<UUID, Boolean> shapedMode = new HashMap<>();

    public CraftingGUI(CustomCrafting plugin) {
        this.plugin = plugin;
    }

    public void openCreator(Player player, String recipeName) {
        Inventory gui = Bukkit.createInventory(null, 54, GUI_TITLE);
        
        // Wypełnij tło
        ItemStack background = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) {
            gui.setItem(i, background);
        }
        
        // Obszar craftingu (3x3)
        ItemStack craftingSlot = createItem(Material.LIGHT_GRAY_STAINED_GLASS_PANE, "§7Połóż przedmiot");
        for (int slot : CRAFTING_SLOTS) {
            gui.setItem(slot, craftingSlot);
        }
        
        // Slot na wynik
        ItemStack resultSlot = createItem(Material.LIME_STAINED_GLASS_PANE, "§aWynik craftingu", "§7Połóż tutaj przedmiot wynikowy");
        gui.setItem(RESULT_SLOT, resultSlot);
        
        // Strzałka
        ItemStack arrow = createItem(Material.ARROW, "§e➜ Wynik");
        gui.setItem(23, arrow);
        
        // Przycisk potwierdzenia
        ItemStack confirm = createItem(Material.LIME_WOOL, "§a§lZATWIERDŹ", "§7Kliknij aby utworzyć przepis");
        gui.setItem(CONFIRM_SLOT, confirm);
        
        // Przycisk anulowania
        ItemStack cancel = createItem(Material.RED_WOOL, "§c§lANULUJ", "§7Kliknij aby anulować");
        gui.setItem(CANCEL_SLOT, cancel);
        
        // Przycisk typu (shaped/shapeless)
        shapedMode.put(player.getUniqueId(), true);
        updateTypeButton(gui, true);
        
        // Info
        ItemStack info = createItem(Material.BOOK, "§e§lInstrukcja",
                "§7Połóż przedmioty w siatce 3x3",
                "§7Połóż wynik w zielonym slocie",
                "§7Kliknij ZATWIERDŹ aby zapisać",
                "",
                "§6Nazwa przepisu: §f" + recipeName);
        gui.setItem(4, info);
        
        pendingRecipes.put(player.getUniqueId(), recipeName);
        player.openInventory(gui);
    }

    public void openList(Player player) {
        Collection<pl.customcrafting.models.CustomRecipe> recipes = plugin.getRecipeManager().getAllRecipes();
        int size = Math.max(9, (int) Math.ceil(recipes.size() / 9.0) * 9);
        size = Math.min(54, size);
        
        Inventory gui = Bukkit.createInventory(null, size, LIST_TITLE);
        
        int slot = 0;
        for (pl.customcrafting.models.CustomRecipe recipe : recipes) {
            if (slot >= size) break;
            
            ItemStack display = recipe.getResult().clone();
            ItemMeta meta = display.getItemMeta();
            
            List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
            lore.add("");
            lore.add("§6Nazwa: §f" + recipe.getName());
            lore.add("§6Typ: §f" + (recipe.isShaped() ? "Shaped" : "Shapeless"));
            lore.add("");
            lore.add("§c§lShift + PPM aby usunąć");
            
            meta.setLore(lore);
            if (!meta.hasDisplayName()) {
                meta.setDisplayName("§f" + recipe.getResult().getType().name());
            }
            display.setItemMeta(meta);
            
            gui.setItem(slot++, display);
        }
        
        player.openInventory(gui);
    }

    public void updateTypeButton(Inventory gui, boolean shaped) {
        ItemStack typeButton;
        if (shaped) {
            typeButton = createItem(Material.CRAFTING_TABLE, "§e§lTyp: SHAPED",
                    "§7Pozycja przedmiotów ma znaczenie",
                    "",
                    "§aKliknij aby zmienić na SHAPELESS");
        } else {
            typeButton = createItem(Material.CHEST, "§e§lTyp: SHAPELESS",
                    "§7Pozycja przedmiotów nie ma znaczenia",
                    "",
                    "§aKliknij aby zmienić na SHAPED");
        }
        gui.setItem(TOGGLE_TYPE_SLOT, typeButton);
    }

    private ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        if (lore.length > 0) {
            meta.setLore(Arrays.asList(lore));
        }
        item.setItemMeta(meta);
        return item;
    }

    public String getPendingRecipe(UUID uuid) {
        return pendingRecipes.get(uuid);
    }

    public void removePendingRecipe(UUID uuid) {
        pendingRecipes.remove(uuid);
        shapedMode.remove(uuid);
    }

    public boolean isShapedMode(UUID uuid) {
        return shapedMode.getOrDefault(uuid, true);
    }

    public void toggleShapedMode(UUID uuid) {
        shapedMode.put(uuid, !shapedMode.getOrDefault(uuid, true));
    }

    public boolean isCreatorSlot(int slot) {
        for (int s : CRAFTING_SLOTS) {
            if (s == slot) return true;
        }
        return slot == RESULT_SLOT;
    }
}

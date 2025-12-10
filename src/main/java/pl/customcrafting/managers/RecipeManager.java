package pl.customcrafting.managers;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import pl.customcrafting.CustomCrafting;
import pl.customcrafting.models.CustomRecipe;

import java.util.*;

public class RecipeManager {

    private final CustomCrafting plugin;
    private final Map<String, CustomRecipe> recipes;
    private final Set<NamespacedKey> registeredKeys;

    public RecipeManager(CustomCrafting plugin) {
        this.plugin = plugin;
        this.recipes = new HashMap<>();
        this.registeredKeys = new HashSet<>();
    }

    public void loadRecipes() {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("recipes");
        if (section == null) return;

        for (String recipeName : section.getKeys(false)) {
            try {
                loadRecipe(recipeName, section.getConfigurationSection(recipeName));
            } catch (Exception e) {
                plugin.getLogger().warning("Nie można załadować przepisu: " + recipeName);
                e.printStackTrace();
            }
        }
    }

    private void loadRecipe(String name, ConfigurationSection section) {
        if (section == null) return;

        // Wczytaj wynik
        Material resultMaterial = Material.valueOf(section.getString("result.material", "STONE"));
        int resultAmount = section.getInt("result.amount", 1);
        String resultName = section.getString("result.name", null);
        List<String> resultLore = section.getStringList("result.lore");

        ItemStack result = new ItemStack(resultMaterial, resultAmount);
        if (resultName != null || !resultLore.isEmpty()) {
            ItemMeta meta = result.getItemMeta();
            if (resultName != null) {
                meta.setDisplayName(plugin.colorize(resultName));
            }
            if (!resultLore.isEmpty()) {
                List<String> coloredLore = new ArrayList<>();
                for (String line : resultLore) {
                    coloredLore.add(plugin.colorize(line));
                }
                meta.setLore(coloredLore);
            }
            result.setItemMeta(meta);
        }

        // Wczytaj składniki
        ItemStack[] ingredients = new ItemStack[9];
        ConfigurationSection ingredientsSection = section.getConfigurationSection("ingredients");
        if (ingredientsSection != null) {
            for (int i = 0; i < 9; i++) {
                ConfigurationSection slotSection = ingredientsSection.getConfigurationSection(String.valueOf(i));
                if (slotSection != null) {
                    Material mat = Material.valueOf(slotSection.getString("material", "AIR"));
                    int amount = slotSection.getInt("amount", 1);
                    ingredients[i] = new ItemStack(mat, amount);
                    
                    String itemName = slotSection.getString("name", null);
                    List<String> itemLore = slotSection.getStringList("lore");
                    if (itemName != null || !itemLore.isEmpty()) {
                        ItemMeta meta = ingredients[i].getItemMeta();
                        if (itemName != null) {
                            meta.setDisplayName(plugin.colorize(itemName));
                        }
                        if (!itemLore.isEmpty()) {
                            List<String> coloredLore = new ArrayList<>();
                            for (String line : itemLore) {
                                coloredLore.add(plugin.colorize(line));
                            }
                            meta.setLore(coloredLore);
                        }
                        ingredients[i].setItemMeta(meta);
                    }
                }
            }
        }

        boolean shaped = section.getBoolean("shaped", true);

        CustomRecipe recipe = new CustomRecipe(name, result, ingredients, shaped);
        addRecipe(recipe);
    }

    public void addRecipe(CustomRecipe recipe) {
        recipes.put(recipe.getName().toLowerCase(), recipe);
        registerBukkitRecipe(recipe);
    }

    private void registerBukkitRecipe(CustomRecipe recipe) {
        NamespacedKey key = new NamespacedKey(plugin, recipe.getName().toLowerCase());
        
        // Usuń stary przepis jeśli istnieje
        Bukkit.removeRecipe(key);
        
        if (recipe.isShaped()) {
            registerShapedRecipe(recipe, key);
        } else {
            registerShapelessRecipe(recipe, key);
        }
        
        registeredKeys.add(key);
    }

    private void registerShapedRecipe(CustomRecipe recipe, NamespacedKey key) {
        ShapedRecipe shapedRecipe = new ShapedRecipe(key, recipe.getResult());
        
        // Określ kształt
        StringBuilder[] rows = new StringBuilder[3];
        for (int i = 0; i < 3; i++) {
            rows[i] = new StringBuilder();
        }
        
        Map<Material, Character> materialChars = new HashMap<>();
        char currentChar = 'A';
        
        ItemStack[] ingredients = recipe.getIngredients();
        for (int i = 0; i < 9; i++) {
            int row = i / 3;
            ItemStack item = ingredients[i];
            
            if (item == null || item.getType() == Material.AIR) {
                rows[row].append(' ');
            } else {
                Material mat = item.getType();
                if (!materialChars.containsKey(mat)) {
                    materialChars.put(mat, currentChar++);
                }
                rows[row].append(materialChars.get(mat));
            }
        }
        
        shapedRecipe.shape(rows[0].toString(), rows[1].toString(), rows[2].toString());
        
        for (Map.Entry<Material, Character> entry : materialChars.entrySet()) {
            shapedRecipe.setIngredient(entry.getValue(), entry.getKey());
        }
        
        try {
            Bukkit.addRecipe(shapedRecipe);
        } catch (Exception e) {
            plugin.getLogger().warning("Nie można zarejestrować przepisu shaped: " + recipe.getName());
        }
    }

    private void registerShapelessRecipe(CustomRecipe recipe, NamespacedKey key) {
        ShapelessRecipe shapelessRecipe = new ShapelessRecipe(key, recipe.getResult());
        
        for (ItemStack item : recipe.getIngredients()) {
            if (item != null && item.getType() != Material.AIR) {
                shapelessRecipe.addIngredient(item.getType());
            }
        }
        
        try {
            Bukkit.addRecipe(shapelessRecipe);
        } catch (Exception e) {
            plugin.getLogger().warning("Nie można zarejestrować przepisu shapeless: " + recipe.getName());
        }
    }

    public void removeRecipe(String name) {
        String key = name.toLowerCase();
        recipes.remove(key);
        
        NamespacedKey nsKey = new NamespacedKey(plugin, key);
        Bukkit.removeRecipe(nsKey);
        registeredKeys.remove(nsKey);
        
        // Usuń z configu
        plugin.getConfig().set("recipes." + key, null);
        plugin.saveConfig();
    }

    public void saveRecipes() {
        for (CustomRecipe recipe : recipes.values()) {
            saveRecipe(recipe);
        }
        plugin.saveConfig();
    }

    public void saveRecipe(CustomRecipe recipe) {
        String path = "recipes." + recipe.getName().toLowerCase();
        
        // Zapisz wynik
        plugin.getConfig().set(path + ".result.material", recipe.getResult().getType().name());
        plugin.getConfig().set(path + ".result.amount", recipe.getResult().getAmount());
        
        ItemMeta resultMeta = recipe.getResult().getItemMeta();
        if (resultMeta != null) {
            if (resultMeta.hasDisplayName()) {
                plugin.getConfig().set(path + ".result.name", resultMeta.getDisplayName());
            }
            if (resultMeta.hasLore()) {
                plugin.getConfig().set(path + ".result.lore", resultMeta.getLore());
            }
        }
        
        // Zapisz składniki
        ItemStack[] ingredients = recipe.getIngredients();
        for (int i = 0; i < 9; i++) {
            String slotPath = path + ".ingredients." + i;
            if (ingredients[i] != null && ingredients[i].getType() != Material.AIR) {
                plugin.getConfig().set(slotPath + ".material", ingredients[i].getType().name());
                plugin.getConfig().set(slotPath + ".amount", ingredients[i].getAmount());
                
                ItemMeta meta = ingredients[i].getItemMeta();
                if (meta != null) {
                    if (meta.hasDisplayName()) {
                        plugin.getConfig().set(slotPath + ".name", meta.getDisplayName());
                    }
                    if (meta.hasLore()) {
                        plugin.getConfig().set(slotPath + ".lore", meta.getLore());
                    }
                }
            }
        }
        
        plugin.getConfig().set(path + ".shaped", recipe.isShaped());
        plugin.saveConfig();
    }

    public void unloadAllRecipes() {
        for (NamespacedKey key : registeredKeys) {
            Bukkit.removeRecipe(key);
        }
        registeredKeys.clear();
        recipes.clear();
    }

    public CustomRecipe getRecipe(String name) {
        return recipes.get(name.toLowerCase());
    }

    public Collection<CustomRecipe> getAllRecipes() {
        return recipes.values();
    }

    public Set<String> getRecipeNames() {
        return recipes.keySet();
    }

    public int getRecipeCount() {
        return recipes.size();
    }

    public boolean recipeExists(String name) {
        return recipes.containsKey(name.toLowerCase());
    }
}

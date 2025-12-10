package pl.customcrafting.models;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import java.util.Map;

public class CustomRecipe {

    private final String name;
    private final ItemStack result;
    private final ItemStack[] ingredients; // 9 slotów dla shaped
    private final boolean shaped;

    public CustomRecipe(String name, ItemStack result, ItemStack[] ingredients, boolean shaped) {
        this.name = name;
        this.result = result;
        this.ingredients = ingredients;
        this.shaped = shaped;
    }

    public String getName() {
        return name;
    }

    public ItemStack getResult() {
        return result;
    }

    public ItemStack[] getIngredients() {
        return ingredients;
    }

    public boolean isShaped() {
        return shaped;
    }

    public boolean hasValidIngredients() {
        for (ItemStack item : ingredients) {
            if (item != null && item.getType() != Material.AIR) {
                return true;
            }
        }
        return false;
    }
}

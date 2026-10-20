package fi.zoniboy.enchantreveal;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.view.EnchantmentView;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/** Calls the native enchanting calculation on supported Paper versions. */
final class VanillaPreview {
    record Entry(Enchantment enchantment, int level) {}
    record Offer(int requiredLevel, List<Entry> enchantments) {}

    private final Method getHandle;
    private final Method asNmsCopy;
    private final Method getList;
    private final Method holderToBukkit;
    private final Method worldHandle;
    private final Method registryAccess;
    private final Field costs;
    private final Field containerId;
    private final Method stateId;
    private final Method instanceEnchantment;
    private final Method instanceLevel;

    VanillaPreview() throws ReflectiveOperationException {
        Class<?> menu = Class.forName("net.minecraft.world.inventory.EnchantmentMenu");
        Class<?> instance = Class.forName("net.minecraft.world.item.enchantment.EnchantmentInstance");
        getHandle = Class.forName("org.bukkit.craftbukkit.inventory.CraftInventoryView").getMethod("getHandle");
        asNmsCopy = Class.forName("org.bukkit.craftbukkit.inventory.CraftItemStack")
                .getMethod("asNMSCopy", ItemStack.class);
        getList = menu.getDeclaredMethod("getEnchantmentList", Class.forName("net.minecraft.core.RegistryAccess"),
                Class.forName("net.minecraft.world.item.ItemStack"), int.class, int.class);
        getList.setAccessible(true);
        holderToBukkit = Class.forName("org.bukkit.craftbukkit.enchantments.CraftEnchantment")
                .getMethod("minecraftHolderToBukkit", Class.forName("net.minecraft.core.Holder"));
        worldHandle = Class.forName("org.bukkit.craftbukkit.CraftWorld").getMethod("getHandle");
        registryAccess = worldHandle.getReturnType().getMethod("registryAccess");
        costs = menu.getField("costs");
        containerId = menu.getField("containerId");
        stateId = menu.getMethod("getStateId");
        instanceEnchantment = instance.getMethod("enchantment");
        instanceLevel = instance.getMethod("level");
    }

    boolean matches(EnchantmentView view, int expectedContainer, int expectedState) throws ReflectiveOperationException {
        Object menu = getHandle.invoke(view);
        return containerId.getInt(menu) == expectedContainer && (int) stateId.invoke(menu) == expectedState;
    }

    List<Offer> read(Player player, EnchantmentView view, ItemStack item) throws ReflectiveOperationException {
        Object menu = getHandle.invoke(view);
        Object registries = registryAccess.invoke(worldHandle.invoke(player.getWorld()));
        Object nmsItem = asNmsCopy.invoke(null, item);
        int[] levels = ((int[]) costs.get(menu)).clone();
        List<Offer> result = new ArrayList<>(3);
        for (int slot = 0; slot < 3; slot++) {
            List<Entry> enchants = new ArrayList<>();
            if (levels[slot] > 0) {
                // Vanilla resets its temporary RNG from the table seed and slot.
                // This does not advance or overwrite the player's enchantment seed.
                List<?> instances = (List<?>) getList.invoke(menu, registries, nmsItem, slot, levels[slot]);
                for (Object entry : instances) {
                    Enchantment enchantment = (Enchantment) holderToBukkit.invoke(null, instanceEnchantment.invoke(entry));
                    if (enchantment == null) throw new IllegalStateException("Unregistered enchantment");
                    enchants.add(new Entry(enchantment, (int) instanceLevel.invoke(entry)));
                }
            }
            result.add(new Offer(levels[slot], List.copyOf(enchants)));
        }
        return List.copyOf(result);
    }
}

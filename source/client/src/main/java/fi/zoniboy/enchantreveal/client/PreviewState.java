package fi.zoniboy.enchantreveal.client;

import fi.zoniboy.enchantreveal.wire.Protocol;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Per-screen cache; stale responses cannot replace a newer request. */
public final class PreviewState {
    private static long nextNonce;
    private Snapshot requested;
    private Protocol.Preview received;
    private long nonce;
    private int waitingTicks;
    private int attempts;

    private record Snapshot(int containerId, int stateId, ItemStack item, int seed,
                            int[] costs, int[] clues, int[] levels) {
        Snapshot(EnchantmentMenu menu) {
            this(menu.containerId, menu.getStateId(), menu.getSlot(0).getItem().copy(), menu.getEnchantmentSeed(),
                    menu.costs.clone(), menu.enchantClue.clone(), menu.levelClue.clone());
        }
        boolean matches(EnchantmentMenu menu) {
            return containerId == menu.containerId && stateId == menu.getStateId()
                    && seed == menu.getEnchantmentSeed()
                    && ItemStack.isSameItemSameComponents(item, menu.getSlot(0).getItem())
                    && Arrays.equals(costs, menu.costs) && Arrays.equals(clues, menu.enchantClue)
                    && Arrays.equals(levels, menu.levelClue);
        }
    }

    public void tick(EnchantmentMenu menu) {
        if (menu.getSlot(0).getItem().isEmpty() || Arrays.stream(menu.costs).allMatch(cost -> cost <= 0)) {
            requested = null;
            received = null;
            return;
        }
        if (!ClientPlayNetworking.canSend(Packets.Request.TYPE)) return;
        if (requested == null || !requested.matches(menu)) {
            requested = new Snapshot(menu);
            received = null;
            attempts = 0;
            send(menu);
        } else if (received == null && attempts < 3 && ++waitingTicks >= 20) {
            send(menu);
        }
    }

    private void send(EnchantmentMenu menu) {
        nonce = ++nextNonce;
        waitingTicks = 0;
        attempts++;
        ClientPlayNetworking.send(new Packets.Request(new Protocol.Request(menu.containerId, menu.getStateId(), nonce)));
    }

    public void receive(EnchantmentMenu menu, Protocol.Preview preview) {
        Protocol.Request request = preview.request();
        if (requested == null || request.nonce() != nonce || request.containerId() != menu.containerId
                || request.stateId() != menu.getStateId() || !requested.matches(menu)) return;
        for (int i = 0; i < 3; i++) if (preview.offers().get(i).requiredLevel() != menu.costs[i]) return;
        received = preview;
    }

    public List<Component> tooltip(EnchantmentMenu menu, int slot, List<Component> original) {
        if (received == null || requested == null || !requested.matches(menu) || original.isEmpty()) return original;
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) return original;
        var registry = client.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        List<Component> result = new ArrayList<>();
        for (Protocol.Enchant enchant : received.offers().get(slot).enchantments()) {
            var holder = registry.get(Identifier.parse(enchant.key()));
            // Preserve vanilla if a custom server enchantment is unknown to the client.
            if (holder.isEmpty()) return original;
            result.add(Enchantment.getFullname(holder.get(), enchant.level()).copy().withStyle(ChatFormatting.WHITE));
        }
        if (result.isEmpty()) return original;
        result.addAll(original.subList(1, original.size()));
        return result;
    }
}

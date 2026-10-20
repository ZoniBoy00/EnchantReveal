package fi.zoniboy.enchantreveal;

import fi.zoniboy.enchantreveal.wire.Protocol;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.view.EnchantmentView;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

/** Provides native enchant previews to the companion Fabric client. */
public final class EnchantReveal extends JavaPlugin {
    private static final Set<String> SUPPORTED = Set.of("26.1.2", "26.2", "26.3");
    private final Map<UUID, Protocol.Request> pending = new HashMap<>();
    private VanillaPreview adapter;

    @Override
    public void onEnable() {
        if (!SUPPORTED.contains(Bukkit.getMinecraftVersion())) {
            getLogger().severe("Supported Paper versions: 26.1.2, 26.2 and 26.3.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        try {
            adapter = new VanillaPreview();
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            fail(e);
            return;
        }
        getServer().getMessenger().registerOutgoingPluginChannel(this, Protocol.PREVIEW);
        getServer().getMessenger().registerIncomingPluginChannel(this, Protocol.REQUEST, (channel, player, bytes) -> {
            try {
                Protocol.Request request = Protocol.decodeRequest(bytes);
                // Coalesce requests: at most one native calculation per player per tick.
                if (pending.put(player.getUniqueId(), request) == null) {
                    Bukkit.getScheduler().runTask(this, () -> respond(player));
                }
            } catch (IOException ignored) {
                // Malformed requests cannot alter the table or consume resources.
            }
        });
    }

    private void respond(Player player) {
        Protocol.Request request = pending.remove(player.getUniqueId());
        if (request == null || !player.isOnline()
                || !player.getListeningPluginChannels().contains(Protocol.PREVIEW)
                || !(player.getOpenInventory() instanceof EnchantmentView view)) return;
        ItemStack item = view.getTopInventory().getItem(0);
        if (item == null || item.getType().isAir()) return;
        try {
            if (!adapter.matches(view, request.containerId(), request.stateId())) return;
            List<Protocol.Offer> offers = adapter.read(player, view, item).stream()
                    .map(offer -> new Protocol.Offer(offer.requiredLevel(), offer.enchantments().stream()
                            .map(entry -> new Protocol.Enchant(entry.enchantment().getKey().toString(), entry.level()))
                            .toList())).toList();
            player.sendPluginMessage(this, Protocol.PREVIEW, Protocol.encodePreview(new Protocol.Preview(request, offers)));
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            fail(e);
        }
    }

    private void fail(Throwable error) {
        getLogger().log(Level.SEVERE, "Enchant preview unavailable. Vanilla enchanting is unaffected.", error);
        Bukkit.getPluginManager().disablePlugin(this);
    }

    @Override
    public void onDisable() {
        pending.clear();
        getServer().getMessenger().unregisterIncomingPluginChannel(this);
        getServer().getMessenger().unregisterOutgoingPluginChannel(this);
    }
}

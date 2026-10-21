package fi.zoniboy.enchantreveal;

import fi.zoniboy.enchantreveal.wire.Protocol;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.view.EnchantmentView;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

/** Provides native enchant previews to the companion Fabric client. */
public final class EnchantReveal extends JavaPlugin implements Listener {
    private static final Set<String> SUPPORTED = Set.of("26.1.2", "26.2", "26.3");
    /** Minimum ticks between two native calculations for the same player. */
    private static final int MIN_INTERVAL_TICKS = 4;
    private static final long WARNING_INTERVAL_NANOS = 60_000_000_000L;
    private final Map<UUID, Protocol.Request> pending = new HashMap<>();
    private final Map<UUID, Integer> lastResponseTick = new HashMap<>();
    private long lastWarningNanos;
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
                    // Spaces out repeated requests so a client cannot force a calculation every tick.
                    int last = lastResponseTick.getOrDefault(player.getUniqueId(), Integer.MIN_VALUE / 2);
                    long delay = Math.max(1, last + MIN_INTERVAL_TICKS - Bukkit.getCurrentTick());
                    Bukkit.getScheduler().runTaskLater(this, () -> respond(player), delay);
                }
            } catch (IOException ignored) {
                // Malformed requests cannot alter the table or consume resources.
            }
        });
        getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        pending.remove(event.getPlayer().getUniqueId());
        lastResponseTick.remove(event.getPlayer().getUniqueId());
    }

    private void respond(Player player) {
        Protocol.Request request = pending.remove(player.getUniqueId());
        lastResponseTick.put(player.getUniqueId(), Bukkit.getCurrentTick());
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
        } catch (InvocationTargetException e) {
            // The native call itself failed for this item or state; one bad request must not disable the plugin.
            skip(e.getCause() == null ? e : e.getCause());
        } catch (ReflectiveOperationException | LinkageError e) {
            // The server internals no longer match what this build expects.
            fail(e);
        } catch (RuntimeException e) {
            skip(e);
        }
    }

    private void skip(Throwable error) {
        long now = System.nanoTime();
        if (lastWarningNanos == 0 || now - lastWarningNanos > WARNING_INTERVAL_NANOS) {
            lastWarningNanos = now;
            getLogger().log(Level.WARNING, "Enchant preview skipped for one request; vanilla tooltip is used.", error);
        }
    }

    private void fail(Throwable error) {
        getLogger().log(Level.SEVERE, "Enchant preview unavailable. Vanilla enchanting is unaffected.", error);
        Bukkit.getPluginManager().disablePlugin(this);
    }

    @Override
    public void onDisable() {
        pending.clear();
        lastResponseTick.clear();
        getServer().getMessenger().unregisterIncomingPluginChannel(this);
        getServer().getMessenger().unregisterOutgoingPluginChannel(this);
    }
}

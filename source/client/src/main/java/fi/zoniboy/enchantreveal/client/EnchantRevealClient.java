package fi.zoniboy.enchantreveal.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.client.Minecraft;
import java.lang.ref.WeakReference;

public final class EnchantRevealClient implements ClientModInitializer {
    private static WeakReference<PreviewReceiver> active = new WeakReference<>(null);

    public static void track(PreviewReceiver receiver) {
        if (active.get() != receiver) active = new WeakReference<>(receiver);
    }

    @Override
    public void onInitializeClient() {
        PayloadTypeRegistry.serverboundPlay().register(Packets.Request.TYPE, Packets.Request.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(Packets.Preview.TYPE, Packets.Preview.CODEC);
        ClientPlayNetworking.registerGlobalReceiver(Packets.Preview.TYPE, (packet, context) -> {
            if (packet.value() == null) return;
            Minecraft client = Minecraft.getInstance();
            client.execute(() -> {
                PreviewReceiver receiver = active.get();
                if (receiver != null) receiver.enchantreveal$receive(packet.value());
            });
        });
    }
}

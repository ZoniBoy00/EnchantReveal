package fi.zoniboy.enchantreveal.client.mixin;

import fi.zoniboy.enchantreveal.client.PreviewReceiver;
import fi.zoniboy.enchantreveal.client.EnchantRevealClient;
import fi.zoniboy.enchantreveal.client.PreviewState;
import fi.zoniboy.enchantreveal.wire.Protocol;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.EnchantmentScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.EnchantmentMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(value = EnchantmentScreen.class, remap = false)
public abstract class EnchantmentScreenMixin extends AbstractContainerScreen<EnchantmentMenu> implements PreviewReceiver {
    @Unique private final PreviewState enchantreveal$state = new PreviewState();

    protected EnchantmentScreenMixin(EnchantmentMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Inject(method = "containerTick", at = @At("TAIL"), remap = false)
    private void enchantreveal$tick(CallbackInfo ci) {
        EnchantRevealClient.track(this);
        enchantreveal$state.tick(menu);
    }

    @ModifyArgs(method = "extractRenderState", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/client/gui/GuiGraphicsExtractor;setComponentTooltipForNextFrame(Lnet/minecraft/client/gui/Font;Ljava/util/List;II)V"), remap = false)
    private void enchantreveal$tooltip(Args args, GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        for (int slot = 0; slot < 3; slot++) {
            if (isHovering(60, 14 + 19 * slot, 108, 17, mouseX, mouseY)) {
                args.set(1, enchantreveal$state.tooltip(menu, slot, args.get(1)));
                return;
            }
        }
    }

    @Override
    public void enchantreveal$receive(Protocol.Preview preview) {
        enchantreveal$state.receive(menu, preview);
    }
}

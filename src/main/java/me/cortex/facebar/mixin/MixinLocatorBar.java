package me.cortex.facebar.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.contextualbar.LocatorBar;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.WaypointStyle;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.waypoints.*;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.gui.components.PlayerFaceExtractor;

@Mixin(LocatorBar.class)
public class MixinLocatorBar {
    @Shadow @Final private Minecraft minecraft;
    @Unique private TrackedWaypoint waypoint;

    @Inject(method = "lambda$extractRenderState$1", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIIII)V",
            ordinal = 0))
    private void injectRender(Entity entity, Level level, PartialTickSupplier partialTickSupplier, GuiGraphicsExtractor guiGraphics, int i, TrackedWaypoint trackedWaypoint, CallbackInfo ci) {
        this.waypoint = trackedWaypoint;
    }

    @WrapOperation(method = "lambda$extractRenderState$1", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIIII)V",
            ordinal = 0))
    private void redirectBlit(GuiGraphicsExtractor instance, RenderPipeline renderPipeline, Identifier resourceLocation, int x, int y, int width, int height, int color,Operation<Void> original) {

        TrackedWaypoint waypoint = this.waypoint;
        Entity camera = Minecraft.getInstance().getCameraEntity();

        boolean shouldDrawHead = waypoint != null && camera != null;

        if (shouldDrawHead) {
            PlayerInfo playerInfo = waypoint.id().left()
                    .map(uuid -> {
                        var conn = Minecraft.getInstance().getConnection();
                        return conn != null ? conn.getPlayerInfo(uuid) : null;
                    })
                    .orElse(null);
            if (playerInfo != null) {
                Waypoint.Icon icon = waypoint.icon();
                WaypointStyle style = this.minecraft.gui.hud.getWaypointStyles().get(icon.style);
                float distance = Mth.sqrt((float) waypoint.distanceSquared(camera));
                float near = style.nearDistance();
                float far = style.farDistance();
                float progress = (far - near) > 0.001F
                        ? 1.0F - Mth.clamp((distance - near) / (far - near), 0.0F, 1.0F)
                        : 1.0F;
                int baseSize = 9;
                int minSize = 5;
                int scaledSize = Mth.lerpInt(progress, minSize, baseSize);
                int offset = (baseSize - scaledSize) / 2;
                int drawX = x + offset;
                int drawY = y + offset;

                PlayerFaceExtractor.extractRenderState(instance, playerInfo.getSkin(), drawX, drawY, scaledSize);
                return;
            }
        }
        original.call(instance, renderPipeline, resourceLocation, x, y, width, height, color);
    }
}

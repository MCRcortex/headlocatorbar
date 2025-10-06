package me.cortex.facebar.mixin;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.contextualbar.LocatorBarRenderer;
import net.minecraft.client.resources.WaypointStyle;
import net.minecraft.resources.ResourceLocation;
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
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocatorBarRenderer.class)
public class MixinLocatorBarRenderer {
    @Shadow @Final private Minecraft minecraft;
    @Unique private ResourceLocation blitOverride;
    @Unique private TrackedWaypoint waypoint;

    @Inject(method = "method_70870", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/ResourceLocation;IIIII)V",shift = At.Shift.BEFORE))
    private void injectRender(Entity entity, Level level, PartialTickSupplier partialTickSupplier, GuiGraphics guiGraphics, int i, TrackedWaypoint trackedWaypoint, CallbackInfo ci) {
        this.waypoint = trackedWaypoint;
        var connection = Minecraft.getInstance().getConnection();
        if (connection != null && trackedWaypoint.id().left().isPresent()) {
            var info = connection.getPlayerInfo(trackedWaypoint.id().left().get());
            if (info != null) {
                this.blitOverride = Minecraft.getInstance().getSkinManager().createLookup(info.getProfile(), false).get().body().id();
            }
        }
    }

    @Redirect(method = "method_70870", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/ResourceLocation;IIIII)V"))
    private void redirectBlit(GuiGraphics instance, RenderPipeline renderPipeline, ResourceLocation resourceLocation, int i, int j, int k, int l, int m) {
        if (this.blitOverride == null) {
            instance.blitSprite(renderPipeline, resourceLocation, i, j, k, l, m);
        } else {
            float distance = Mth.sqrt((float)this.waypoint.distanceSquared(this.minecraft.getCameraEntity()));
            Waypoint.Icon icon = this.waypoint.icon();
            WaypointStyle style = this.minecraft.getWaypointStyles().get(icon.style);
            float progress = 1-Mth.clamp((distance-style.nearDistance())/(style.farDistance()-style.nearDistance()),0,1);
            k = Mth.lerpInt(progress, 4*100+100, (k)*100);
            l = Mth.lerpInt(progress, 4*100+100, (l)*100);
            instance.pose().pushMatrix();
            instance.pose().scale(0.01f);
            //TODO: maybe use `PlayerFaceRenderer.draw`
            instance.blit(this.blitOverride, (i*100+450)-(k/2), (j*100+450)-(l/2), (i*100+450)-(k/2)+k, (j*100+450)-(l/2)+l, 1f/8,2f/8,1f/8,2f/8);
            instance.blit(this.blitOverride, (i*100+450)-(k/2), (j*100+450)-(l/2), (i*100+450)-(k/2)+k, (j*100+450)-(l/2)+l, 5f/8,6f/8,1f/8,2f/8);
            instance.pose().popMatrix();
        }
        this.blitOverride = null;
    }
}

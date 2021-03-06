/*
 * This file is part of Dynamic Surroundings, licensed under the MIT License (MIT).
 *
 * Copyright (c) OreCruncher
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.blockartistry.DynSurround.client.weather;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraftforge.client.IRenderHandler;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.blockartistry.DynSurround.DSurround;
import org.blockartistry.DynSurround.client.handlers.AuroraEffectHandler;
import org.blockartistry.DynSurround.registry.DimensionRegistry;
import org.blockartistry.DynSurround.registry.RegistryManager;
import org.blockartistry.DynSurround.registry.RegistryManager.RegistryType;
import org.blockartistry.lib.Color;
import org.lwjgl.opengl.GL11;

@Mod.EventBusSubscriber(value = Side.CLIENT, modid = DSurround.MOD_ID)
public final class AuroraRenderer extends IRenderHandler {

	protected final IRenderHandler handler;

	public AuroraRenderer(@Nullable final IRenderHandler handler) {
		this.handler = handler;
	}

	@Override
	public void render(final float partialTicks, @Nonnull final WorldClient world, @Nonnull final Minecraft mc) {
		if (this.handler == null) {
			// There isn't another handler. This means we have to call back
			// into the Minecraft code to render the normal sky. This is tricky
			// because we need to unhook ourselves and rehook after rendering.
			world.provider.setSkyRenderer(null);
			try {
				mc.renderGlobal.renderSky(partialTicks, 2);
			} catch (final Throwable t) {
				;
			}
			world.provider.setSkyRenderer(this);
		} else {
			// Call the existing handler
			this.handler.render(partialTicks, world, mc);
		}

		// Render our aurora if it is present
		final Aurora aurora = AuroraEffectHandler.getCurrentAurora();
		if (aurora != null)
			renderAurora(partialTicks, aurora);
	}

	protected final DimensionRegistry dimensions = RegistryManager.<DimensionRegistry>get(RegistryType.DIMENSION);

	private int getZOffset() {
		return (Minecraft.getMinecraft().gameSettings.renderDistanceChunks + 1) * 16;
	}

	private double getScaledHeight() {
		return 4D + 16D * ((Minecraft.getMinecraft().gameSettings.renderDistanceChunks - 6D) / 24D);
	}

	protected void renderAurora(final float partialTick, @Nonnull final Aurora aurora) {

		final float alpha = aurora.getAlphaf();
		if (alpha <= 0.0F)
			return;

		final Minecraft mc = Minecraft.getMinecraft();
		final Tessellator tess = Tessellator.getInstance();
		final BufferBuilder renderer = tess.getBuffer();

		final double tranY = this.dimensions.getSeaLevel(mc.world)
				- ((mc.player.lastTickPosY + (mc.player.posY - mc.player.lastTickPosY) * partialTick));

		final double tranX = mc.player.posX
				- (mc.player.lastTickPosX + (mc.player.posX - mc.player.lastTickPosX) * partialTick);

		final double tranZ = (mc.player.posZ - getZOffset())
				- (mc.player.lastTickPosZ + (mc.player.posZ - mc.player.lastTickPosZ) * partialTick);

		aurora.translate(partialTick);

		final Color base = aurora.getBaseColor();
		final Color fade = aurora.getFadeColor();
		final double zero = 0.0D;

		GlStateManager.pushMatrix();
		GlStateManager.pushAttrib();

		GlStateManager.translate(tranX, tranY, tranZ);
		GlStateManager.scale(0.5D, getScaledHeight(), 0.5D);
		GlStateManager.disableTexture2D();
		GlStateManager.disableFog();
		GlStateManager.shadeModel(GL11.GL_SMOOTH);
		GlStateManager.disableLighting();
		GlStateManager.enableBlend();
		GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE,
				GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
		GlStateManager.disableAlpha();
		GlStateManager.disableCull();
		GlStateManager.depthMask(false);

		final Node[] array = aurora.getNodeList();
		for (int i = 0; i < array.length - 1; i++) {

			final Node node = array[i];

			final double posY = node.getModdedY();
			final double posX = node.tetX;
			final double posZ = node.tetZ;
			final double tetX = node.tetX2;
			final double tetZ = node.tetZ2;

			final double posX2;
			final double posZ2;
			final double tetX2;
			final double tetZ2;
			final double posY2;

			if (i < array.length - 2) {
				final Node nodePlus = array[i + 1];
				posX2 = nodePlus.tetX;
				posZ2 = nodePlus.tetZ;
				tetX2 = nodePlus.tetX2;
				tetZ2 = nodePlus.tetZ2;
				posY2 = nodePlus.getModdedY();
			} else {
				posX2 = tetX2 = node.posX;
				posZ2 = tetZ2 = node.getModdedZ();
				posY2 = 0.0D;
			}

			// Front
			renderer.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION_COLOR);
			renderer.pos(posX, zero, posZ).color(base.red, base.green, base.blue, alpha).endVertex();
			renderer.pos(posX, posY, posZ).color(fade.red, fade.green, fade.blue, 0).endVertex();
			renderer.pos(posX2, posY2, posZ2).color(fade.red, fade.green, fade.blue, 0).endVertex();
			renderer.pos(posX2, zero, posZ2).color(base.red, base.green, base.blue, alpha).endVertex();
			tess.draw();

			// Bottom
			renderer.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION_COLOR);
			renderer.pos(posX, zero, posZ).color(base.red, base.green, base.blue, alpha).endVertex();
			renderer.pos(posX2, zero, posZ2).color(base.red, base.green, base.blue, alpha).endVertex();
			renderer.pos(tetX2, zero, tetZ2).color(base.red, base.green, base.blue, alpha).endVertex();
			renderer.pos(tetX, zero, tetZ).color(base.red, base.green, base.blue, alpha).endVertex();
			tess.draw();

			// Back
			renderer.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION_COLOR);
			renderer.pos(tetX, zero, tetZ).color(base.red, base.green, base.blue, alpha).endVertex();
			renderer.pos(tetX, posY, tetZ).color(fade.red, fade.green, fade.blue, 0).endVertex();
			renderer.pos(tetX2, posY2, tetZ2).color(fade.red, fade.green, fade.blue, 0).endVertex();
			renderer.pos(tetX2, zero, tetZ2).color(base.red, base.green, base.blue, alpha).endVertex();
			tess.draw();
		}

		GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA,
				GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE,
				GlStateManager.DestFactor.ZERO);
		GlStateManager.shadeModel(GL11.GL_FLAT);
		GlStateManager.depthMask(true);
		GlStateManager.enableCull();
		GlStateManager.enableFog();
		GlStateManager.enableTexture2D();
		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

		GlStateManager.disableBlend();
		GlStateManager.popAttrib();
		GlStateManager.popMatrix();
	}

	private static boolean shouldHook(@Nonnull final World world) {
		final IRenderHandler handler = world.provider.getSkyRenderer();
		if (handler instanceof AuroraRenderer)
			return false;

		final DimensionRegistry registry = RegistryManager.<DimensionRegistry>get(RegistryType.DIMENSION);
		return registry.hasAuroras(world);
	}
	
	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void doRender(@Nonnull final RenderWorldLastEvent event) {
		// Make sure that the sky renderer is an aurora renderer
		if (shouldHook(Minecraft.getMinecraft().world)) {
			final AuroraRenderer hook = new AuroraRenderer(Minecraft.getMinecraft().world.provider.getSkyRenderer());
			Minecraft.getMinecraft().world.provider.setSkyRenderer(hook);
		}
	}

}

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

package org.blockartistry.DynSurround.proxy;

import javax.annotation.Nonnull;

import org.blockartistry.DynSurround.ModOptions;
import org.blockartistry.DynSurround.client.fx.particle.ParticleDripOverride;
import org.blockartistry.DynSurround.client.handlers.EffectManager;
import org.blockartistry.DynSurround.client.handlers.ExpressionStateHandler;
import org.blockartistry.DynSurround.client.hud.GuiHUDHandler;
import org.blockartistry.DynSurround.client.sound.MusicTickerReplacement;
import org.blockartistry.DynSurround.commands.CommandCalc;
import org.blockartistry.lib.Localization;

import net.minecraft.client.Minecraft;
import net.minecraft.util.EnumParticleTypes;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent.ClientConnectedToServerEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent.ClientDisconnectionFromServerEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class ProxyClient extends Proxy {
	
	@Override
	protected void registerLanguage() {
		Localization.initialize(Side.CLIENT);
	}

	@Override
	public boolean isRunningAsServer() {
		return false;
	}

	@Override
	public Side effectiveSide() {
		return FMLCommonHandler.instance().getEffectiveSide();
	}

	@Override
	public void preInit(@Nonnull final FMLPreInitializationEvent event) {
		super.preInit(event);
		ExpressionStateHandler.register();
	}

	@Override
	public void init(@Nonnull final FMLInitializationEvent event) {
		super.init(event);
		ParticleDripOverride.register();
		
		ClientCommandHandler.instance.registerCommand(new CommandCalc());
		
		if(ModOptions.disableWaterSuspendParticle)
			Minecraft.getMinecraft().effectRenderer.registerParticle(EnumParticleTypes.SUSPENDED.getParticleID(), null);
	}

	@Override
	public void postInit(@Nonnull final FMLPostInitializationEvent event) {
		MusicTickerReplacement.initialize();
	}

	@Override
	public void clientConnect(@Nonnull final ClientConnectedToServerEvent event) {
		Minecraft.getMinecraft().addScheduledTask(new Runnable() {
			public void run() {
				EffectManager.register();
				GuiHUDHandler.register();
				ProxyClient.this.connectionTime = System.currentTimeMillis();
			}
		});
	}

	@Override
	public void clientDisconnect(@Nonnull final ClientDisconnectionFromServerEvent event) {
		Minecraft.getMinecraft().addScheduledTask(new Runnable() {
			public void run() {
				EffectManager.unregister();
				GuiHUDHandler.unregister();
				ProxyClient.this.connectionTime = 0;
			}
		});
	}

}

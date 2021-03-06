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

package org.blockartistry.DynSurround.client.hud;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import org.blockartistry.DynSurround.DSurround;
import org.blockartistry.DynSurround.ModOptions;
import org.blockartistry.DynSurround.client.footsteps.implem.BlockMap;
import org.blockartistry.DynSurround.client.fx.BlockEffect;
import org.blockartistry.DynSurround.client.handlers.EnvironStateHandler.EnvironState;
import org.blockartistry.DynSurround.client.sound.SoundEffect;
import org.blockartistry.DynSurround.registry.BlockInfo;
import org.blockartistry.DynSurround.registry.BlockRegistry;
import org.blockartistry.DynSurround.registry.FootstepsRegistry;
import org.blockartistry.DynSurround.registry.RegistryManager;
import org.blockartistry.DynSurround.registry.RegistryManager.RegistryType;
import org.blockartistry.lib.MCHelper;
import org.blockartistry.lib.WorldUtils;
import org.blockartistry.lib.gui.TextPanel;
import org.blockartistry.lib.gui.Panel.Reference;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.oredict.OreDictionary;

@Mod.EventBusSubscriber(value = Side.CLIENT, modid = DSurround.MOD_ID)
public class BlockInfoHelperHUD extends GuiOverlay {

	private static final String TEXT_FOOTSTEP_ACOUSTICS = TextFormatting.DARK_PURPLE + "<Footstep Accoustics>";
	private static final String TEXT_BLOCK_EFFECTS = TextFormatting.DARK_PURPLE + "<Block Effects>";
	private static final String TEXT_ALWAYS_ON_EFFECTS = TextFormatting.DARK_PURPLE + "<Always On Effects>";
	private static final String TEXT_STEP_SOUNDS = TextFormatting.DARK_PURPLE + "<Step Sounds>";
	private static final String TEXT_BLOCK_SOUNDS = TextFormatting.DARK_PURPLE + "<Block Sounds>";
	private static final String TEXT_DICTIONARY_NAMES = TextFormatting.DARK_PURPLE + "<Dictionary Names>";

	private final BlockRegistry blocks = RegistryManager.get(RegistryType.BLOCK);
	private final FootstepsRegistry footsteps = RegistryManager.get(RegistryType.FOOTSTEPS);
	private final BlockInfo.BlockInfoMutable block = new BlockInfo.BlockInfoMutable();

	private static List<String> gatherOreNames(final ItemStack stack) {
		final List<String> result = new ArrayList<String>();
		if (stack != null && !stack.isEmpty())
			for (int i : OreDictionary.getOreIDs(stack))
				result.add(OreDictionary.getOreName(i));
		return result;
	}

	private static String getItemName(final ItemStack stack) {
		final Item item = stack.getItem();
		final String itemName = MCHelper.nameOf(item);

		if (itemName != null) {
			final StringBuilder builder = new StringBuilder();
			builder.append(itemName);
			if (stack.getHasSubtypes())
				builder.append(':').append(stack.getItemDamage());
			return builder.toString();
		}

		return null;
	}

	private List<String> gatherBlockText(final ItemStack stack, final List<String> text, final IBlockState state,
			final BlockPos pos) {

		if (stack != null && !stack.isEmpty()) {
			text.add(TextFormatting.RED + stack.getDisplayName());
			final String itemName = getItemName(stack);
			if (itemName != null) {
				text.add("ITEM: " + itemName);
				text.add(TextFormatting.DARK_AQUA + stack.getItem().getClass().getName());
			}
		}

		if (state != null) {
			this.block.set(state);
			text.add("BLOCK: " + this.block.toString());
			text.add(TextFormatting.DARK_AQUA + this.block.getBlock().getClass().getName());
			text.add("Material: " + MCHelper.getMaterialName(state.getMaterial()));

			final BlockMap bm = this.footsteps.getBlockMap();
			if (bm != null) {
				final List<String> data = new ArrayList<String>();
				bm.collectData(state, pos, data);
				if (data.size() > 0) {
					text.add(TEXT_FOOTSTEP_ACOUSTICS);
					for (final String s : data)
						text.add(TextFormatting.GOLD + s);
				}
			}

			BlockEffect[] effects = this.blocks.getEffects(state);
			if (effects.length > 0) {
				text.add(TEXT_BLOCK_EFFECTS);
				for (final BlockEffect e : effects) {
					text.add(TextFormatting.GOLD + e.getEffectType().getName());
				}
			}

			effects = this.blocks.getAlwaysOnEffects(state);
			if (effects.length > 0) {
				text.add(TEXT_ALWAYS_ON_EFFECTS);
				for (final BlockEffect e : effects) {
					text.add(TextFormatting.GOLD + e.getEffectType().getName());
				}
			}

			SoundEffect[] sounds = this.blocks.getAllStepSounds(state);
			if (sounds.length > 0) {
				text.add(TEXT_STEP_SOUNDS);
				text.add(TextFormatting.DARK_GREEN + "Chance: 1 in " + this.blocks.getStepSoundChance(state));
				for (final SoundEffect s : sounds)
					text.add(TextFormatting.GOLD + s.toString());
			}

			sounds = this.blocks.getAllSounds(state);
			if (sounds.length > 0) {
				text.add(TEXT_BLOCK_SOUNDS);
				text.add(TextFormatting.DARK_GREEN + "Chance: 1 in " + this.blocks.getSoundChance(state));
				for (final SoundEffect s : sounds)
					text.add(TextFormatting.GOLD + s.toString());
			}
		}

		final List<String> oreNames = gatherOreNames(stack);
		if (oreNames.size() > 0) {
			text.add(TEXT_DICTIONARY_NAMES);
			for (final String ore : oreNames)
				text.add(TextFormatting.GOLD + ore);
		}

		return text;
	}

	private static final ItemStack tool = new ItemStack(Items.NETHER_STAR, 64);

	private static boolean isHolding() {
		final EntityPlayer player = EnvironState.getPlayer();
		return ItemStack.areItemStacksEqual(tool, player.getHeldItem(EnumHand.MAIN_HAND));
	}

	private final TextPanel textPanel;

	public BlockInfoHelperHUD() {
		this.textPanel = new TextPanel();
	}

	@Override
	public void doTick(final int tickRef) {

		if (tickRef != 0 && tickRef % 5 == 0) {

			this.textPanel.resetText();

			// Only trigger if the player is in creative and is holding a stack
			// of nether stars
			if (EnvironState.getPlayer().isCreative() && isHolding()) {
				final RayTraceResult current = Minecraft.getMinecraft().objectMouseOver;
				final BlockPos targetBlock = (current == null || current.getBlockPos() == null) ? BlockPos.ORIGIN
						: current.getBlockPos();
				final IBlockState state = WorldUtils.getBlockState(EnvironState.getWorld(), targetBlock);

				final List<String> data = new ArrayList<String>();
				if (!WorldUtils.isAirBlock(state)) {
					final ItemStack stack = state != null ? state.getBlock().getPickBlock(state, current,
							EnvironState.getWorld(), targetBlock, EnvironState.getPlayer()) : null;

					gatherBlockText(stack, data, state, targetBlock);
					this.textPanel.setText(data);
				}
			}
		}
	}

	@Override
	public void doRender(@Nonnull final RenderGameOverlayEvent.Pre event) {
		if (event.getType() == ElementType.TEXT && this.textPanel.hasText()) {
			final int centerX = event.getResolution().getScaledWidth() / 2;
			final int centerY = 80;
			this.textPanel.render(centerX, centerY, Reference.TOP_CENTER);
		}
	}

	@SubscribeEvent
	public static void tooltipEvent(@Nonnull final ItemTooltipEvent event) {
		if (ModOptions.enableDebugLogging) {
			final ItemStack stack = event.getItemStack();
			if (stack != null) {
				final String itemName = getItemName(stack);
				event.getToolTip().add(TextFormatting.GOLD + itemName);
				event.getToolTip().add(TextFormatting.GOLD + stack.getItem().getClass().getName());
			}
		}
	}
}

/*
 * This file is part of Titanium
 * Copyright (C) 2018, Horizon Studio <contact@hrznstudio.com>, All rights reserved.
 *
 * This means no, you cannot steal this code. This is licensed for sole use by Horizon Studio and its subsidiaries, you MUST be granted specific written permission by Horizon Studio to use this code, thinking you have permission IS NOT PERMISSION!
 */
package com.hrznstudio.titanium.item;

import com.hrznstudio.titanium.energy.EnergyStorageItemStack;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

public class ItemEnergy extends ItemBase {
    private final int capacity;
    private final int input;
    private final int output;

    public ItemEnergy(String name, int capacity, int input, int output) {
        super(name);
        this.capacity = capacity;
        this.input = input;
        this.output = output;
        setMaxStackSize(1);
    }

    public ItemEnergy(String name, int capacity, int throughput) {
        this(name, capacity, throughput, throughput);
    }

    public int getCapacity() {
        return capacity;
    }

    public int getInput() {
        return input;
    }

    public int getOutput() {
        return output;
    }

    @Override
    public boolean hasDetails(@Nullable Key key) {
        return key == Key.SHIFT || super.hasDetails(key);
    }

    @Override
    public void addDetails(@Nullable Key key, ItemStack stack, List<String> tooltip, boolean advanced) {
        super.addDetails(key, stack, tooltip, advanced);
        if (key == Key.SHIFT) {
            getEnergyStorage(stack).ifPresent(storage -> tooltip.add(TextFormatting.YELLOW + "Energy: " + TextFormatting.RED + storage.getEnergyStored() + TextFormatting.YELLOW + "/" + TextFormatting.RED + storage.getMaxEnergyStored() + TextFormatting.RESET));
        }
    }

    @Override
    public boolean showDurabilityBar(ItemStack stack) {
        return getEnergyStorage(stack).isPresent();
    }

    @Override
    public double getDurabilityForDisplay(ItemStack stack) {
        return getEnergyStorage(stack).map(storage -> 1 - (double) storage.getEnergyStored() / (double) storage.getMaxEnergyStored()).orElse(0.0);
    }

    @Override
    public int getRGBDurabilityForDisplay(ItemStack stack) {
        return 0x00E93232;
    }

    public Optional<IEnergyStorage> getEnergyStorage(ItemStack stack) {
        return Optional.ofNullable(stack.getCapability(CapabilityEnergy.ENERGY, null));
    }

    @Nullable
    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable NBTTagCompound nbt) {
        return new CapabilityProvider(new EnergyStorageItemStack(stack, capacity, input, output));
    }

    public static class CapabilityProvider implements ICapabilityProvider {
        private EnergyStorageItemStack energy;

        public CapabilityProvider(EnergyStorageItemStack energy) {
            this.energy = energy;
        }

        @Override
        public boolean hasCapability(@Nonnull Capability<?> capability, @Nullable EnumFacing facing) {
            return capability == CapabilityEnergy.ENERGY;
        }

        @Nullable
        @Override
        public <T> T getCapability(@Nonnull Capability<T> capability, @Nullable EnumFacing facing) {
            return capability == CapabilityEnergy.ENERGY ? CapabilityEnergy.ENERGY.cast(energy) : null;
        }
    }
}
/*
 * This file is part of Titanium
 * Copyright (C) 2019, Horizon Studio <contact@hrznstudio.com>, All rights reserved.
 *
 * This means no, you cannot steal this code. This is licensed for sole use by Horizon Studio and its subsidiaries, you MUST be granted specific written permission by Horizon Studio to use this code, thinking you have permission IS NOT PERMISSION!
 */

package com.hrznstudio.titanium.nbthandler.data;

import com.hrznstudio.titanium.nbthandler.INBTHandler;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.INBTSerializable;

import javax.annotation.Nonnull;

public class NBTSerializableNBTHandler implements INBTHandler<INBTSerializable> {

    @Override
    public boolean isClassValid(Class<?> aClass) {
        return INBTSerializable.class.isAssignableFrom(aClass);
    }

    @Override
    public boolean storeToNBT(@Nonnull NBTTagCompound compound, @Nonnull String name, @Nonnull INBTSerializable object) {
        compound.setTag(name, object.serializeNBT());
        return false;
    }

    @Override
    public INBTSerializable readFromNBT(@Nonnull NBTTagCompound compound, @Nonnull String name, INBTSerializable currentValue) {
        if (compound.hasKey(name)) {
            currentValue.deserializeNBT(compound.getTag(name));
            return currentValue;
        }
        return null;
    }
}

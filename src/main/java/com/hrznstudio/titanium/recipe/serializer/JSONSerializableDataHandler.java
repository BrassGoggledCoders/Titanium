/*
 * This file is part of Titanium
 * Copyright (C) 2020, Horizon Studio <contact@hrznstudio.com>.
 *
 * This code is licensed under GNU Lesser General Public License v3.0, the full license text can be found in LICENSE.txt
 */

package com.hrznstudio.titanium.recipe.serializer;


import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.hrznstudio.titanium.Titanium;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.util.Pair;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.util.JSONUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Iterator;

public class JSONSerializableDataHandler {

    private static HashMap<Class, Pair<Writer, Reader>> FIELD_SERIALIZER = new HashMap<>();

    static {
        map(byte.class, JsonPrimitive::new, JsonElement::getAsByte);
        map(short.class, JsonPrimitive::new, JsonElement::getAsShort);
        map(int.class, JsonPrimitive::new, JsonElement::getAsInt);
        map(long.class, JsonPrimitive::new, JsonElement::getAsLong);
        map(float.class, JsonPrimitive::new, JsonElement::getAsFloat);
        map(double.class, JsonPrimitive::new, JsonElement::getAsDouble);
        map(boolean.class, JsonPrimitive::new, JsonElement::getAsBoolean);
        map(char.class, JsonPrimitive::new, JsonElement::getAsCharacter);
        map(Byte.class, JsonPrimitive::new, JsonElement::getAsByte);
        map(Short.class, JsonPrimitive::new, JsonElement::getAsShort);
        map(Integer.class, JsonPrimitive::new, JsonElement::getAsInt);
        map(Long.class, JsonPrimitive::new, JsonElement::getAsLong);
        map(Float.class, JsonPrimitive::new, JsonElement::getAsFloat);
        map(Double.class, JsonPrimitive::new, JsonElement::getAsDouble);
        map(Boolean.class, JsonPrimitive::new, JsonElement::getAsBoolean);
        map(Character.class, JsonPrimitive::new, JsonElement::getAsCharacter);
        map(String.class, JsonPrimitive::new, JsonElement::getAsString);


        map(ItemStack.class, JSONSerializableDataHandler::writeItemStack, element -> readItemStack(element.getAsJsonObject()));
        map(ResourceLocation.class, type -> new JsonPrimitive(type.toString()), element -> new ResourceLocation(element.getAsString()));
        map(Block.class, type -> new JsonPrimitive(type.getRegistryName().toString()), element -> ForgeRegistries.BLOCKS.getValue(new ResourceLocation(element.getAsString())));
        map(FluidStack.class, JSONSerializableDataHandler::writeFluidStack, JSONSerializableDataHandler::readFluidStack);

        map(Biome.class, JSONSerializableDataHandler::writeBiomeType, JSONSerializableDataHandler::readBiomeType);
        map(Biome[].class, (biomes) -> {
            JsonArray array = new JsonArray();
            for (Biome biome : biomes) {
                array.add(biome.getRegistryName().toString());
            }
            return array;
        }, element -> {
            Biome[] biomes = new Biome[element.getAsJsonArray().size()];
            int i = 0;
            for (Iterator<JsonElement> iterator = element.getAsJsonArray().iterator(); iterator.hasNext(); i++) {
                JsonElement jsonElement = iterator.next();
                biomes[i] = ForgeRegistries.BIOMES.getValue(new ResourceLocation(jsonElement.getAsString()));
            }
            return biomes;
        });
        map(Ingredient.class, Ingredient::serialize, Ingredient::deserialize);
        map(Ingredient[].class, (type) -> {
            JsonArray array = new JsonArray();
            for (Ingredient ingredient : type) {
                array.add(ingredient.serialize());
            }
            return array;
        }, (element) -> {
            Ingredient[] ingredients = new Ingredient[element.getAsJsonArray().size()];
            int i = 0;
            for (Iterator<JsonElement> iterator = element.getAsJsonArray().iterator(); iterator.hasNext(); i++) {
                JsonElement jsonElement = iterator.next();
                ingredients[i] = Ingredient.deserialize(jsonElement);
            }
            return ingredients;
        });
        map(Ingredient.IItemList.class, Ingredient.IItemList::serialize, element -> Ingredient.deserializeItemList(element.getAsJsonObject()));
        map(Ingredient.IItemList[].class, type -> {
            JsonArray array = new JsonArray();
            for (Ingredient.IItemList ingredient : type) {
                array.add(ingredient.serialize());
            }
            return array;
        }, element -> {
            Ingredient.IItemList[] ingredient = new Ingredient.IItemList[element.getAsJsonArray().size()];
            int i = 0;
            for (JsonElement jsonElement : element.getAsJsonArray()) {
                ingredient[i] = Ingredient.deserializeItemList(jsonElement.getAsJsonObject());
                ++i;
            }
            return ingredient;
        });
    }

    public static <T> void map(Class<T> type, Writer<T> writer, Reader<T> reader) {
        FIELD_SERIALIZER.put(type, Pair.of(writer, reader));
    }

    public static boolean acceptField(Field f, Class<?> type) {
        int mods = f.getModifiers();
        return !Modifier.isFinal(mods) && !Modifier.isStatic(mods) && !Modifier.isTransient(mods) && FIELD_SERIALIZER.containsKey(type);
    }

    public static <T> T read(Class<T> type, JsonElement element) {
        return (T) FIELD_SERIALIZER.get(type).getSecond().read(element);
    }

    public static JsonElement write(Class<?> type, Object value) {
        return FIELD_SERIALIZER.get(type).getFirst().write(value);
    }

    public static JsonObject writeItemStack(ItemStack stack) {
        JsonObject object = new JsonObject();
        object.addProperty("item", stack.getItem().getRegistryName().toString());
        object.addProperty("count", stack.getCount());
        if (stack.hasTag()) {
            object.addProperty("nbt", stack.getTag().toString());
        }
        return object;
    }

    public static JsonElement writeFluidStack(FluidStack fluidStack) {
        return new JsonPrimitive(fluidStack.writeToNBT(new CompoundNBT()).toString());
    }

    public static FluidStack readFluidStack(JsonElement object) {
        try {
            return FluidStack.loadFluidStackFromNBT(JsonToNBT.getTagFromJson(object.getAsString()));
        } catch (CommandSyntaxException e) {
            Titanium.LOGGER.catching(e);
        }
        return FluidStack.EMPTY;
    }

    public static ItemStack readItemStack(JsonObject object) {
        ItemStack stack = new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation(object.get("item").getAsString())),
                JSONUtils.getInt(object, "count", 1));
        if (object.has("nbt")) {
            try {
                stack.setTag(JsonToNBT.getTagFromJson(object.get("nbt").getAsString()));
            } catch (CommandSyntaxException e) {
                Titanium.LOGGER.catching(e);
            }
        }
        return stack;
    }

    public static JsonObject writeBiomeType(Biome biome) {
        JsonObject object = new JsonObject();
        object.addProperty("biome", biome.getRegistryName().toString());
        return object;
    }

    public static Biome readBiomeType(JsonElement element) {
        return ForgeRegistries.BIOMES.getValue(new ResourceLocation(element.getAsString()));
    }

    public interface Writer<T> {
        JsonElement write(T type);
    }

    public interface Reader<T> {
        T read(JsonElement element);
    }
}

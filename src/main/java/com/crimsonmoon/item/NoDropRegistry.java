package com.crimsonmoon.item;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.HashSet;
import java.util.Set;

public class NoDropRegistry {

    private static final Set<Item> PROTECTED_ITEMS = new HashSet<>();

    public static void protect(Item item) {
        PROTECTED_ITEMS.add(item);
    }

    public static boolean isProtected(ItemStack stack) {
        return !stack.isEmpty() && PROTECTED_ITEMS.contains(stack.getItem());
    }
}
package com.crimsonmoon.item;

import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PendingItemStash {

    private static final Map<UUID, List<ItemStack>> STASH = new HashMap<>();

    public static void add(UUID playerId, ItemStack stack) {
        STASH.computeIfAbsent(playerId, id -> new ArrayList<>()).add(stack.copy());
    }

    public static List<ItemStack> take(UUID playerId) {
        List<ItemStack> stacks = STASH.remove(playerId);
        return stacks != null ? stacks : List.of();
    }
}
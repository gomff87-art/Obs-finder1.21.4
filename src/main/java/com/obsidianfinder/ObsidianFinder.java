package com.obsidianfinder;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.HashSet;
import java.util.Set;

public class ObsidianFinder implements ClientModInitializer {

    private static final int RADIUS = 50;
    private static final int SCAN_EVERY_TICKS = 60;
    private static final int MAX_PER_SCAN = 10;

    private boolean enabled = false;
    private int tickCounter = 0;
    private final Set<Long> reported = new HashSet<>();

    @Override
    public void onInitializeClient() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
            dispatcher.register(ClientCommandManager.literal("obsidian")
                .then(ClientCommandManager.literal("start").executes(ctx -> {
                    enabled = true;
                    tickCounter = SCAN_EVERY_TICKS;
                    ctx.getSource().sendFeedback(Text.literal("§a[Obsidian] Поиск включён. Лети, координаты будут в чате."));
                    return 1;
                }))
                .then(ClientCommandManager.literal("stop").executes(ctx -> {
                    enabled = false;
                    ctx.getSource().sendFeedback(Text.literal("§c[Obsidian] Поиск выключен."));
                    return 1;
                }))
                .then(ClientCommandManager.literal("clear").executes(ctx -> {
                    reported.clear();
                    ctx.getSource().sendFeedback(Text.literal("§e[Obsidian] Список очищен."));
                    return 1;
                }))
            )
        );

        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
    }

    private void onTick(MinecraftClient client) {
        if (!enabled || client.player == null || client.world == null) return;
        if (++tickCounter < SCAN_EVERY_TICKS) return;
        tickCounter = 0;
        scan(client);
    }

    private void scan(MinecraftClient client) {
        ClientWorld world = client.world;
        BlockPos center = client.player.getBlockPos();

        int minY = Math.max(world.getBottomY(), center.getY() - RADIUS);
        int maxY = Math.min(world.getBottomY() + world.getHeight() - 1, center.getY() + RADIUS);

        BlockPos.Mutable pos = new BlockPos.Mutable();
        int sent = 0;
        int extra = 0;

        for (int x = center.getX() - RADIUS; x <= center.getX() + RADIUS; x++) {
            for (int z = center.getZ() - RADIUS; z <= center.getZ() + RADIUS; z++) {
                if (!world.getChunkManager().isChunkLoaded(x >> 4, z >> 4)) continue;

                for (int y = minY; y <= maxY; y++) {
                    pos.set(x, y, z);
                    Block block = world.getBlockState(pos).getBlock();
                    boolean obs = block == Blocks.OBSIDIAN;
                    boolean cry = block == Blocks.CRYING_OBSIDIAN;
                    if (!obs && !cry) continue;

                    long key = pos.asLong();
                    if (reported.contains(key)) continue;

                    if (sent < MAX_PER_SCAN) {
                        reported.add(key);
                        String name = obs ? "§5Обсидиан" : "§dПлачущий обсидиан";
                        client.player.sendMessage(Text.literal(
                            "[Obsidian] " + name + "§f: " + x + " " + y + " " + z), false);
                        sent++;
                    } else {
                        extra++;
                    }
                }
            }
        }

        if (extra > 0) {
            client.player.sendMessage(Text.literal(
                "§7[Obsidian] ...и ещё " + extra + " новых блоков."), false);
        }
    }
  }

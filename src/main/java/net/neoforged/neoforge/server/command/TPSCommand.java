/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.neoforge.server.command;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.text.DecimalFormat;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.TimeUtil;
import net.minecraft.world.level.dimension.DimensionType;

class TPSCommand {
    private static final DecimalFormat TIME_FORMATTER = new DecimalFormat("########0.000");
    private static final long[] UNLOADED = new long[] { 0 };

    static ArgumentBuilder<CommandSourceStack, ?> register() {
        return Commands.literal("tps")
                .requires(cs -> cs.hasPermission(0)) //permission
                .then(Commands.argument("dim", DimensionArgument.dimension())
                        .executes(ctx -> sendTime(ctx.getSource(), DimensionArgument.getDimension(ctx, "dim"))))
                .executes(ctx -> {
                    for (ServerLevel dim : ctx.getSource().getServer().getAllLevels())
                        sendTime(ctx.getSource(), dim);

                    long[] times = ctx.getSource().getServer().getTickTimesNanos();
                    double meanTickTime = mean(times) * 1.0E-6D;
                    double meanTPS = TimeUtil.MILLISECONDS_PER_SECOND / Math.max(meanTickTime, ctx.getSource().getServer().tickRateManager().millisecondsPerTick());
                    ctx.getSource().sendSuccess(() -> Component.translatable("commands.neoforge.tps.summary.all", TIME_FORMATTER.format(meanTickTime), TIME_FORMATTER.format(meanTPS)), false);

                    return 0;
                });
    }

    private static int sendTime(CommandSourceStack cs, ServerLevel dim) throws CommandSyntaxException {
        long[] times = cs.getServer().getTickTime(dim.dimension());

        if (times == null) // Null means the world is unloaded. Not invalid. That's taken care of by DimensionArgument itself.
            times = UNLOADED;

        final Registry<DimensionType> reg = cs.registryAccess().registryOrThrow(Registries.DIMENSION_TYPE);
        double worldTickTime = mean(times) * 1.0E-6D;
        double worldTPS = TimeUtil.MILLISECONDS_PER_SECOND / Math.max(worldTickTime, dim.tickRateManager().millisecondsPerTick());
        cs.sendSuccess(() -> Component.translatable("commands.neoforge.tps.summary.named", dim.getDescription(), reg.getKey(dim.dimensionType()).toString(), TIME_FORMATTER.format(worldTickTime), TIME_FORMATTER.format(worldTPS)), false);

        return 1;
    }

    private static long mean(long[] values) {
        long sum = 0L;
        for (long v : values)
            sum += v;
        return sum / values.length;
    }
}

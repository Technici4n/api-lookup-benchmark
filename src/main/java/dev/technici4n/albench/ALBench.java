package dev.technici4n.albench;

import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.lookup.v1.block.BlockApiCache;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.registry.Registry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

import static net.minecraft.server.command.CommandManager.literal;

public class ALBench implements ModInitializer {
	public static Block FOR_BLOCK_ENTITY;
	public static Block FOR_BLOCK;

	@Override
	public void onInitialize() {
		CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
			dispatcher.register(literal("albench").executes(ALBench::runTests));
		});

		FOR_BLOCK_ENTITY = block("for_block_entity", LookupKind.FOR_BLOCK_ENTITY);
		FOR_BLOCK = block("for_block", LookupKind.FOR_BLOCK);
	}

	private static Block block(String id, LookupKind kind) {
		var beSupplier = new Supplier<BlockEntityType<TankBlockEntity>>() {
			private BlockEntityType<TankBlockEntity> bet;

			@Override
			public BlockEntityType<TankBlockEntity> get() {
				return bet;
			}
		};
		var block = new TankBlock(beSupplier);
		Registry.register(Registry.BLOCK, makeId(id), block);
		var type = FabricBlockEntityTypeBuilder.create(TankBlockEntity.of(beSupplier), block).build();
		beSupplier.bet = type;
		Registry.register(Registry.BLOCK_ENTITY_TYPE, makeId(id), type);

		switch (kind) {
			case FOR_BLOCK_ENTITY -> FluidStorage.SIDED.registerForBlockEntity((be, dir) -> be.fluidStorage, type);
			case FOR_BLOCK -> FluidStorage.SIDED.registerForBlocks((world, pos, state, be, dir) -> {
				if (be != null) {
					return ((TankBlockEntity) be).fluidStorage;
				}
				return null;
			}, block);
		}

		return block;
	}

	private enum LookupKind {
		FOR_BLOCK_ENTITY, FOR_BLOCK,
	}

	private static Identifier makeId(String id) {
		return new Identifier("albench", id);
	}

	private static int runTests(CommandContext<ServerCommandSource> context) {
		var world = context.getSource().getWorld();

		// Fill with many of our custom block, a few cauldrons, and a few empty spaces.
		int y = 100;
		int M = 50;
		int N = 50;
		int cauldrons = 0;
		int air = 0;
		int bes = N - cauldrons - air;

		for (int cacheType = 2; cacheType --> 0;) {
			for (LookupKind kind : LookupKind.values()) {
				var block = switch (kind) {
					case FOR_BLOCK -> FOR_BLOCK;
					case FOR_BLOCK_ENTITY -> FOR_BLOCK_ENTITY;
				};

				BlockApiCache<Storage<FluidVariant>, Direction>[][] caches = new BlockApiCache[M][N];
				List<BlockPos> positions = new ArrayList<>();
				for (int x = 0; x < M; ++x) {
					for (int z = 0; z < N; ++z) {
						var pos = new BlockPos(x, y, z);
						positions.add(pos);
						if (z < cauldrons) {
							world.setBlockState(pos, Blocks.CAULDRON.getDefaultState());
						} else if (z < cauldrons + air) {
							world.setBlockState(pos, Blocks.AIR.getDefaultState());
						} else {
							world.setBlockState(pos, block.getDefaultState());
						}
						// Build cache for query below
						if (cacheType == 0) {
							caches[x][z] = BlockApiCache.create(FluidStorage.SIDED, world, pos);
						} else {
							caches[x][z] = CustomCache.create(FluidStorage.SIDED, world, pos);
						}
					}
				}

				var random = new Random(42);
				Collections.shuffle(positions, random);

				// Do many queries
				var t1 = System.nanoTime();
				int expectedTotal = M * (cauldrons + bes);
				int loops = 5 * 20_000;
				for (int loop = 0; loop < loops; ++loop) {
					int totalFound = 0;
					for (var pos : positions) {
						var api = caches[pos.getX()][pos.getZ()].find(Direction.UP);
						if (api != null) {
							totalFound++;
						}
					}
					if (totalFound != expectedTotal) {
						System.err.println("Wrong count!");
					}
				}

				var t2 = System.nanoTime();
				var secs = (t2 - t1) / 1e9;
				var queries = M * N * loops;
				var msg = "Took %.1f seconds for %.0e queries (%.0f queries/s)".formatted(secs, (double) queries, queries / secs);

				msg += " [%d cauldrons, %d air, %d BEs per stripe, kind=%s, custom cache=%s]".formatted(cauldrons, air, bes, kind, cacheType == 1);

				context.getSource().sendFeedback(new LiteralText(msg), false);
			}
		}

		return 1;
	}
}

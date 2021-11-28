package dev.technici4n.albench;

import dev.technici4n.albench.mixin.BlockApiCacheImplAccessor;
import net.fabricmc.fabric.api.lookup.v1.block.BlockApiCache;
import net.fabricmc.fabric.api.lookup.v1.block.BlockApiLookup;
import net.fabricmc.fabric.impl.lookup.block.BlockApiCacheImpl;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

public class CustomCache<A, C> implements BlockApiCache<A, C> {
	public static <A, C> CustomCache<A, C> create(BlockApiLookup<A, C> lookup, ServerWorld world, BlockPos pos) {
		return new CustomCache<>(lookup, world, pos);
	}

	private final BlockApiCache<A, C> cache;
	@Nullable
	private A cachedApi = null;

	private final Runnable invalidateCallback = () -> cachedApi = null;

	private CustomCache(BlockApiLookup<A, C> lookup, ServerWorld world, BlockPos pos) {
		this.cache = BlockApiCache.create(lookup, world, pos);
	}

	@Override
	public @Nullable A find(@Nullable BlockState state, C context) {
		if (cachedApi != null) {
			return cachedApi;
		}
		A foundApi = cache.find(state, context);
		if (foundApi != null) {
			BlockEntity be = ((BlockApiCacheImplAccessor) cache).getCachedBlockEntity();
			if (be instanceof CustomCacheable cacheable) {
				cacheable.registerCallback(invalidateCallback);
				cachedApi = foundApi;
			}
		}
		return foundApi;
	}
}

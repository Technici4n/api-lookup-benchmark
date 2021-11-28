package dev.technici4n.albench;

import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantStorage;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.BlockPos;

import java.util.Set;
import java.util.function.Supplier;

public class TankBlockEntity extends BlockEntity implements CustomCacheable {
	public static FabricBlockEntityTypeBuilder.Factory<TankBlockEntity> of(Supplier<BlockEntityType<TankBlockEntity>> type) {
		return (pos, state) -> new TankBlockEntity(type.get(), pos, state);
	}

	public Storage<FluidVariant> fluidStorage = new SingleVariantStorage<>() {
		@Override
		protected FluidVariant getBlankVariant() {
			return FluidVariant.blank();
		}

		@Override
		protected long getCapacity(FluidVariant variant) {
			return FluidConstants.BUCKET;
		}
	};

	public TankBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	private final Set<Runnable> cacheInvalidateCallbacks = new ReferenceOpenHashSet<>();

	@Override
	public void registerCallback(Runnable invalidateCallback) {
		cacheInvalidateCallbacks.add(invalidateCallback);
	}

	@Override
	public void markRemoved() {
		cacheInvalidateCallbacks.forEach(Runnable::run);
	}
}

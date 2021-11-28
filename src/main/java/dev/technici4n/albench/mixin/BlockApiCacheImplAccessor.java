package dev.technici4n.albench.mixin;

import net.fabricmc.fabric.impl.lookup.block.BlockApiCacheImpl;
import net.minecraft.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BlockApiCacheImpl.class)
public interface BlockApiCacheImplAccessor {
	@Accessor
	BlockEntity getCachedBlockEntity();
}

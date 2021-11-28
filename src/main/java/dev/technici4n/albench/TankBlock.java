package dev.technici4n.albench;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.Material;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public class TankBlock extends BlockWithEntity {
	private final Supplier<BlockEntityType<TankBlockEntity>> bet;

	public TankBlock(Supplier<BlockEntityType<TankBlockEntity>> bet) {
		super(Settings.of(Material.METAL));
		this.bet = bet;
	}

	@Nullable
	@Override
	public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
		return bet.get().instantiate(pos, state);
	}
}

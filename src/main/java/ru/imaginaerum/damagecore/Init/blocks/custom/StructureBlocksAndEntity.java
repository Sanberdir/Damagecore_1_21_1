package ru.imaginaerum.damagecore.Init.blocks.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.network.PacketDistributor;
import ru.imaginaerum.damagecore.Init.blocks.entity.DCBlockEntities;
import ru.imaginaerum.damagecore.Init.blocks.entity.StructureBlockEntity;

public class StructureBlocksAndEntity extends Block implements EntityBlock {

    public StructureBlocksAndEntity(Properties properties) {
        super(properties);
    }
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new StructureBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        // при необходимости: if (!player.hasPermissions(2)) return InteractionResult.PASS;
        if (level.getBlockEntity(pos) instanceof StructureBlockEntity be
                && player instanceof ServerPlayer sp) {
            PacketDistributor.sendToPlayer(sp,
                    new BlockConfigNetwork.OpenConfig(pos, be.getEntityIds(), be.getBlockIds()));
        }
        return InteractionResult.CONSUME;
    }
    // импорты: BlockEntityTicker, BlockEntityType, DCBlockEntities
    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> type) {
        if (level.isClientSide() || type != DCBlockEntities.STRUCTURE_BE.get()) return null;
        return (BlockEntityTicker<T>) (BlockEntityTicker<StructureBlockEntity>)
                StructureBlockEntity::serverTick;
    }
    /** Полный куб для выделения и коллизии (по умолчанию так и есть, оставлено явно). */
    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
                                  net.minecraft.world.phys.shapes.CollisionContext ctx) {
        return Shapes.block();
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
                                           net.minecraft.world.phys.shapes.CollisionContext ctx) {
        return Shapes.block();
    }

    /** Ключевой метод: грани не считаются прочными, поэтому заборы, решётки и стены не соединяются. */
    @Override
    protected VoxelShape getBlockSupportShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.empty();
    }
}

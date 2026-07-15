package com.perigrine3.createcybernetics.block.ironsspells;

import com.mojang.serialization.MapCodec;
import com.perigrine3.createcybernetics.compat.ironsspells.IronsSpellbooksCompatBlockEntities;
import com.perigrine3.createcybernetics.compat.ironsspells.IronsSpellbooksStaffCompat;
import com.perigrine3.createcybernetics.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class AnomalyCoreBlock extends BaseEntityBlock {
    public static final MapCodec<AnomalyCoreBlock> CODEC = simpleCodec(AnomalyCoreBlock::new);

    /*
     * False = placed block, hidden from normal chunk rendering.
     * True  = temporary render-state used only by the BlockEntityRenderer.
     */
    public static final BooleanProperty RENDERING = BooleanProperty.create("rendering");

    private static final VoxelShape SHAPE = Block.box(
            4.0D, 4.0D, 4.0D,
            12.0D, 12.0D, 12.0D
    );

    public AnomalyCoreBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(RENDERING, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(RENDERING);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        /*
         * The real placed block uses ENTITYBLOCK_ANIMATED so the static block model
         * is not rendered by the chunk renderer.
         *
         * The renderer temporarily flips RENDERING=true and asks Minecraft to render
         * the normal block model manually after applying rotation.
         */
        return state.getValue(RENDERING) ? RenderShape.MODEL : RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AnomalyCoreBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level,
            BlockState state,
            BlockEntityType<T> blockEntityType
    ) {
        if (level.isClientSide) {
            return null;
        }

        return createTickerHelper(
                blockEntityType,
                IronsSpellbooksCompatBlockEntities.ANOMALY_CORE.get(),
                AnomalyCoreBlockEntity::serverTick
        );
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hitResult
    ) {
        if (!level.isClientSide
                && level.getBlockEntity(pos) instanceof AnomalyCoreBlockEntity blockEntity) {
            blockEntity.tryStartRitual(player);
        }

        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected ItemInteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hitResult
    ) {
        if (IronsSpellbooksStaffCompat.isIronsSpellbooksStaff(stack)) {
            if (!level.isClientSide) {
                transformStaffIntoAnomalousStaff(level, pos, player, hand);
            }

            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }

        if (!level.isClientSide
                && level.getBlockEntity(pos) instanceof AnomalyCoreBlockEntity blockEntity) {
            blockEntity.tryStartRitual(player);
        }

        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    private void transformStaffIntoAnomalousStaff(
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand
    ) {
        ItemStack anomalousStaff = new ItemStack(ModItems.ANOMALOUS_STAFF.get());

        /*
         * Replace the exact staff stack in the hand used to activate the core.
         * The original staff and its spell container contents are consumed.
         */
        player.setItemInHand(hand, anomalousStaff);

        /*
         * false prevents the Anomaly Core block item from being dropped.
         */
        level.destroyBlock(pos, false);
    }

    @Override
    protected VoxelShape getShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context
    ) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context
    ) {
        return SHAPE;
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    @Override
    protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 1.0F;
    }
}
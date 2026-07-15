package com.perigrine3.createcybernetics.block.entity;

import com.perigrine3.createcybernetics.block.ComputerTowerBlock;
import com.perigrine3.createcybernetics.screen.custom.computer.ComputerTowerMenu;
import com.perigrine3.createcybernetics.util.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ComputerTowerBlockEntity extends BaseContainerBlockEntity {
    private static final String TAG_LOOT_TABLE =
            "LootTable";

    private static final String TAG_LOOT_TABLE_SEED =
            "LootTableSeed";

    public static final int INVENTORY_SIZE = 24;

    private NonNullList<ItemStack> items =
            NonNullList.withSize(INVENTORY_SIZE, ItemStack.EMPTY);

    private ResourceLocation lootTable;
    private long lootTableSeed;

    public ComputerTowerBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        super(
                ModBlockEntities.COMPUTER_TOWER.get(),
                pos,
                state
        );
    }

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            ComputerTowerBlockEntity tower
    ) {
        if (level.isClientSide) {
            return;
        }

        tower.generateLootIfNeeded(null);
    }

    public void forceGenerateLootFromBlockNbt(
            @Nullable Player player
    ) {
        generateLootIfNeeded(
                player
        );
    }

    private void generateLootIfNeeded(
            @Nullable Player player
    ) {
        if (level == null || level.isClientSide) {
            return;
        }

        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        if (lootTable == null) {
            return;
        }

        ResourceLocation lootTableId =
                lootTable;

        ResourceKey<LootTable> lootTableKey =
                ResourceKey.create(
                        Registries.LOOT_TABLE,
                        lootTableId
                );

        LootTable table =
                serverLevel.getServer()
                        .reloadableRegistries()
                        .getLootTable(
                                lootTableKey
                        );

        LootParams.Builder params =
                new LootParams.Builder(serverLevel)
                        .withParameter(
                                LootContextParams.ORIGIN,
                                Vec3.atCenterOf(worldPosition)
                        );

        if (player != null) {
            params.withLuck(
                    player.getLuck()
            );
        }

        LootParams lootParams =
                params.create(
                        LootContextParamSets.CHEST
                );

        NonNullList<ItemStack> generatedItems =
                NonNullList.withSize(
                        INVENTORY_SIZE,
                        ItemStack.EMPTY
                );

        int filledSlots =
                0;

        /*
         * LootTableSeed 0L should behave like randomized loot.
         * Do not reuse 0L as a deterministic seed here.
         */
        for (int attempt = 0; attempt < 8 && filledSlots <= 0; attempt++) {
            long seed =
                    lootTableSeed == 0L
                            ? serverLevel.getRandom().nextLong()
                            : lootTableSeed + attempt;

            List<ItemStack> generatedLoot =
                    table.getRandomItems(
                            lootParams,
                            seed
                    );

            int slot =
                    0;

            generatedItems =
                    NonNullList.withSize(
                            INVENTORY_SIZE,
                            ItemStack.EMPTY
                    );

            for (ItemStack generatedStack : generatedLoot) {
                if (generatedStack.isEmpty()) {
                    continue;
                }

                while (!generatedStack.isEmpty() &&
                        slot < INVENTORY_SIZE) {
                    ItemStack insertedStack =
                            generatedStack.copy();

                    insertedStack.setCount(1);

                    generatedItems.set(
                            slot,
                            insertedStack
                    );

                    generatedStack.shrink(1);
                    slot++;
                }

                if (slot >= INVENTORY_SIZE) {
                    break;
                }
            }

            filledSlots =
                    slot;
        }

        if (filledSlots <= 0) {
            return;
        }

        lootTable = null;
        lootTableSeed = 0L;

        items = generatedItems;

        setChanged();
        updateStoredShardState();

        BlockState currentState =
                level.getBlockState(worldPosition);

        level.sendBlockUpdated(
                worldPosition,
                currentState,
                currentState,
                Block.UPDATE_ALL
        );

        level.updateNeighbourForOutputSignal(
                worldPosition,
                currentState.getBlock()
        );
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable(
                "container.createcybernetics.computer_tower"
        );
    }

    @Override
    protected AbstractContainerMenu createMenu(
            int containerId,
            Inventory playerInventory
    ) {
        forceGenerateLootFromBlockNbt(
                playerInventory.player
        );

        return new ComputerTowerMenu(
                containerId,
                playerInventory,
                this
        );
    }

    @Override
    public int getContainerSize() {
        return INVENTORY_SIZE;
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    protected void setItems(
            NonNullList<ItemStack> items
    ) {
        this.items = items;
    }

    @Override
    public boolean canPlaceItem(
            int slot,
            ItemStack stack
    ) {
        return stack.is(ModTags.Items.DATA_SHARDS);
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public int getMaxStackSize(
            ItemStack stack
    ) {
        return 1;
    }

    @Override
    public void setItem(
            int slot,
            ItemStack stack
    ) {
        if (!stack.isEmpty() &&
                stack.getCount() > getMaxStackSize()) {
            stack.setCount(
                    getMaxStackSize()
            );
        }

        super.setItem(
                slot,
                stack
        );
    }

    public int getStoredShardCount() {
        int count = 0;

        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                count++;
            }
        }

        return count;
    }

    public int getComparatorOutput() {
        int storedShards =
                getStoredShardCount();

        if (storedShards <= 0) {
            return 0;
        }

        return Math.min(
                15,
                1 + Math.floorDiv(
                        (storedShards - 1) * 14,
                        INVENTORY_SIZE - 1
                )
        );
    }

    @Override
    public void setChanged() {
        super.setChanged();

        if (level == null || level.isClientSide) {
            return;
        }

        updateStoredShardState();

        BlockState currentState =
                level.getBlockState(worldPosition);

        level.sendBlockUpdated(
                worldPosition,
                currentState,
                currentState,
                Block.UPDATE_CLIENTS
        );

        level.updateNeighbourForOutputSignal(
                worldPosition,
                currentState.getBlock()
        );
    }

    private void updateStoredShardState() {
        if (level == null || level.isClientSide) {
            return;
        }

        BlockState currentState =
                level.getBlockState(worldPosition);

        if (!currentState.hasProperty(ComputerTowerBlock.STORED_SHARDS)) {
            return;
        }

        int storedShardCount =
                getStoredShardCount();

        if (currentState.getValue(ComputerTowerBlock.STORED_SHARDS) ==
                storedShardCount) {
            return;
        }

        BlockState updatedState =
                currentState.setValue(
                        ComputerTowerBlock.STORED_SHARDS,
                        storedShardCount
                );

        level.setBlock(
                worldPosition,
                updatedState,
                Block.UPDATE_ALL
        );

        level.updateNeighbourForOutputSignal(
                worldPosition,
                updatedState.getBlock()
        );
    }

    @Override
    protected void saveAdditional(
            CompoundTag tag,
            HolderLookup.Provider provider
    ) {
        super.saveAdditional(
                tag,
                provider
        );

        if (lootTable != null) {
            tag.putString(
                    TAG_LOOT_TABLE,
                    lootTable.toString()
            );

            tag.putLong(
                    TAG_LOOT_TABLE_SEED,
                    lootTableSeed
            );

            return;
        }

        ContainerHelper.saveAllItems(
                tag,
                items,
                provider
        );
    }

    @Override
    protected void loadAdditional(
            CompoundTag tag,
            HolderLookup.Provider provider
    ) {
        super.loadAdditional(
                tag,
                provider
        );

        items = NonNullList.withSize(
                INVENTORY_SIZE,
                ItemStack.EMPTY
        );

        lootTable = null;
        lootTableSeed = 0L;

        if (tag.contains(
                TAG_LOOT_TABLE
        )) {
            lootTable =
                    ResourceLocation.tryParse(
                            tag.getString(
                                    TAG_LOOT_TABLE
                            )
                    );

            if (tag.contains(
                    TAG_LOOT_TABLE_SEED
            )) {
                lootTableSeed =
                        tag.getLong(
                                TAG_LOOT_TABLE_SEED
                        );
            }

            return;
        }

        ContainerHelper.loadAllItems(
                tag,
                items,
                provider
        );
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(
            HolderLookup.Provider provider
    ) {
        return saveWithoutMetadata(provider);
    }
}
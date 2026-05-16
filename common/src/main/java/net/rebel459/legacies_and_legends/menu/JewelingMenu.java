package net.rebel459.legacies_and_legends.menu;

import net.minecraft.tags.BlockTags;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.rebel459.legacies_and_legends.block.JewelingTableBlock;
import net.rebel459.legacies_and_legends.item.WandItem;
import net.rebel459.legacies_and_legends.registry.LaLDataComponents;
import net.rebel459.legacies_and_legends.registry.LaLMenus;
import net.rebel459.legacies_and_legends.sound.LaLSounds;
import net.rebel459.legacies_and_legends.util.Gem;

public class JewelingMenu extends AbstractContainerMenu {
    private static final int WAND_SLOT = 0;
    private static final int PRIMARY_SLOT = 1;
    private static final int SECONDARY_SLOT = 2;
    private static final int CONTAINER_SLOT_COUNT = 3;
    private static final int PLAYER_INV_START = CONTAINER_SLOT_COUNT;
    private static final int PLAYER_INV_END = PLAYER_INV_START + 27;
    private static final int HOTBAR_START = PLAYER_INV_END;
    private static final int HOTBAR_END = HOTBAR_START + 9;

    private final Container container;
    private final Player player;
    private boolean syncing;
    private ItemStack lastWand = ItemStack.EMPTY;
    private ItemStack lastPrimary = ItemStack.EMPTY;
    private ItemStack lastSecondary = ItemStack.EMPTY;
    private final ContainerLevelAccess access;

    public JewelingMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, ContainerLevelAccess.NULL);
    }

    public JewelingMenu(int containerId, Inventory inventory, ContainerLevelAccess access) {
        super(LaLMenus.JEWLING.get(), containerId);

        this.player = inventory.player;
        this.container = new SimpleContainer(3);
        this.access = access;

        this.addSlot(new Slot(container, WAND_SLOT, 34, 47) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof WandItem;
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }

            @Override
            public void onTake(Player player, ItemStack stack) {
                player.playSound(LaLSounds.JEWEL.get());
                JewelingMenu.this.applyGemSlotsToWand(stack);
                JewelingMenu.this.clearGemSlots();
                super.onTake(player, stack);
            }
        });

        this.addSlot(new Slot(container, PRIMARY_SLOT, 88, 47) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.has(LaLDataComponents.GEM.get()) && !JewelingMenu.this.matchesOtherGem(stack, SECONDARY_SLOT) && hasWand();
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });

        this.addSlot(new Slot(container, SECONDARY_SLOT, 124, 47) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.has(LaLDataComponents.GEM.get()) && !JewelingMenu.this.matchesOtherGem(stack, PRIMARY_SLOT) && hasWand();
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });

        this.addStandardInventorySlots(inventory, 8, 84);
        this.snapshotCurrentState();
    }

    @Override
    public void broadcastChanges() {
        this.syncMenuState();
        super.broadcastChanges();
    }

    @Override
    public void clicked(int slotId, int button, ContainerInput input, Player player) {
        super.clicked(slotId, button, input, player);
        this.syncMenuState();
    }

    private void syncMenuState() {
        if (this.syncing || player.level().isClientSide()) return;

        ItemStack wand = this.container.getItem(WAND_SLOT);
        ItemStack primary = this.container.getItem(PRIMARY_SLOT);
        ItemStack secondary = this.container.getItem(SECONDARY_SLOT);

        boolean wandChanged = !ItemStack.isSameItemSameComponents(wand, this.lastWand);
        boolean gemsChanged = !ItemStack.isSameItemSameComponents(primary, this.lastPrimary) || !ItemStack.isSameItemSameComponents(secondary, this.lastSecondary);

        if (!wandChanged && !gemsChanged) return;

        this.syncing = true;
        try {
            if (wandChanged) {
                this.loadGemsFromWand();
            } else {
                this.writeGemsToWand(wand);
            }
        } finally {
            this.syncing = false;
        }

        this.snapshotCurrentState();
        this.broadcastFullState();
    }

    private void loadGemsFromWand() {
        ItemStack wand = this.container.getItem(WAND_SLOT);
        if (wand.isEmpty()) {
            return;
        }

        if (!(wand.getItem() instanceof WandItem)) {
            this.clearGemSlots();
            return;
        }

        Gem.Slots gems = WandItem.getGems(wand);
        this.container.setItem(PRIMARY_SLOT, gems.primary() == Gem.EMPTY ? ItemStack.EMPTY : gems.primary().item().getDefaultInstance());
        this.container.setItem(SECONDARY_SLOT, gems.secondary() == Gem.EMPTY ? ItemStack.EMPTY : gems.secondary().item().getDefaultInstance());
    }

    private void writeGemsToWand(ItemStack wand) {
        if (wand.isEmpty() || !(wand.getItem() instanceof WandItem)) {
            return;
        }

        this.applyGemSlotsToWand(wand);
        this.container.setItem(WAND_SLOT, wand);
    }

    private void applyGemSlotsToWand(ItemStack wand) {
        if (wand.isEmpty() || !(wand.getItem() instanceof WandItem)) {
            return;
        }

        ItemStack primaryStack = this.container.getItem(PRIMARY_SLOT);
        ItemStack secondaryStack = this.container.getItem(SECONDARY_SLOT);
        Gem primary = primaryStack.getOrDefault(LaLDataComponents.GEM.get(), Gem.EMPTY);
        Gem secondary = secondaryStack.getOrDefault(LaLDataComponents.GEM.get(), Gem.EMPTY);
        if (primary != Gem.EMPTY && primary == secondary) {
            secondary = Gem.EMPTY;
            this.clearSlot(SECONDARY_SLOT);
        }
        WandItem.setGems(wand, primary, secondary);
    }

    private boolean matchesOtherGem(ItemStack stack, int otherSlot) {
        Gem gem = stack.getOrDefault(LaLDataComponents.GEM.get(), Gem.EMPTY);
        if (gem == Gem.EMPTY) {
            return false;
        }

        return this.container.getItem(otherSlot).getOrDefault(LaLDataComponents.GEM.get(), Gem.EMPTY) == gem;
    }

    private boolean hasWand() {
        ItemStack stack = this.container.getItem(WAND_SLOT);
        return stack != ItemStack.EMPTY && stack.getItem() instanceof WandItem;
    }

    private void clearSlot(int slot) {
        boolean wasSyncing = this.syncing;
        this.syncing = true;
        try {
            this.container.setItem(slot, ItemStack.EMPTY);
        } finally {
            this.syncing = wasSyncing;
        }
    }

    private void clearGemSlots() {
        boolean wasSyncing = this.syncing;
        this.syncing = true;
        try {
            this.clearSlot(PRIMARY_SLOT);
            this.clearSlot(SECONDARY_SLOT);
        } finally {
            this.syncing = wasSyncing;
        }

        this.snapshotCurrentState();
        if (!this.player.level().isClientSide()) {
            this.broadcastFullState();
        }
    }

    private void snapshotCurrentState() {
        this.lastWand = this.container.getItem(WAND_SLOT).copy();
        this.lastPrimary = this.container.getItem(PRIMARY_SLOT).copy();
        this.lastSecondary = this.container.getItem(SECONDARY_SLOT).copy();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack movedStack;
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = slot.getItem();
        movedStack = stack.copy();

        if (index < CONTAINER_SLOT_COUNT) {
            if (!this.moveItemStackTo(stack, PLAYER_INV_START, HOTBAR_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (stack.getItem() instanceof WandItem) {
            if (!this.moveItemStackTo(stack, WAND_SLOT, WAND_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (stack.has(LaLDataComponents.GEM.get())) {
            if (!this.moveItemStackTo(stack, PRIMARY_SLOT, SECONDARY_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (index < PLAYER_INV_END) {
            if (!this.moveItemStackTo(stack, HOTBAR_START, HOTBAR_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!this.moveItemStackTo(stack, PLAYER_INV_START, PLAYER_INV_END, false)) {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        if (stack.getCount() == movedStack.getCount()) {
            return ItemStack.EMPTY;
        }

        slot.onTake(player, stack);
        this.syncMenuState();
        return movedStack;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.access.evaluate((level, pos) -> this.isValidBlock(level.getBlockState(pos)) && player.isWithinBlockInteractionRange(pos, 4.0F), true);
    }

    private boolean isValidBlock(BlockState state) {
        return state.getBlock() instanceof JewelingTableBlock;
    }

    @Override
    public void removed(Player player) {
        if (this.slots.getFirst().hasItem()) player.playSound(LaLSounds.JEWEL.get());
        this.syncing = true;
        try {
            this.applyGemSlotsToWand(this.container.getItem(WAND_SLOT));
            this.clearSlot(PRIMARY_SLOT);
            this.clearSlot(SECONDARY_SLOT);
            this.clearContainer(player, this.container);
        } finally {
            this.syncing = false;
        }
        this.lastWand = ItemStack.EMPTY;
        this.lastPrimary = ItemStack.EMPTY;
        this.lastSecondary = ItemStack.EMPTY;
        super.removed(player);
    }
}

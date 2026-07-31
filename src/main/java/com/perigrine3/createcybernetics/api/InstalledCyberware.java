package com.perigrine3.createcybernetics.api;

import com.perigrine3.createcybernetics.common.durability.CyberwareDurabilityData;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

public class InstalledCyberware {

    private ItemStack item = ItemStack.EMPTY;
    private CyberwareSlot slot = null;
    private int index = -1;
    private int humanityCost = 0;
    private boolean powered = true;
    private int durability = -1;
    private int naturalRepairFatigue = 0;
    private long lastPassiveDurabilityTick = 0L;
    private long energyReceivedSinceDurabilityDamage = 0L;
    private long energyExtractedSinceDurabilityDamage = 0L;

    public InstalledCyberware() {}

    public InstalledCyberware(ItemStack item, CyberwareSlot slot, int index, int humanityCost) {
        this.item = item == null ? ItemStack.EMPTY : item.copy();
        this.slot = slot;
        this.index = index;
        this.humanityCost = humanityCost;
        initializeDurability();
    }

    public ItemStack getItem() {
        return item;
    }

    public CyberwareSlot getSlot() {
        return slot;
    }

    public int getIndex() {
        return index;
    }

    public int getHumanityCost() {
        return humanityCost;
    }

    public boolean isPowered() {
        return powered;
    }

    public void setPowered(boolean powered) {
        this.powered = powered;
    }

    public int getMaxDurability() {
        if (item == null || item.isEmpty()) return 0;
        if (!(item.getItem() instanceof ICyberwareItem cyberwareItem)) return 0;

        return Math.max(1, cyberwareItem.getMaxCyberwareDurability(item, slot));
    }

    public int getDurability() {
        initializeDurability();

        int maxDurability = getMaxDurability();
        if (maxDurability <= 0) return 0;

        return Mth.clamp(durability, 0, maxDurability);
    }

    public void setDurability(int durability) {
        int maxDurability = getMaxDurability();

        if (maxDurability <= 0) {
            this.durability = 0;
            return;
        }

        this.durability = Mth.clamp(durability, 0, maxDurability);
        CyberwareDurabilityData.setDurability(item, slot, this.durability);
    }

    public int damageDurability(int amount) {
        if (amount <= 0) return 0;

        int previous = getDurability();
        setDurability(previous - amount);

        return previous - getDurability();
    }

    public int repairDurability(int amount) {
        if (amount <= 0) return 0;

        int previous = getDurability();
        setDurability(previous + amount);

        return getDurability() - previous;
    }

    public void setToMaxDurability() {
        setDurability(getMaxDurability());
    }

    public boolean isBroken() {
        return getMaxDurability() > 0 && getDurability() <= 0;
    }

    public boolean isAtMaxDurability() {
        return getDurability() >= getMaxDurability();
    }

    public float getDurabilityPercent() {
        int maxDurability = getMaxDurability();
        if (maxDurability <= 0) return 1.0F;

        return Mth.clamp((float) getDurability() / (float) maxDurability, 0.0F, 1.0F);
    }

    public int getNaturalRepairFatigue() {
        return Math.max(0, naturalRepairFatigue);
    }

    public void setNaturalRepairFatigue(int naturalRepairFatigue) {
        this.naturalRepairFatigue = Mth.clamp(naturalRepairFatigue, 0, 100);
        CyberwareDurabilityData.setRepairFatigue(item, this.naturalRepairFatigue);
    }

    public void addNaturalRepairFatigue(int amount) {
        if (amount <= 0) return;

        setNaturalRepairFatigue(naturalRepairFatigue + amount);
    }

    public void reduceNaturalRepairFatigue(int amount) {
        if (amount <= 0) return;

        setNaturalRepairFatigue(naturalRepairFatigue - amount);
    }

    public void clearNaturalRepairFatigue() {
        naturalRepairFatigue = 0;
    }

    public long getLastPassiveDurabilityTick() {
        return Math.max(0L, lastPassiveDurabilityTick);
    }

    public void setLastPassiveDurabilityTick(long lastPassiveDurabilityTick) {
        this.lastPassiveDurabilityTick = Math.max(0L, lastPassiveDurabilityTick);
    }

    public long getEnergyReceivedSinceDurabilityDamage() {
        return Math.max(0L, energyReceivedSinceDurabilityDamage);
    }

    public void setEnergyReceivedSinceDurabilityDamage(long value) {
        energyReceivedSinceDurabilityDamage = Math.max(0L, value);
    }

    public void addEnergyReceivedSinceDurabilityDamage(long value) {
        if (value <= 0L) return;

        energyReceivedSinceDurabilityDamage = Math.max(0L, energyReceivedSinceDurabilityDamage + value);
    }

    public long getEnergyExtractedSinceDurabilityDamage() {
        return Math.max(0L, energyExtractedSinceDurabilityDamage);
    }

    public void setEnergyExtractedSinceDurabilityDamage(long value) {
        energyExtractedSinceDurabilityDamage = Math.max(0L, value);
    }

    public void addEnergyExtractedSinceDurabilityDamage(long value) {
        if (value <= 0L) return;

        energyExtractedSinceDurabilityDamage = Math.max(0L, energyExtractedSinceDurabilityDamage + value);
    }

    private void initializeDurability() {
        if (durability >= 0) return;

        durability = CyberwareDurabilityData.getDurability(item, slot);
        naturalRepairFatigue = CyberwareDurabilityData.getRepairFatigue(item);
    }

    public CompoundTag save(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();

        if (item != null && !item.isEmpty()) {
            tag.put("Item", item.save(provider));
        }

        if (slot != null) {
            tag.putString("Slot", slot.name());
            tag.putInt("Index", index);
        }

        tag.putInt("Humanity", humanityCost);
        tag.putBoolean("Powered", powered);
        tag.putInt("Durability", getDurability());
        tag.putInt("NaturalRepairFatigue", naturalRepairFatigue);
        tag.putLong("LastPassiveDurabilityTick", lastPassiveDurabilityTick);
        tag.putLong("EnergyReceivedSinceDurabilityDamage", energyReceivedSinceDurabilityDamage);
        tag.putLong("EnergyExtractedSinceDurabilityDamage", energyExtractedSinceDurabilityDamage);

        return tag;
    }

    public static InstalledCyberware load(CompoundTag tag, HolderLookup.Provider provider) {
        InstalledCyberware installed = new InstalledCyberware();

        if (tag.contains("Item", Tag.TAG_COMPOUND)) {
            installed.item = ItemStack.parse(provider, tag.getCompound("Item")).orElse(ItemStack.EMPTY);
        } else {
            installed.item = ItemStack.EMPTY;
        }

        if (tag.contains("Slot", Tag.TAG_STRING)) {
            try {
                installed.slot = CyberwareSlot.valueOf(tag.getString("Slot"));
                installed.index = tag.getInt("Index");
            } catch (IllegalArgumentException ignored) {
                installed.slot = null;
                installed.index = -1;
            }
        } else {
            installed.slot = null;
            installed.index = -1;
        }

        installed.humanityCost = tag.getInt("Humanity");
        installed.powered = !tag.contains("Powered", Tag.TAG_BYTE) || tag.getBoolean("Powered");
        installed.durability = tag.contains("Durability", Tag.TAG_INT) ? tag.getInt("Durability") : -1;
        installed.naturalRepairFatigue = tag.contains("NaturalRepairFatigue", Tag.TAG_INT) ? Mth.clamp(tag.getInt("NaturalRepairFatigue"), 0, 100) : 0;
        installed.lastPassiveDurabilityTick = tag.contains("LastPassiveDurabilityTick", Tag.TAG_LONG) ? Math.max(0L, tag.getLong("LastPassiveDurabilityTick")) : 0L;
        installed.energyReceivedSinceDurabilityDamage = tag.contains("EnergyReceivedSinceDurabilityDamage", Tag.TAG_LONG) ? Math.max(0L, tag.getLong("EnergyReceivedSinceDurabilityDamage")) : 0L;
        installed.energyExtractedSinceDurabilityDamage = tag.contains("EnergyExtractedSinceDurabilityDamage", Tag.TAG_LONG) ? Math.max(0L, tag.getLong("EnergyExtractedSinceDurabilityDamage")) : 0L;

        installed.initializeDurability();
        installed.setDurability(installed.durability);

        return installed;
    }
}
package com.perigrine3.createcybernetics.item.generic;

import com.perigrine3.createcybernetics.util.ModTags;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

public class InfologDataShardItem extends DataShardItem {

    private final String defaultTitleTranslationKey;
    private final List<InfologGuideSection> defaultSections;
    private final boolean permanentlyLocked;

    public InfologDataShardItem(Properties props) {
        this(props, "", "", false);
    }

    public InfologDataShardItem(
            Properties props,
            String defaultTitleTranslationKey,
            String defaultTextTranslationKey,
            boolean permanentlyLocked
    ) {
        this(
                props,
                defaultTitleTranslationKey,
                permanentlyLocked,
                InfologGuideSection.always(defaultTextTranslationKey)
        );
    }

    public InfologDataShardItem(
            Properties props,
            String defaultTitleTranslationKey,
            boolean permanentlyLocked,
            InfologGuideSection... defaultSections
    ) {
        super(props);

        this.defaultTitleTranslationKey = defaultTitleTranslationKey == null ? "" : defaultTitleTranslationKey;
        this.defaultSections = List.of(defaultSections);
        this.permanentlyLocked = permanentlyLocked;
    }

    public String getDefaultTitle() {
        if (defaultTitleTranslationKey.isBlank()) {
            return "";
        }

        return Component.translatable(defaultTitleTranslationKey).getString();
    }

    public String getDefaultText() {
        List<String> sections = new ArrayList<>();

        for (InfologGuideSection section : defaultSections) {
            if (!section.isEnabled()) continue;
            if (section.translationKey() == null || section.translationKey().isBlank()) continue;

            String sectionText = Component.translatable(section.translationKey()).getString();
            if (!sectionText.isBlank()) {
                sections.add(sectionText);
            }
        }

        return String.join("\n\n", sections);
    }

    public boolean isPermanentlyLocked() {
        return permanentlyLocked;
    }

    @Override
    public Component getName(ItemStack stack) {
        String title = InfologTextData.getTitle(stack);

        if (!title.isBlank()) {
            return Component.literal(title);
        }

        return super.getName(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        if (InfologTextData.isLocked(stack)) {
            tooltip.add(Component.translatable("item.createcybernetics.data_shard_infolog.saved")
                    .withStyle(ChatFormatting.GREEN));
        } else {
            tooltip.add(Component.translatable("item.createcybernetics.data_shard_infolog.editable")
                    .withStyle(ChatFormatting.GRAY));
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (stack.isEmpty() || !stack.is(ModTags.Items.DATA_SHARDS)) {
            return InteractionResultHolder.pass(stack);
        }

        return InteractionResultHolder.pass(stack);
    }

    public boolean isDyeable(ItemStack stack) {
        return true;
    }
}
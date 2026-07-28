package net.zodiuss.luckyblock.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.zodiuss.luckyblock.LuckyBlock;
import net.zodiuss.luckyblock.block.custom.LuckyBlockBlock;
import net.zodiuss.luckyblock.component.CustomDropData;
import net.zodiuss.luckyblock.component.ModComponents;
import org.jspecify.annotations.NonNull;

import java.util.function.Consumer;

public class LuckyBlockItem extends BlockItem {

    public LuckyBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    protected BlockState getPlacementState(BlockPlaceContext context) {
        BlockState state = super.getPlacementState(context);
        if (state != null && state.hasProperty(LuckyBlockBlock.LUCK)) {
            int luck = context.getItemInHand().getOrDefault(ModComponents.LUCK, 0);
            state = state.setValue(LuckyBlockBlock.LUCK, LuckyBlockBlock.encodeLuck(luck));
        }

        return state;
    }


    @Override
    public void appendHoverText(ItemStack stack, @NonNull TooltipContext context, @NonNull TooltipDisplay displayComponent, @NonNull Consumer<Component> textConsumer, @NonNull TooltipFlag type) {
        textConsumer.accept(formatLuck(stack.getOrDefault(ModComponents.LUCK, 0)));

        CustomDropData customDrop = stack.getOrDefault(ModComponents.CUSTOM_DROP, CustomDropData.EMPTY);
        if (customDrop.isPresent()) {
            textConsumer.accept(Component.translatable("text." + LuckyBlock.MOD_ID + ".custom_drop").withStyle(ChatFormatting.GOLD));
            customDrop.dropId().ifPresent(dropId -> textConsumer.accept(
                    Component.literal(dropId).withStyle(ChatFormatting.GRAY)
            ));
        }
    }

    public static Component formatLuck(int luck) {
        String text = String.valueOf(luck);

        ChatFormatting color = ChatFormatting.GRAY;

        if (luck > 0) {
            color = ChatFormatting.GREEN;
            text = "+" + text;
        } else if (luck < 0) {
            color = ChatFormatting.RED;
        }

        return Component.translatable("text." + LuckyBlock.MOD_ID + ".luck").append(": ").withStyle(ChatFormatting.GRAY).append(Component.literal(text).withStyle(color));
    }
}

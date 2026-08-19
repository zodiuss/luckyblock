package net.zodiuss.luckyblock.client.addon;

import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackCompatibility;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.repository.RepositorySource;
import net.minecraft.world.flag.FeatureFlagSet;
import net.zodiuss.luckyblock.addon.AddonRegistry;
import net.zodiuss.luckyblock.addon.LuckyAddon;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public final class AddonRepositorySource implements RepositorySource {
    @Override
    public void loadPacks(Consumer<Pack> consumer) {
        for (LuckyAddon addon : AddonRegistry.getAddons()) {
            PackLocationInfo location = new PackLocationInfo(
                    addon.packId(),
                    Component.literal(addon.config().name()),
                    PackSource.BUILT_IN,
                    Optional.empty()
            );

            Pack.ResourcesSupplier resourcesSupplier = new Pack.ResourcesSupplier() {
                @Override
                public PackResources openPrimary(PackLocationInfo locationInfo) {
                    return new AddonPackResources(locationInfo, addon);
                }

                @Override
                public PackResources openFull(PackLocationInfo locationInfo, Pack.Metadata metadata) {
                    return openPrimary(locationInfo);
                }
            };

            Pack.Metadata metadata = new Pack.Metadata(
                    Component.literal(addon.config().name()),
                    PackCompatibility.COMPATIBLE,
                    FeatureFlagSet.of(),
                    List.of()
            );

            consumer.accept(new Pack(
                    location,
                    resourcesSupplier,
                    metadata,
                    new PackSelectionConfig(true, Pack.Position.TOP, false)
            ));
        }
    }
}

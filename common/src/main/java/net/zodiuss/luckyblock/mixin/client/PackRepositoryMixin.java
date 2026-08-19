package net.zodiuss.luckyblock.mixin.client;

import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.RepositorySource;
import net.zodiuss.luckyblock.client.addon.AddonRepositorySource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.Arrays;

@Mixin(PackRepository.class)
public class PackRepositoryMixin {
    @ModifyVariable(method = "<init>", at = @At("HEAD"), argsOnly = true)
    private static RepositorySource[] lucky$appendAddonSource(RepositorySource[] sources) {
        RepositorySource[] extended = Arrays.copyOf(sources, sources.length + 1);
        extended[sources.length] = new AddonRepositorySource();
        return extended;
    }
}

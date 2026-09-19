package com.cappleapple.combattraces.mixin;

import java.util.*;
import net.neoforged.fml.loading.FMLLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.*;

public final class CombatTracesMixinPlugin implements IMixinConfigPlugin {
  public void onLoad(String pkg) {}

  public String getRefMapperConfig() {
    return null;
  }

  public boolean shouldApplyMixin(String target, String mixin) {
    return !mixin.contains(".bettercombat.")
        || FMLLoader.getLoadingModList().getMods().stream()
            .anyMatch(m -> m.getModId().equals("bettercombat"));
  }

  public void acceptTargets(Set<String> mine, Set<String> others) {}

  public List<String> getMixins() {
    // Validation guards are absent from the release JAR and never apply to production clients.
    if (!FMLLoader.isProduction()
        && FMLLoader.getDist() == net.neoforged.api.distmarker.Dist.CLIENT
        && Boolean.getBoolean("combattraces.developmentSafeClient")
        && getClass()
                .getClassLoader()
                .getResource(
                    "com/cappleapple/combattraces/mixin/validation/DevelopmentMouseMixin.class")
            != null) {
      return List.of(
          "validation.DevelopmentMouseMixin",
          "validation.DevelopmentCursorMixin",
          "validation.DevelopmentSoundMixin");
    }
    return null;
  }

  public void preApply(String target, ClassNode node, String mixin, IMixinInfo info) {}

  public void postApply(String target, ClassNode node, String mixin, IMixinInfo info) {}
}

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
    return null;
  }

  public void preApply(String target, ClassNode node, String mixin, IMixinInfo info) {}

  public void postApply(String target, ClassNode node, String mixin, IMixinInfo info) {}
}

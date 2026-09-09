package com.cappleapple.combattraces.client.render;

import com.mojang.blaze3d.vertex.*;
import java.util.*;
import net.minecraft.client.renderer.*;
import net.minecraft.resources.ResourceLocation;

public final class VfxRenderTypes extends RenderType {
  private record Key(ResourceLocation texture, boolean additive, boolean priority) {}

  private static final Map<Key, RenderType> CACHE = new HashMap<>();

  private VfxRenderTypes(
      String n,
      VertexFormat f,
      VertexFormat.Mode m,
      int size,
      boolean crumbling,
      boolean sort,
      Runnable setup,
      Runnable clear) {
    super(n, f, m, size, crumbling, sort, setup, clear);
  }

  public static RenderType get(ResourceLocation texture, boolean additive) {
    return get(texture, additive, false);
  }

  public static RenderType impact(ResourceLocation texture, boolean additive) {
    return get(
        texture,
        additive,
        com.cappleapple.combattraces.config.ClientConfig.IMPACT_DEPTH_PRIORITY.get());
  }

  private static RenderType get(ResourceLocation texture, boolean additive, boolean priority) {
    return CACHE.computeIfAbsent(
        new Key(texture, additive, priority),
        k ->
            create(
                "combattraces",
                DefaultVertexFormat.POSITION_TEX_COLOR,
                VertexFormat.Mode.QUADS,
                8192,
                false,
                !additive,
                CompositeState.builder()
                    .setShaderState(new ShaderStateShard(GameRenderer::getPositionTexColorShader))
                    .setTextureState(new TextureStateShard(texture, false, false))
                    .setTransparencyState(
                        additive ? LIGHTNING_TRANSPARENCY : TRANSLUCENT_TRANSPARENCY)
                    .setCullState(NO_CULL)
                    .setLightmapState(NO_LIGHTMAP)
                    .setOverlayState(NO_OVERLAY)
                    .setWriteMaskState(COLOR_WRITE)
                    .setDepthTestState(priority ? NO_DEPTH_TEST : LEQUAL_DEPTH_TEST)
                    .createCompositeState(false)));
  }

  public static void clearCache() {
    CACHE.clear();
  }
}

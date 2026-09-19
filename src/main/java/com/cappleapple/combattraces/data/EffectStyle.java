package com.cappleapple.combattraces.data;

import net.minecraft.resources.ResourceLocation;

public record EffectStyle(
    ResourceLocation texture,
    boolean additive,
    double lifetime,
    float width,
    float opacity,
    float scroll,
    String fade,
    float scale,
    float cameraBias,
    int color,
    int frames,
    float fps,
    ResourceLocation particle,
    float particleRate,
    boolean swept) {
  /** Existing API styles remain textured unless they opt into generated geometry. */
  public EffectStyle(
      ResourceLocation texture,
      boolean additive,
      double lifetime,
      float width,
      float opacity,
      float scroll,
      String fade,
      float scale,
      float cameraBias,
      int color,
      int frames,
      float fps,
      ResourceLocation particle,
      float particleRate) {
    this(
        texture,
        additive,
        lifetime,
        width,
        opacity,
        scroll,
        fade,
        scale,
        cameraBias,
        color,
        frames,
        fps,
        particle,
        particleRate,
        false);
  }

  public static EffectStyle trail() {
    return new EffectStyle(
        ResourceLocation.fromNamespaceAndPath("combattraces", "textures/vfx/trails/slash.png"),
        false,
        0.30,
        1,
        0.7f,
        0,
        "ease_out",
        1,
        0.25f,
        0xffffff,
        1,
        20,
        null,
        0,
        true);
  }

  public EffectStyle tinted(int tint) {
    return new EffectStyle(
        texture,
        additive,
        lifetime,
        width,
        opacity,
        scroll,
        fade,
        scale,
        cameraBias,
        tint,
        frames,
        fps,
        particle,
        particleRate,
        swept);
  }

  public float alpha(double age) {
    float t = (float) Math.clamp(1 - age / Math.max(0.001, lifetime), 0, 1);
    return opacity
        * switch (fade) {
          case "linear" -> t;
          case "smoothstep" -> t * t * (3 - 2 * t);
          default -> t * t;
        };
  }
}

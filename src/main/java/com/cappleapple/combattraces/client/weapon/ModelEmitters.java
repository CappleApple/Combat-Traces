package com.cappleapple.combattraces.client.weapon;

import com.cappleapple.combattraces.api.*;
import com.cappleapple.combattraces.data.JsonFields;
import com.cappleapple.combattraces.motion.EmitterAnalysis;
import com.google.gson.JsonParser;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public final class ModelEmitters {
  private record Key(net.minecraft.world.item.Item item, BakedModel model, WeaponClass family) {}

  private static final Map<Key, Optional<EmitterAnalysis.Shape>> GEOMETRY = new HashMap<>();
  private static final Map<ResourceLocation, List<TrailEmitter>> METADATA = new HashMap<>();

  public static void clear() {
    GEOMETRY.clear();
    METADATA.clear();
  }

  public static List<TrailEmitter> metadata(ItemStack stack, BakedModel model) {
    if (model instanceof TrailEmitterModel emitterModel)
      return emitterModel.combatTracesEmitters().stream().limit(32).toList();
    var id = BuiltInRegistries.ITEM.getKey(stack.getItem());
    return METADATA.computeIfAbsent(
        id,
        key -> {
          var path =
              ResourceLocation.fromNamespaceAndPath(
                  id.getNamespace(), "models/item/" + id.getPath() + ".json");
          var resource = Minecraft.getInstance().getResourceManager().getResource(path);
          if (resource.isEmpty()) return List.of();
          try (var reader = resource.get().openAsReader()) {
            var json = JsonParser.parseReader(reader).getAsJsonObject();
            return json.has("combat_traces")
                ? JsonFields.emitters(json.getAsJsonObject("combat_traces"))
                : List.of();
          } catch (Exception e) {
            return List.of();
          }
        });
  }

  public static Optional<EmitterAnalysis.Shape> analyze(ItemStack stack, BakedModel model) {
    return analyze(stack, model, WeaponClass.SLASH);
  }

  public static Optional<EmitterAnalysis.Shape> analyze(
      ItemStack stack, BakedModel model, WeaponClass family) {
    if (GEOMETRY.size() > 2048) GEOMETRY.clear();
    return GEOMETRY.computeIfAbsent(
        new Key(stack.getItem(), model, family),
        key -> {
          if (model.isCustomRenderer()) return Optional.empty();
          try {
            var quads = new ArrayList<net.minecraft.client.renderer.block.model.BakedQuad>();
            var random = RandomSource.create(42);
            for (var pass : model.getRenderPasses(stack, true)) {
              for (int face = -1; face < 6; face++) {
                random.setSeed(42);
                for (var quad :
                    pass.getQuads(null, face < 0 ? null : Direction.values()[face], random)) {
                  if (quads.size() < 512) quads.add(quad);
                }
              }
            }
            // Generated item front/back quads span the entire texture, including
            // transparent corners.
            // Their extruded alpha-silhouette edges are the actual shape to analyze.
            var edges =
                !model.isGui3d()
                    ? quads.stream()
                        .filter(q -> q.getDirection().getAxis() != Direction.Axis.Z)
                        .toList()
                    : quads;
            if (edges.isEmpty()) edges = quads;
            var outlines = new ArrayList<Vec3[]>();
            var vertices = new LinkedHashSet<Vec3>();
            for (var quad : edges) {
              int[] data = quad.getVertices();
              int stride = data.length / 4;
              var points = new Vec3[4];
              for (int v = 0; v < 4; v++) {
                int i = v * stride;
                points[v] =
                    new Vec3(
                        Float.intBitsToFloat(data[i]) - .5,
                        Float.intBitsToFloat(data[i + 1]) - .5,
                        Float.intBitsToFloat(data[i + 2]) - .5);
                vertices.add(points[v]);
              }
              outlines.add(points);
            }
            // Include long cuboid edges in the width profile, not just their end
            // corners.
            outer:
            for (var points : outlines) {
              for (int edge = 0; edge < 4; edge++) {
                Vec3 a = points[edge], b = points[(edge + 1) % 4];
                int steps = Math.clamp((int) Math.ceil(a.distanceTo(b) / .025), 1, 64);
                for (int i = 1; i < steps; i++) {
                  if (vertices.size() >= 8192) break outer;
                  vertices.add(a.lerp(b, (double) i / steps));
                }
              }
            }
            return EmitterAnalysis.analyze(new ArrayList<>(vertices), family);
          } catch (RuntimeException e) {
            return Optional.empty();
          }
        });
  }
}

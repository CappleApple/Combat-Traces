package com.cappleapple.combattraces.validation;

import com.cappleapple.combattraces.client.render.SweptTrailRenderer;
import com.cappleapple.combattraces.client.trail.TrailInstance;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.phys.Vec3;

/** Collects the production renderer's mesh without a GPU; excluded from the release JAR. */
public final class TrailGeometryValidation {
  private TrailGeometryValidation() {}

  public record Mesh(List<Vec3> positions, List<Integer> colors) {}

  public static List<Vec3> vertices(TrailInstance trail, Vec3 camera, float width) {
    return mesh(trail, camera, width).positions();
  }

  public static Mesh mesh(TrailInstance trail, Vec3 camera, float width) {
    var out = new Capture();
    double now = trail.history.get(trail.history.size() - 1).time();
    SweptTrailRenderer.renderGeometry(out, trail, camera, now, width, 1, 1);
    return new Mesh(List.copyOf(out.positions), List.copyOf(out.colors));
  }

  /**
   * Every blade strip must start/end on its actual captured tips and remain within its blade spans.
   */
  public static boolean followsBlade(TrailInstance trail, Vec3 camera) {
    var positions = vertices(trail, camera, 1);
    if (positions.size() != 12 * (trail.history.size() - 1)) return false;
    for (int i = 1; i < trail.history.size(); i++) {
      var a = trail.history.get(i - 1);
      var b = trail.history.get(i);
      int start = (i - 1) * 12;
      if (positions.get(start).add(camera).distanceTo(a.tip()) > 1e-5
          || positions.get(start + 3).add(camera).distanceTo(b.tip()) > 1e-5) return false;
      for (int v = 0; v < 12; v++) {
        var s = v % 4 < 2 ? a : b;
        Vec3 point = positions.get(start + v).add(camera);
        Vec3 axis = s.tip().subtract(s.origin());
        double t = point.subtract(s.origin()).dot(axis) / axis.lengthSqr();
        if (t < -1e-5 || t > 1.00001 || s.origin().add(axis.scale(t)).distanceTo(point) > 1e-5)
          return false;
      }
    }
    return true;
  }

  private static final class Capture implements VertexConsumer {
    final List<Vec3> positions = new ArrayList<>();
    final List<Integer> colors = new ArrayList<>();

    @Override
    public VertexConsumer addVertex(float x, float y, float z) {
      positions.add(new Vec3(x, y, z));
      return this;
    }

    @Override
    public VertexConsumer setColor(int r, int g, int b, int a) {
      colors.add(r << 16 | g << 8 | b);
      return this;
    }

    @Override
    public VertexConsumer setUv(float u, float v) {
      return this;
    }

    @Override
    public VertexConsumer setUv1(int u, int v) {
      return this;
    }

    @Override
    public VertexConsumer setUv2(int u, int v) {
      return this;
    }

    @Override
    public VertexConsumer setNormal(float x, float y, float z) {
      return this;
    }
  }
}

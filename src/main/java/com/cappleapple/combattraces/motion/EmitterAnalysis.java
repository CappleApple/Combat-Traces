package com.cappleapple.combattraces.motion;

import com.cappleapple.combattraces.api.*;
import java.util.*;
import net.minecraft.world.phys.Vec3;

/** Cached, model-space silhouette analysis; animation poses are never approximated here. */
public final class EmitterAnalysis {
  public record Shape(List<TrailEmitter> emitters, double length, double aspect) {
    public Shape {
      emitters = List.copyOf(emitters);
    }

    public Shape(TrailEmitter emitter, double length, double aspect) {
      this(List.of(emitter), length, aspect);
    }

    /** The first emitter, retained for integrations that only inspect the main blade. */
    public TrailEmitter emitter() {
      return emitters.getFirst();
    }
  }

  private static final int SECTIONS = 48;

  private EmitterAnalysis() {}

  public static Optional<Shape> analyze(List<Vec3> vertices) {
    return analyze(vertices, WeaponClass.SLASH);
  }

  public static Optional<Shape> analyze(List<Vec3> vertices, WeaponClass family) {
    var points = vertices.stream().filter(TrailHistory::finite).distinct().limit(8192).toList();
    if (points.size() < 4) return Optional.empty();
    Vec3 center = average(points);
    Vec3 axis = principal(points, center);
    if (axis.lengthSqr() < .5) return Optional.empty();
    if (axis.y < -.01 || (Math.abs(axis.y) < .01 && axis.x < 0)) axis = axis.scale(-1);
    double low = Double.POSITIVE_INFINITY, high = Double.NEGATIVE_INFINITY;
    for (var p : points) {
      double d = p.subtract(center).dot(axis);
      low = Math.min(low, d);
      high = Math.max(high, d);
    }
    double length = high - low;
    if (length < .15 || length > 16) return Optional.empty();
    // A stable orthogonal frame works for extruded sprites and genuinely 3-D heads.
    Vec3 side =
        axis.cross(Math.abs(axis.z) < .8 ? new Vec3(0, 0, 1) : new Vec3(0, 1, 0)).normalize();
    Vec3 depth = axis.cross(side).normalize();
    double[] widths = new double[SECTIONS];
    var bins = new ArrayList<List<Vec3>>();
    for (int i = 0; i < SECTIONS; i++) bins.add(new ArrayList<>());
    for (var p : points) {
      int i =
          Math.clamp(
              (int) ((p.subtract(center).dot(axis) - low) / length * SECTIONS), 0, SECTIONS - 1);
      bins.get(i).add(p);
    }
    for (int i = 0; i < SECTIONS; i++) {
      var section = bins.get(i);
      if (!section.isEmpty()) widths[i] = Math.max(span(section, side), span(section, depth));
    }
    for (int i = 1; i < SECTIONS - 1; i++) {
      if (widths[i] != 0) continue;
      int next = i + 1;
      while (next < SECTIONS && widths[next] == 0) next++;
      if (next < SECTIONS) {
        double from = widths[i - 1], to = widths[next];
        for (int j = i; j < next; j++)
          widths[j] = from + (to - from) * (j - i + 1d) / (next - i + 1d);
        i = next;
      }
    }
    double maxWidth = Arrays.stream(widths).max().orElse(.02);
    if (family == WeaponClass.BLUNT) {
      // A hammer/mace has a sustained distal bulge, separated from its thin shaft.
      double shaft = median(widths, 5, 24);
      double distal = Arrays.stream(widths, 27, SECTIONS).max().orElse(0);
      if (distal > Math.max(shaft * 1.65, length * .10)) {
        int start = 26;
        while (start < SECTIONS - 2 && widths[start] < Math.max(shaft * 1.65, distal * .48))
          start++;
        double headStart = low + length * start / SECTIONS;
        var head = new ArrayList<Vec3>();
        for (var p : points) if (p.subtract(center).dot(axis) >= headStart) head.add(p);
        if (head.size() >= 4) {
          Vec3 headCenter = average(head);
          Vec3 headAxis = principal(head, headCenter);
          double a = Double.POSITIVE_INFINITY, b = Double.NEGATIVE_INFINITY;
          for (var p : head) {
            double d = p.subtract(headCenter).dot(headAxis);
            a = Math.min(a, d);
            b = Math.max(b, d);
          }
          if (b - a > .05)
            return Optional.of(
                new Shape(
                    new TrailEmitter(
                        headCenter.add(headAxis.scale(a)), headCenter.add(headAxis.scale(b))),
                    length,
                    length / Math.max(.02, maxWidth)));
        }
      }
      return Optional.of(
          new Shape(
              new TrailEmitter(
                  center.add(axis.scale(low + length * .75)), center.add(axis.scale(high))),
              length,
              length / Math.max(.02, maxWidth)));
    }
    // Detect a crossguard from localized widening; keep the whole blade above it.
    double bladeWidth = median(widths, 28, 42);
    int guard = -1;
    for (int i = 5; i < 29; i++)
      if (widths[i] > Math.max(bladeWidth * 1.5, length * .08)
          && (guard < 0 || widths[i] > widths[guard])) guard = i;
    double start = .18;
    if (guard >= 0) {
      int end = guard;
      while (end < 32 && widths[end] > Math.max(bladeWidth * 1.25, widths[guard] * .60)) end++;
      start = (double) end / SECTIONS;
    }
    // Fit the blade itself so an asymmetric guard cannot pull the axis off the blade.
    double bladeStart = low + length * start;
    var blade = new ArrayList<Vec3>();
    for (var p : points) if (p.subtract(center).dot(axis) >= bladeStart) blade.add(p);
    if (blade.size() < 4)
      return Optional.of(
          new Shape(
              new TrailEmitter(center.add(axis.scale(bladeStart)), center.add(axis.scale(high))),
              length,
              length / Math.max(.02, maxWidth)));
    Vec3 bladeCenter = average(blade);
    Vec3 bladeAxis = principal(blade, bladeCenter);
    if (bladeAxis.dot(axis) < 0) bladeAxis = bladeAxis.scale(-1);
    double tip = Double.NEGATIVE_INFINITY;
    for (var p : blade) tip = Math.max(tip, p.subtract(bladeCenter).dot(bladeAxis));
    double base =
        (bladeStart - bladeCenter.subtract(center).dot(axis)) / Math.max(.2, bladeAxis.dot(axis));
    return Optional.of(
        new Shape(
            new TrailEmitter(
                bladeCenter.add(bladeAxis.scale(base)), bladeCenter.add(bladeAxis.scale(tip))),
            length,
            length / Math.max(.02, maxWidth)));
  }

  public static Optional<Shape> analyze(
      List<Vec3> vertices, WeaponClass family, WeaponTopology topology) {
    if (topology == WeaponTopology.SINGLE) return analyze(vertices, family);
    return MultiEmitterGeometry.analyze(vertices, family, topology);
  }

  private static double median(double[] values, int from, int to) {
    double[] copy = Arrays.copyOfRange(values, from, to);
    Arrays.sort(copy);
    return copy[copy.length / 2];
  }

  private static double span(List<Vec3> points, Vec3 axis) {
    double low = Double.POSITIVE_INFINITY, high = Double.NEGATIVE_INFINITY;
    for (var p : points) {
      double v = p.dot(axis);
      low = Math.min(low, v);
      high = Math.max(high, v);
    }
    return high - low;
  }

  private static Vec3 average(List<Vec3> points) {
    Vec3 center = Vec3.ZERO;
    for (var p : points) center = center.add(p);
    return center.scale(1d / points.size());
  }

  private static Vec3 principal(List<Vec3> points, Vec3 center) {
    double[][] c = new double[3][3];
    for (var p : points) {
      var d = p.subtract(center);
      double[] v = {d.x, d.y, d.z};
      for (int i = 0; i < 3; i++) for (int j = 0; j < 3; j++) c[i][j] += v[i] * v[j];
    }
    int major = c[1][1] > c[0][0] ? 1 : 0;
    if (c[2][2] > c[major][major]) major = 2;
    Vec3 axis = major == 0 ? new Vec3(1, 0, 0) : major == 1 ? new Vec3(0, 1, 0) : new Vec3(0, 0, 1);
    for (int i = 0; i < 32; i++) {
      var next =
          new Vec3(
              c[0][0] * axis.x + c[0][1] * axis.y + c[0][2] * axis.z,
              c[1][0] * axis.x + c[1][1] * axis.y + c[1][2] * axis.z,
              c[2][0] * axis.x + c[2][1] * axis.y + c[2][2] * axis.z);
      if (next.lengthSqr() < 1e-12) return Vec3.ZERO;
      axis = next.normalize();
    }
    return axis;
  }
}

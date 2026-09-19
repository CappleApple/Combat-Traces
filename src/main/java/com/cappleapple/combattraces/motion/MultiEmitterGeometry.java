package com.cappleapple.combattraces.motion;

import com.cappleapple.combattraces.api.TrailEmitter;
import com.cappleapple.combattraces.api.WeaponClass;
import java.util.*;
import net.minecraft.world.phys.Vec3;

/** Bounded model-space fitting for opposite striking ends and a circular cutting rim. */
public final class MultiEmitterGeometry {
  public static final int RIM_SEGMENTS = 8;

  private record PlanePoint(double x, double y) {}

  private record Frame(Vec3 center, Vec3 axis, Vec3 side, Vec3 depth, double low, double high) {
    double length() {
      return high - low;
    }
  }

  private MultiEmitterGeometry() {}

  public static Optional<EmitterAnalysis.Shape> analyze(
      List<Vec3> vertices, WeaponClass family, WeaponTopology topology) {
    var points = vertices.stream().filter(TrailHistory::finite).distinct().limit(8192).toList();
    if (points.size() < 4) return Optional.empty();
    Frame frame = frame(points);
    if (frame == null || frame.length() < .15 || frame.length() > 16) return Optional.empty();
    return topology == WeaponTopology.CIRCULAR
        ? rim(points, frame)
        : ends(points, frame, family, topology == WeaponTopology.DOUBLE_BLUNT);
  }

  private static Optional<EmitterAnalysis.Shape> ends(
      List<Vec3> points, Frame f, WeaponClass family, boolean blunt) {
    double middle = (f.low + f.high) * .5;
    var negative = new ArrayList<Vec3>();
    var positive = new ArrayList<Vec3>();
    for (Vec3 point : points) {
      if (point.subtract(f.center).dot(f.axis) <= middle) negative.add(point);
      else positive.add(point);
    }
    if (negative.size() < 4 || positive.size() < 4) return Optional.empty();
    var emitters = new ArrayList<TrailEmitter>();
    if (blunt) {
      emitters.add(cap(positive, f, true));
      emitters.add(cap(negative, f, false));
    } else {
      // Mirroring the lower half lets the ordinary guard/blade fitter face away from the grip.
      var upper = EmitterAnalysis.analyze(positive, family);
      var lower = EmitterAnalysis.analyze(negative.stream().map(p -> p.scale(-1)).toList(), family);
      if (upper.isEmpty() || lower.isEmpty()) return Optional.empty();
      emitters.add(upper.get().emitter());
      var mirrored = lower.get().emitter();
      emitters.add(new TrailEmitter(mirrored.origin().scale(-1), mirrored.tip().scale(-1)));
    }
    if (emitters.stream().anyMatch(e -> !valid(e))) return Optional.empty();
    return Optional.of(new EmitterAnalysis.Shape(emitters, f.length(), aspect(points, f)));
  }

  private static TrailEmitter cap(List<Vec3> half, Frame f, boolean positive) {
    double threshold = positive ? f.high - f.length() * .12 : f.low + f.length() * .12;
    var end =
        half.stream()
            .filter(
                p ->
                    positive
                        ? p.subtract(f.center).dot(f.axis) >= threshold
                        : p.subtract(f.center).dot(f.axis) <= threshold)
            .toList();
    Vec3 center = average(end);
    Vec3 across = span(end, f.side) >= span(end, f.depth) ? f.side : f.depth;
    double low = Double.POSITIVE_INFINITY, high = Double.NEGATIVE_INFINITY;
    for (Vec3 point : end) {
      double offset = point.subtract(center).dot(across);
      low = Math.min(low, offset);
      high = Math.max(high, offset);
    }
    return new TrailEmitter(center.add(across.scale(low)), center.add(across.scale(high)));
  }

  private static Optional<EmitterAnalysis.Shape> rim(List<Vec3> points, Frame f) {
    double width = span(points, f.side), thickness = span(points, f.depth);
    // Circular hints still need a broad, approximately planar outline.
    if (width < f.length() * .45 || thickness > Math.max(width, f.length()) * .35)
      return Optional.empty();
    var projected =
        points.stream()
            .map(
                p -> {
                  var d = p.subtract(f.center);
                  return new PlanePoint(d.dot(f.axis), d.dot(f.side));
                })
            .distinct()
            .sorted(Comparator.comparingDouble(PlanePoint::x).thenComparingDouble(PlanePoint::y))
            .toList();
    var hull = hull(projected);
    if (hull.size() < 3) return Optional.empty();
    double cx = (projected.getFirst().x + projected.getLast().x) * .5;
    double cy =
        (projected.stream().mapToDouble(PlanePoint::y).min().orElseThrow()
                + projected.stream().mapToDouble(PlanePoint::y).max().orElseThrow())
            * .5;
    Vec3 center = f.center.add(f.axis.scale(cx)).add(f.side.scale(cy));
    var rim = new ArrayList<Vec3>();
    for (int i = 0; i < RIM_SEGMENTS; i++) {
      double angle = Math.PI * 2 * i / RIM_SEGMENTS;
      double dx = Math.cos(angle), dy = Math.sin(angle), radius = Double.POSITIVE_INFINITY;
      for (int j = 0; j < hull.size(); j++) {
        var a = hull.get(j);
        var b = hull.get((j + 1) % hull.size());
        double ex = b.x - a.x, ey = b.y - a.y;
        double denominator = cross(dx, dy, ex, ey);
        if (Math.abs(denominator) < 1e-9) continue;
        double t = cross(a.x - cx, a.y - cy, ex, ey) / denominator;
        double u = cross(a.x - cx, a.y - cy, dx, dy) / denominator;
        if (t > 0 && u >= -1e-8 && u <= 1 + 1e-8) radius = Math.min(radius, t);
      }
      if (!Double.isFinite(radius) || radius < .04) return Optional.empty();
      rim.add(center.add(f.axis.scale(dx * radius)).add(f.side.scale(dy * radius)));
    }
    var emitters = new ArrayList<TrailEmitter>();
    for (int i = 0; i < rim.size(); i++)
      emitters.add(new TrailEmitter(rim.get((i + 1) % rim.size()), rim.get(i)));
    return Optional.of(new EmitterAnalysis.Shape(emitters, f.length(), aspect(points, f)));
  }

  private static List<PlanePoint> hull(List<PlanePoint> points) {
    var result = new ArrayList<PlanePoint>();
    for (var p : points) {
      while (result.size() >= 2 && turn(result.get(result.size() - 2), result.getLast(), p) <= 0)
        result.removeLast();
      result.add(p);
    }
    int lower = result.size();
    for (int i = points.size() - 2; i >= 0; i--) {
      var p = points.get(i);
      while (result.size() > lower && turn(result.get(result.size() - 2), result.getLast(), p) <= 0)
        result.removeLast();
      result.add(p);
    }
    result.removeLast();
    return result;
  }

  private static double turn(PlanePoint a, PlanePoint b, PlanePoint c) {
    return cross(b.x - a.x, b.y - a.y, c.x - a.x, c.y - a.y);
  }

  private static double cross(double ax, double ay, double bx, double by) {
    return ax * by - ay * bx;
  }

  private static Frame frame(List<Vec3> points) {
    Vec3 center = average(points), axis = principal(points, center);
    if (axis.lengthSqr() < .5) return null;
    if (axis.y < -.01 || (Math.abs(axis.y) < .01 && axis.x < 0)) axis = axis.scale(-1);
    var residual = new ArrayList<Vec3>();
    for (var p : points) {
      Vec3 d = p.subtract(center);
      residual.add(d.subtract(axis.scale(d.dot(axis))));
    }
    Vec3 side = principal(residual, Vec3.ZERO);
    if (side.lengthSqr() < .5) return null;
    Vec3 depth = axis.cross(side).normalize();
    side = depth.cross(axis).normalize();
    double low = Double.POSITIVE_INFINITY, high = Double.NEGATIVE_INFINITY;
    for (Vec3 p : points) {
      double d = p.subtract(center).dot(axis);
      low = Math.min(low, d);
      high = Math.max(high, d);
    }
    return new Frame(center, axis, side, depth, low, high);
  }

  private static boolean valid(TrailEmitter emitter) {
    return TrailHistory.finite(emitter.origin())
        && TrailHistory.finite(emitter.tip())
        && emitter.origin().distanceToSqr(emitter.tip()) > 1e-5;
  }

  private static double aspect(List<Vec3> points, Frame f) {
    return f.length() / Math.max(.02, Math.max(span(points, f.side), span(points, f.depth)));
  }

  private static double span(List<Vec3> points, Vec3 axis) {
    double low = Double.POSITIVE_INFINITY, high = Double.NEGATIVE_INFINITY;
    for (var p : points) {
      double d = p.dot(axis);
      low = Math.min(low, d);
      high = Math.max(high, d);
    }
    return high - low;
  }

  private static Vec3 average(List<Vec3> points) {
    Vec3 sum = Vec3.ZERO;
    for (var point : points) sum = sum.add(point);
    return sum.scale(1d / points.size());
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

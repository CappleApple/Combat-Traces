package com.cappleapple.combattraces.motion;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

class EmitterAnalysisTest {
  @Test
  void infersDiagonalBladeFromGeometry() {
    var points = new ArrayList<Vec3>();
    for (int i = 0; i < 12; i++)
      for (int side = -1; side <= 1; side += 2)
        points.add(new Vec3(i * 0.1 + side * 0.025, i * 0.1 - side * 0.025, 0));
    var result = EmitterAnalysis.analyze(points).orElseThrow();
    var axis = result.emitter().tip().subtract(result.emitter().origin()).normalize();
    assertEquals(Math.sqrt(0.5), axis.x, 0.01);
    assertEquals(Math.sqrt(0.5), axis.y, 0.01);
    assertTrue(result.aspect() > 5);
  }

  @Test
  void negativeSlopeModelDoesNotCollapsePowerIteration() {
    var points =
        List.of(new Vec3(0, 1, 0), new Vec3(1, 0, 0), new Vec3(0.1, 1, 0), new Vec3(1, 0.1, 0));
    assertTrue(EmitterAnalysis.analyze(points).isPresent());
  }

  @Test
  void emptyAndNonfiniteModelsFallBack() {
    assertTrue(EmitterAnalysis.analyze(List.of()).isEmpty());
    assertTrue(EmitterAnalysis.analyze(List.of(new Vec3(Double.NaN, 0, 0))).isEmpty());
  }
}

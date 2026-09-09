package com.cappleapple.combattraces.motion;

import static org.junit.jupiter.api.Assertions.*;

import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.junit.jupiter.api.Test;

class PoseTransformsTest {
  @Test
  void cameraTranslationDoesNotMoveWorldEmitter() {
    var point = new Vec3(0.5, 1, 0.5);
    var first =
        PoseTransforms.toWorld(new Matrix4f().translation(2, 3, 4), point, new Vec3(10, 5, 8));
    var second =
        PoseTransforms.toWorld(new Matrix4f().translation(1, 1, 1), point, new Vec3(11, 7, 11));
    assertEquals(first, second);
  }

  @Test
  void arbitraryAnimatedRotationAndScaleDriveTip() {
    var pose = new Matrix4f().translation(2, 3, 4).rotateZ((float) Math.PI / 2).scale(2);
    var tip = PoseTransforms.toWorld(pose, new Vec3(0, 1, 0), Vec3.ZERO);
    assertEquals(0, tip.x, 1e-5);
    assertEquals(3, tip.y, 1e-5);
    assertEquals(4, tip.z, 1e-5);
  }

  @Test
  void cameraOffsetPreservesWorldBorderPrecision() {
    var tip =
        PoseTransforms.toWorld(
            new Matrix4f().translation(0.125f, 0, 0), Vec3.ZERO, new Vec3(29_999_900, 0, 0));
    assertEquals(29_999_900.125, tip.x);
  }
}

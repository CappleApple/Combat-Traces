package com.cappleapple.combattraces.motion;

import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class PoseTransforms {
  private PoseTransforms() {}

  public static Vec3 toWorld(Matrix4f modelPose, Vec3 local, Vec3 camera) {
    // World entity item poses are already camera-relative; do not invert camera rotation here.
    var p =
        modelPose.transformPosition(
            (float) local.x, (float) local.y, (float) local.z, new Vector3f());
    return camera.add(p.x, p.y, p.z);
  }
}

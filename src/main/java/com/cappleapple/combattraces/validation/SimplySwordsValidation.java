package com.cappleapple.combattraces.validation;

import com.cappleapple.combattraces.api.*;
import com.cappleapple.combattraces.client.*;
import com.cappleapple.combattraces.client.impact.*;
import com.cappleapple.combattraces.client.trail.*;
import com.cappleapple.combattraces.client.weapon.*;
import com.cappleapple.combattraces.config.ClientConfig;
import com.cappleapple.combattraces.motion.*;
import java.nio.file.*;
import java.util.*;
import net.bettercombat.client.animation.PlayerAttackAnimatable;
import net.bettercombat.logic.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Real Simply Swords resources and Better Combat attacks; entirely excluded from the release JAR.
 */
@EventBusSubscriber(modid = "combattraces", value = Dist.CLIENT)
public final class SimplySwordsValidation {
  private record Case(String weapon, int combo, WeaponClass family, String impact) {}

  private static final Case[] CASES = {
    new Case("diamond_longsword", 0, WeaponClass.SLASH, "slash"),
    new Case("diamond_longsword", 2, WeaponClass.SLASH, "pierce"),
    new Case("diamond_claymore", 0, WeaponClass.CLEAVE, "cleave"),
    new Case("diamond_greathammer", 0, WeaponClass.BLUNT, "blunt"),
    new Case("diamond_greathammer", 2, WeaponClass.BLUNT, "blunt"),
    new Case("diamond_claymore", 2, WeaponClass.CLEAVE, "cleave"),
    new Case("diamond_claymore", 1, WeaponClass.CLEAVE, "pierce")
  };
  private static boolean started, shot, gotImpact, quietWindup;
  private static double expectedHitHeight;
  private static int ticks, maxRibbonSamples, hitShotTick;
  private static volatile int targetId;
  private static long hitsBefore, particlesBefore;
  private static java.util.concurrent.CompletableFuture<Void> reload;
  private static final List<WeaponMotionTracker.Observation> observations = new ArrayList<>();
  private static final List<String> results = new ArrayList<>();
  private static final List<String> detail = new ArrayList<>();
  private static Case current;
  private static com.mojang.blaze3d.platform.NativeImage depthBaseline, depthNormal;

  @SubscribeEvent
  public static void tick(ClientTickEvent.Post event) {
    if (!Boolean.getBoolean("combattraces.validateSimplySwords")) return;
    var mc = Minecraft.getInstance();
    if (!started && mc.screen != null && mc.getOverlay() == null) {
      started = true;
      mc.options.pauseOnLostFocus = false;
      mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
      mc.options.renderDistance().set(4);
      mc.options.framerateLimit().set(120);
      mc.options.fov().set(70);
      DebugRenderer.enabled = true;
      mc.createWorldOpenFlows()
          .createFreshLevel(
              "ct-simplyswords-" + System.currentTimeMillis(),
              new LevelSettings(
                  "Simply Swords effects acceptance",
                  GameType.CREATIVE,
                  false,
                  Difficulty.PEACEFUL,
                  true,
                  new GameRules(),
                  WorldDataConfiguration.DEFAULT),
              new WorldOptions(1234L, false, false),
              registry ->
                  registry
                      .registryOrThrow(Registries.WORLD_PRESET)
                      .getHolderOrThrow(WorldPresets.FLAT)
                      .value()
                      .createWorldDimensions(),
              new TitleScreen());
      return;
    }
    if (mc.player == null || mc.level == null) return;
    ticks++;
    if (ticks == hitShotTick) screenshot("ss-" + ((ticks - 45) / 60) + "-hit.png");
    if (ticks == 25) {
      var uuid = mc.player.getUUID();
      mc.getSingleplayerServer()
          .execute(
              () -> {
                var player = mc.getSingleplayerServer().getPlayerList().getPlayer(uuid);
                player.serverLevel().setDayTime(6000);
                var target = EntityType.IRON_GOLEM.create(player.serverLevel());
                target.setNoAi(true);
                target.setNoGravity(true);
                target.moveTo(player.getX() + .85, player.getY(), player.getZ() + 2.3, 180, 0);
                player.serverLevel().addFreshEntity(target);
                targetId = target.getId();
              });
    }
    if (ticks < 45) return;
    int index = (ticks - 45) / 60, step = (ticks - 45) % 60;
    if (index >= CASES.length) {
      depthTest(mc, index - CASES.length, step);
      return;
    }
    current = CASES[index];
    if (step == 0) {
      mc.setScreen(null);
      mc.player.setYRot(-18);
      mc.player.setXRot(index == 1 ? 12 : index == 2 ? -10 : index == 4 ? 15 : 0);
      ClientConfig.SPEED_THRESHOLD.set(4d);
      quietWindup = true;
      observations.clear();
      TrailManager.clear();
      ImpactManager.clear();
      WeaponMotionTracker.clear();
      shot = false;
      gotImpact = false;
      maxRibbonSamples = 0;
      hitsBefore = ImpactController.receivedHits;
      var uuid = mc.player.getUUID();
      mc.getSingleplayerServer()
          .execute(
              () -> {
                var player = mc.getSingleplayerServer().getPlayerList().getPlayer(uuid);
                player.setItemInHand(
                    InteractionHand.MAIN_HAND,
                    new ItemStack(
                        BuiltInRegistries.ITEM.get(
                            ResourceLocation.parse("simplyswords:" + current.weapon))));
                player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
              });
    }
    if (step == 9) {
      check(
          BuiltInRegistries.ITEM
              .getKey(mc.player.getMainHandItem().getItem())
              .getPath()
              .equals(current.weapon),
          label() + " equipped actual registered item");
      ((PlayerAttackProperties) mc.player).setComboCount(current.combo);
      var attack = PlayerAttackHelper.getCurrentAttack(mc.player, current.combo);
      check(attack != null, label() + " loaded Better Combat attack");
      detail.add(
          label()
              + " animation="
              + attack.attack().animation()
              + " hitbox="
              + attack.attack().hitbox());
      ((PlayerAttackAnimatable) mc.player)
          .playAttackAnimation(
              attack.attack().animation(),
              AnimatedHand.from(false, attack.attributes().isTwoHanded()),
              32,
              (float) attack.upswingRate());
      var motion = CombatTracesApi.motion(mc.player, 0).orElseThrow();
      float pitch = mc.player.getXRot();
      boolean heightMatches = true;
      for (float aim : new float[] {-30, 0, 30}) {
        mc.player.setXRot(aim);
        var volume =
            net.bettercombat.client.collision.TargetFinder.findAttackTargetResult(
                mc.player,
                null,
                attack.attack(),
                PlayerAttackHelper.getRangeForItem(mc.player, motion.weapon())
                    * attack.attack().rangeMultiplier());
        var supplied = CombatTracesApi.motion(mc.player, 0).orElseThrow().hitbox();
        heightMatches &=
            supplied != null
                && supplied.center().distanceTo(volume.obb.center) < 1e-7
                && supplied.size().distanceTo(volume.obb.extent.scale(2)) < 1e-7
                && supplied.widthAxis().distanceTo(volume.obb.axisX) < 1e-7
                && supplied.heightAxis().distanceTo(volume.obb.axisY) < 1e-7
                && supplied.depthAxis().distanceTo(volume.obb.axisZ) < 1e-7;
      }
      mc.player.setXRot(pitch);
      expectedHitHeight = CombatTracesApi.motion(mc.player, 0).orElseThrow().hitboxCenter().y;
      check(
          heightMatches,
          label() + " dimensions, axes, and center match Better Combat at three aim pitches");
      if (current.family != WeaponClass.BLUNT) {
        var expected =
            current.impact.equals("pierce")
                ? HitboxImpact.Kind.STAB
                : current.combo == 2 ? HitboxImpact.Kind.VERTICAL : HitboxImpact.Kind.HORIZONTAL;
        check(
            HitboxImpact.kind(motion.hitbox()) == expected,
            label() + " hitbox proportions select " + expected);
        var fallback =
            HitboxImpact.resolve(
                current.family, motion.hitbox(), MotionAnalysis.Type.UNKNOWN, Vec3.ZERO);
        check(
            fallback.type() != MotionAnalysis.Type.UNKNOWN && fallback.tangent().lengthSqr() > .9,
            label() + " hitbox works without blade velocity samples");
      }
      var model =
          mc.getItemRenderer().getModel(mc.player.getMainHandItem(), mc.level, mc.player, 0);
      var resolved = WeaponResolver.resolve(mc.player, motion, model);
      detail.add("model=" + model.getClass() + " gui3d=" + model.isGui3d());
      var random = net.minecraft.util.RandomSource.create(42);
      var qp = new ArrayList<String>();
      for (var q : model.getQuads(null, null, random)) {
        int[] data = q.getVertices();
        int stride = data.length / 4;
        for (int v = 0; v < 4; v++)
          qp.add(
              q.getDirection()
                  + " "
                  + Float.intBitsToFloat(data[v * stride])
                  + " "
                  + Float.intBitsToFloat(data[v * stride + 1])
                  + " "
                  + Float.intBitsToFloat(data[v * stride + 2]));
      }
      try {
        Files.writeString(Path.of("ss-" + current.weapon + "-quads.txt"), String.join("\n", qp));
      } catch (Exception ignored) {
      }

      check(
          resolved.weaponClass() == current.family,
          label() + " correct weapon family " + resolved.weaponClass());
      var emitter = resolved.emitters().getFirst();
      detail.add(label() + " model emitter=" + emitter);
      if (current.family == WeaponClass.BLUNT) {
        check(
            emitter.origin().x < -.85
                && emitter.tip().x < -.85
                && emitter.origin().y > .65
                && emitter.tip().y > .65,
            label() + " both ribbon endpoints lie on 3D hammer head");
        check(
            emitter.origin().distanceTo(emitter.tip()) > .55
                && emitter.origin().distanceTo(emitter.tip()) < 1,
            label() + " ribbon spans head width");
      } else {
        check(
            emitter
                    .tip()
                    .distanceTo(
                        current.weapon.equals("diamond_longsword")
                            ? new Vec3(.2818, .2818, 0)
                            : new Vec3(.5, .5, 0))
                < .065,
            label() + " emitter reaches full blade tip");
        check(
            emitter.origin().x < .02
                && emitter.origin().y < .02
                && emitter.origin().distanceTo(emitter.tip()) > .45,
            label() + " emitter starts near guard");
      }
    }
    if (step > 9 && step < 48) {
      var o = WeaponMotionTracker.latest(mc.player.getId(), InteractionHand.MAIN_HAND, 0);
      if (o != null
          && (observations.isEmpty()
              || observations.getLast().sample().time() != o.sample().time())) {
        observations.add(o);
        if (o.motion().progress() < o.motion().hitProgress() - .13)
          quietWindup &= TrailManager.trails().isEmpty();
        detail.add(
            String.format(
                Locale.ROOT,
                "%s p=%.3f type=%s speed=%.2f shape=%s",
                label(),
                o.motion().progress(),
                o.state().type(),
                o.state().speed(),
                o.motion().shape()));
      }
      for (var trail : TrailManager.trails())
        maxRibbonSamples = Math.max(maxRibbonSamples, trail.history.size());
      if (!shot && step > 18 && o != null && o.state().speed() > 4 && maxRibbonSamples >= 3) {
        screenshot("ss-" + index + "-ribbon.png");
        shot = true;
      }
      for (var impact : ImpactManager.active())
        if (impact.context().target() != null && impact.context().target().getId() == targetId) {
          var ctx = impact.context();
          var family =
              HitboxImpact.family(ctx.weaponClass(), ctx.motion().hitbox(), ctx.motionType())
                  .path();
          if (!gotImpact) {
            check(family.equals(current.impact), label() + " confirmed hit selects " + family);
            check(
                Math.abs(ctx.position().y - expectedHitHeight) < .03,
                label() + " impact height matches attack hitbox center");
            var box = ctx.motion().hitbox();
            var expectedAxis =
                current.family == WeaponClass.BLUNT
                    ? o.contactTangent()
                    : switch (HitboxImpact.kind(box)) {
                      case STAB -> box.depthAxis();
                      case HORIZONTAL -> box.widthAxis();
                      case VERTICAL -> box.heightAxis();
                      case UNKNOWN -> Vec3.ZERO;
                    };
            check(
                ctx.tangent().dot(expectedAxis) > .98,
                label()
                    + (current.family == WeaponClass.BLUNT
                        ? " blunt impact preserves its motion direction"
                        : " impact follows dominant hitbox axis"));
            check(
                impact
                    .style()
                    .texture()
                    .getPath()
                    .equals("textures/vfx/impacts/" + current.impact + ".png"),
                label() + " actual sprite matches hitbox-selected effect");
            var camera =
                mc.gameRenderer.getMainCamera().getPosition().subtract(ctx.position()).normalize();
            var projected =
                ctx.tangent().subtract(camera.scale(ctx.tangent().dot(camera))).normalize();
            var basis = ImpactMath.orient(ctx.tangent(), camera, camera, 1);
            long seed =
                Double.doubleToLongBits(impact.created())
                    ^ ((long) ctx.attacker().getId() << 32)
                    ^ ctx.target().getId();
            basis = HitboxImpact.vary(basis, ctx.weaponClass(), box, seed);
            check(
                projected.dot(basis.tangent()) >= Math.cos(Math.toRadians(7)) - 1e-6,
                label() + " sprite variation stays within seven degrees");
            detail.add(
                label()
                    + " hitbox size="
                    + box.size()
                    + " kind="
                    + HitboxImpact.kind(box)
                    + " variation="
                    + HitboxImpact.variationDegrees(seed));
            detail.add(
                label()
                    + " contact direction="
                    + ctx.tangent()
                    + " hitY="
                    + ctx.position().y
                    + " expectedHitboxY="
                    + expectedHitHeight
                    + " screenDirection="
                    + projected);
            detail.add(
                label()
                    + " hit motion="
                    + ctx.motionType()
                    + " texture="
                    + impact.style().texture());
            hitShotTick = ticks + 1;
            gotImpact = true;
          }
        }
    }
    if (step == 26) {
      var uuid = mc.player.getUUID();
      mc.getSingleplayerServer()
          .execute(
              () -> {
                var player = mc.getSingleplayerServer().getPlayerList().getPlayer(uuid);
                var target = player.serverLevel().getEntity(targetId);
                if (target instanceof LivingEntity living) {
                  living.invulnerableTime = 0;
                  living.hurt(player.damageSources().playerAttack(player), 1);
                  living.setDeltaMovement(Vec3.ZERO);
                }
              });
    }
    if (step == 50) {
      check(observations.size() >= 8, label() + " samples real rendered animation");
      check(quietWindup, label() + " early windup has no ribbon");
      check(maxRibbonSamples >= 3, label() + " produces ribbon geometry");
      check(
          ImpactController.receivedHits > hitsBefore && gotImpact,
          label() + " server-confirmed impact received");
      if (current.impact.equals("pierce"))
        check(
            observations.stream()
                .anyMatch(o -> HitboxImpact.kind(o.motion().hitbox()) == HitboxImpact.Kind.STAB),
            label() + " depth-dominant volume identifies stab throughout combo");
      screenshot("ss-" + index + "-end.png");
    }
  }

  private static void depthTest(Minecraft mc, int phase, int step) {
    if (phase > 0) {
      stressParticles(mc, phase, step);
      return;
    }
    var target = mc.level.getEntity(targetId);
    if (target == null) return;
    if (step == 0) {
      DebugRenderer.enabled = false;
      mc.options.setCameraType(CameraType.FIRST_PERSON);
      mc.player.setYRot(-20);
      mc.player.setXRot(4);
      TrailManager.clear();
    }
    if (step == 10) {
      var motion =
          new CombatMotion(
              9000,
              mc.player.getMainHandItem(),
              InteractionHand.MAIN_HAND,
              ResourceLocation.parse("combattraces:depth_probe"),
              .5f,
              .5f,
              0,
              1,
              1,
              "sword",
              "");
      var camera = mc.gameRenderer.getMainCamera().getPosition();
      var center = target.getBoundingBox().getCenter();
      var away = center.subtract(camera).normalize();
      // Place the cut completely behind the opaque target. Normal depth must hide it.
      var hit = center.add(away.scale(1.0));
      var tangent = new Vec3(.9, .45, 0);
      var ctx =
          new ImpactContext(
              mc.player,
              target,
              motion,
              hit,
              tangent,
              away,
              10,
              1.5f,
              WeaponClass.SLASH,
              MotionAnalysis.Type.DIAGONAL_UP,
              List.of(),
              ResourceLocation.parse("combattraces:metal"),
              false,
              false);
      var base = ClientDefinitions.current.impact(ResourceLocation.parse("combattraces:slash"));
      var block =
          new ImpactContext(
              ctx.attacker(),
              null,
              ctx.motion(),
              ctx.position(),
              ctx.tangent(),
              ctx.normal(),
              ctx.velocity(),
              ctx.strength(),
              ctx.weaponClass(),
              ctx.motionType(),
              ctx.elements(),
              ctx.material(),
              false,
              false);
      ImpactManager.clear();
      long accents = com.cappleapple.combattraces.client.particle.AccentParticles.spawned;
      ImpactManager.add(block, VisualClock.now());
      ImpactManager.layer(block, base, VisualClock.now(), 1);
      check(
          ImpactManager.active().isEmpty()
              && accents == com.cappleapple.combattraces.client.particle.AccentParticles.spawned,
          "Block contact creates neither impact sprites nor accent particles");
      var style =
          new com.cappleapple.combattraces.data.EffectStyle(
              base.texture(), false, 3, 1, 1, 0, "linear", 1, .95f, 0xffffff, 1, 20, null, 0);
      ImpactManager.clear();
      ImpactManager.layer(ctx, style, VisualClock.now(), 1.5f);
      ClientConfig.IMPACT_DEPTH_PRIORITY.set(false);
      ClientConfig.IMPACTS.set(false);
    }
    if (step == 12) {
      depthBaseline = Screenshot.takeScreenshot(mc.getMainRenderTarget());
      ClientConfig.IMPACTS.set(true);
    }
    if (step == 14) {
      depthNormal = Screenshot.takeScreenshot(mc.getMainRenderTarget());
      screenshot("ss-depth-normal.png");
      ClientConfig.IMPACT_DEPTH_PRIORITY.set(true);
    }
    if (step == 16) {
      screenshot("ss-depth-priority.png");
      try (var priority = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
        int hidden = pixelChanges(depthBaseline, depthNormal),
            visible = pixelChanges(depthBaseline, priority);
        check(
            hidden < 10 && visible > 25,
            "Framebuffer: behind-target cut hidden at normal depth ("
                + hidden
                + " pixels), visible with priority ("
                + visible
                + " pixels)");
      }
      depthBaseline.close();
      depthNormal.close();
    }
    if (step == 25) {
      check(ClientConfig.IMPACT_DEPTH_PRIORITY.get(), "Target depth priority enabled");
      ImpactManager.clear();
    }
  }

  private static void stressParticles(Minecraft mc, int phase, int step) {
    boolean async = net.neoforged.fml.ModList.get().isLoaded("asyncparticles");
    if (phase == 1 && step == 0) {
      particlesBefore = com.cappleapple.combattraces.client.particle.AccentParticles.spawned;
      mc.options.particles().set(ParticleStatus.ALL);
      if (async) {
        try {
          var helper =
              Class.forName(
                  "neoforge.fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper");
          check(
              (boolean) helper.getMethod("isAsyncParticleTick").invoke(null),
              "AsyncParticles worker ticking is enabled");
          check(
              (boolean) helper.getMethod("isGpuParticles").invoke(null),
              "AsyncParticles GPU rendering is enabled");
        } catch (ReflectiveOperationException e) {
          throw new RuntimeException(e);
        }
      }
    }
    if (phase >= 4) {
      long amount =
          com.cappleapple.combattraces.client.particle.AccentParticles.spawned - particlesBefore;
      check(amount >= 300, "Accent stress actually spawned particles: " + amount);
      check(
          reload != null && reload.isDone() && !reload.isCompletedExceptionally(),
          "Resource reload completed during particle stress");
      check(mc.isSameThread(), "Effect producers remain on Minecraft client thread");
      finish(mc);
      return;
    }
    var types =
        new net.minecraft.core.particles.ParticleOptions[] {
          net.minecraft.core.particles.ParticleTypes.SMALL_FLAME,
          net.minecraft.core.particles.ParticleTypes.SNOWFLAKE,
          net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK,
          net.minecraft.core.particles.ParticleTypes.SPORE_BLOSSOM_AIR,
          net.minecraft.core.particles.ParticleTypes.ENCHANTED_HIT,
          net.minecraft.core.particles.ParticleTypes.END_ROD,
          net.minecraft.core.particles.ParticleTypes.SMOKE,
          net.minecraft.core.particles.ParticleTypes.CRIT,
          net.minecraft.core.particles.ParticleTypes.POOF,
          net.minecraft.core.particles.ParticleTypes.SPLASH,
          new net.minecraft.core.particles.BlockParticleOption(
              net.minecraft.core.particles.ParticleTypes.BLOCK,
              net.minecraft.world.level.block.Blocks.STONE.defaultBlockState())
        };
    for (int i = 0; i < types.length; i++)
      com.cappleapple.combattraces.client.particle.AccentParticles.queue(
          types[(i + step) % types.length],
          mc.player.position().add((i % 4 - 1.5) * .3, 1 + (i % 3) * .2, 1.5),
          new Vec3(1, 0, 0),
          new Vec3(0, 1, 0),
          12,
          false,
          0x99ccff);
    if (phase == 2 && step == 0) {
      check(
          com.cappleapple.combattraces.client.particle.AccentParticles.spawned - particlesBefore
              >= 200,
          "Accent stress spawned particles before reload");
      particlesBefore = 0;
      reload = mc.reloadResourcePacks();
    }
    if (phase == 3 && step == 20) screenshot("ss-async-particle-stress.png");
  }

  private static int pixelChanges(
      com.mojang.blaze3d.platform.NativeImage a, com.mojang.blaze3d.platform.NativeImage b) {
    int count = 0, cx = a.getWidth() / 2, cy = a.getHeight() / 2;
    for (int y = cy - 35; y < cy + 35; y++)
      for (int x = cx - 35; x < cx + 35; x++) {
        int p = a.getPixelRGBA(x, y), q = b.getPixelRGBA(x, y);
        int delta =
            Math.abs((p & 255) - (q & 255))
                + Math.abs(((p >> 8) & 255) - ((q >> 8) & 255))
                + Math.abs(((p >> 16) & 255) - ((q >> 16) & 255));
        if (delta > 35) count++;
      }
    return count;
  }

  private static String label() {
    return current.weapon + " combo " + current.combo;
  }

  private static void check(boolean value, String description) {
    if (!value) {
      try {
        Files.writeString(Path.of("simplyswords-validation-detail.txt"), String.join("\n", detail));
      } catch (Exception ignored) {
      }
      throw new IllegalStateException("COMBAT TRACES SIMPLY SWORDS FAIL: " + description);
    }
    results.add("PASS " + description);
    System.out.println("COMBAT TRACES SS PASS: " + description);
  }

  private static void screenshot(String name) {
    var mc = Minecraft.getInstance();
    Screenshot.grab(mc.gameDirectory, name, mc.getMainRenderTarget(), ignored -> {});
  }

  private static void finish(Minecraft mc) {
    try {
      Files.writeString(Path.of("simplyswords-validation.txt"), String.join("\n", results) + "\n");
      Files.writeString(
          Path.of("simplyswords-validation-detail.txt"), String.join("\n", detail) + "\n");
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    System.out.println(
        "COMBAT TRACES SIMPLY SWORDS ACCEPTANCE PASS: " + results.size() + " checks");
    mc.stop();
  }
}

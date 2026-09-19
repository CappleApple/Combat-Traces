package com.cappleapple.combattraces.validation;

import com.cappleapple.combattraces.api.*;
import com.cappleapple.combattraces.client.*;
import com.cappleapple.combattraces.client.trail.*;
import com.cappleapple.combattraces.client.weapon.*;
import com.cappleapple.combattraces.config.ClientConfig;
import com.cappleapple.combattraces.motion.*;
import java.lang.reflect.Field;
import java.nio.file.*;
import java.util.*;
import net.bettercombat.client.animation.PlayerAttackAnimatable;
import net.bettercombat.logic.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.*;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/** Real installed weapon models and combo animations, excluded from the published JAR. */
@EventBusSubscriber(modid = "combattraces", value = Dist.CLIENT)
public final class MultiWeaponValidation {
  private record Case(
      String item,
      int combo,
      boolean dual,
      int fps,
      boolean fire,
      boolean first,
      int emitterCount,
      WeaponClass family) {}

  private static final List<Case> CASES = cases();
  private static final List<String> RESULTS = new ArrayList<>(), DETAILS = new ArrayList<>();
  private static final Map<Integer, Coverage> BASE = new TreeMap<>(), FIRE = new TreeMap<>();
  private static final Map<TrailInstance, Double> RECOVERY = new IdentityHashMap<>();
  private static final Set<String> ANIMATIONS = new TreeSet<>();
  private static boolean started, playing, recoveryQuiet, insideWindow, coherent, shapeCorrect;
  private static boolean sawWindup, windupQuiet, sawRecovery, earlyShot, peakShot, endShot;
  private static int ticks, index, step, finishStep, failures, frames;
  private static long attackId;
  private static float cooldown, previousProgress, maximumFrameStep, firstFastProgress;
  private static double previousTime;
  private static SwingWindow window;
  private static InteractionHand hand;
  private static WeaponResolver.Resolved resolved;
  private static Field trailsField;

  private static final class Coverage {
    float first = Float.POSITIVE_INFINITY, last = Float.NEGATIVE_INFINITY, retained;
    int maximumSamples, renderedFrames;
    boolean finite = true;
    final Set<TrailInstance> strokes = Collections.newSetFromMap(new IdentityHashMap<>());
  }

  private static List<Case> cases() {
    var values = new ArrayList<Case>();
    for (int combo = 0; combo < 3; combo++)
      values.add(
          weapon(
              "simplyswords:diamond_twinblade",
              combo,
              false,
              120,
              false,
              false,
              2,
              WeaponClass.SLASH));
    for (int combo = 0; combo < 4; combo++)
      values.add(
          weapon(
              "simplymore:diamond_quarterstaff",
              combo,
              false,
              120,
              false,
              false,
              2,
              WeaponClass.BLUNT));
    for (int combo = 0; combo < 2; combo++)
      values.add(
          weapon(
              "simplyswords:diamond_warglaive",
              combo,
              false,
              120,
              false,
              false,
              2,
              WeaponClass.SLASH));
    for (int combo : new int[] {2, 3})
      values.add(
          weapon(
              "simplyswords:diamond_warglaive",
              combo,
              true,
              120,
              false,
              false,
              2,
              WeaponClass.SLASH));
    values.add(
        weapon("simplyswords:diamond_chakram", 0, false, 120, false, false, 8, WeaponClass.SLASH));
    for (int combo : new int[] {2, 3})
      values.add(
          weapon(
              "simplyswords:diamond_chakram",
              combo,
              true,
              120,
              false,
              false,
              8,
              WeaponClass.SLASH));
    for (String name : List.of("longsword", "claymore"))
      for (int combo = 0; combo < 3; combo++)
        values.add(
            weapon("simplyswords:diamond_" + name, combo, false, 120, false, false, 1, null));
    for (String name :
        List.of(
            "greathammer",
            "spear",
            "cutlass",
            "glaive",
            "greataxe",
            "halberd",
            "katana",
            "rapier",
            "sai"))
      values.add(weapon("simplyswords:diamond_" + name, 0, false, 120, false, false, 0, null));
    for (String name :
        List.of(
            "backhand_blade",
            "dagger",
            "deer_horns",
            "grandsword",
            "great_katana",
            "great_spear",
            "khopesh",
            "lance",
            "pernach"))
      values.add(weapon("simplymore:diamond_" + name, 0, false, 120, false, false, 0, null));
    // Re-run the extended paths at twenty FPS, including the actual offhand spin selections.
    values.add(
        weapon("simplyswords:diamond_twinblade", 2, false, 20, true, false, 2, WeaponClass.SLASH));
    values.add(
        weapon("simplymore:diamond_quarterstaff", 3, false, 20, true, false, 2, WeaponClass.BLUNT));
    values.add(
        weapon("simplyswords:diamond_warglaive", 2, true, 20, true, false, 2, WeaponClass.SLASH));
    values.add(
        weapon("simplyswords:diamond_warglaive", 3, true, 20, true, true, 2, WeaponClass.SLASH));
    values.add(
        weapon("simplyswords:diamond_chakram", 2, true, 20, true, false, 8, WeaponClass.SLASH));
    values.add(
        weapon("simplyswords:diamond_chakram", 3, true, 20, true, true, 8, WeaponClass.SLASH));
    values.add(
        weapon("simplyswords:diamond_longsword", 0, false, 20, false, false, 1, WeaponClass.SLASH));
    values.add(
        weapon("simplyswords:diamond_spear", 0, false, 20, false, false, 1, WeaponClass.PIERCE));
    values.add(weapon("simplymore:diamond_grandsword", 1, false, 20, false, false, 0, null));
    return List.copyOf(values);
  }

  private static Case weapon(
      String item,
      int combo,
      boolean dual,
      int fps,
      boolean fire,
      boolean first,
      int emitters,
      WeaponClass family) {
    return new Case(item, combo, dual, fps, fire, first, emitters, family);
  }

  private static boolean enabled() {
    return Boolean.getBoolean("combattraces.validateMultiWeapons");
  }

  @SubscribeEvent
  public static void tick(ClientTickEvent.Post event) {
    if (!enabled()) return;
    var mc = Minecraft.getInstance();
    if (!started && mc.screen != null && mc.getOverlay() == null) {
      started = true;
      mc.options.pauseOnLostFocus = false;
      mc.options.renderDistance().set(4);
      mc.options.fov().set(70);
      mc.options.framerateLimit().set(120);
      DebugRenderer.enabled = false;
      ClientConfig.DEBUG.set(false);
      mc.createWorldOpenFlows()
          .createFreshLevel(
              "ct-multi-weapons-" + System.currentTimeMillis(),
              new LevelSettings(
                  "Multi-weapon trail acceptance",
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
    if (mc.player == null || mc.level == null || ++ticks < 45) return;
    if (index >= CASES.size()) {
      finish(mc);
      return;
    }
    if (step == 0) equip(mc);
    if (step == 9) startAttack(mc);
    if (step >= finishStep && step > 9) {
      finishCase();
      ((PlayerAttackAnimatable) mc.player).stopAttackAnimation(0);
      index++;
      step = 0;
      playing = false;
    } else step++;
  }

  private static void equip(Minecraft mc) {
    playing = false;
    finishStep = 60;
    var c = CASES.get(index);
    mc.setScreen(null);
    mc.options.setCameraType(c.first ? CameraType.FIRST_PERSON : CameraType.THIRD_PERSON_BACK);
    mc.options.framerateLimit().set(c.fps);
    mc.player.setYRot(-18);
    mc.player.setXRot(0);
    ClientConfig.SPEED_THRESHOLD.set(4d);
    ClientConfig.TRAILS.set(true);
    ClientConfig.ELEMENTS.set(true);
    ClientConfig.REPLACE_BETTER_COMBAT.set(true);
    ClientState.clearEffects();
    BASE.clear();
    FIRE.clear();
    RECOVERY.clear();
    window = null;
    resolved = null;
    recoveryQuiet = insideWindow = coherent = shapeCorrect = windupQuiet = true;
    sawWindup = sawRecovery = earlyShot = peakShot = endShot = false;
    frames = 0;
    previousTime = -1;
    previousProgress = maximumFrameStep = 0;
    firstFastProgress = Float.POSITIVE_INFINITY;
    var uuid = mc.player.getUUID();
    mc.getSingleplayerServer()
        .execute(
            () -> {
              var player = mc.getSingleplayerServer().getPlayerList().getPlayer(uuid);
              player.serverLevel().setDayTime(6000);
              var stack = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(c.item)));
              if (c.fire)
                stack.enchant(
                    player
                        .registryAccess()
                        .registryOrThrow(Registries.ENCHANTMENT)
                        .getHolderOrThrow(Enchantments.FIRE_ASPECT),
                    1);
              player.setItemInHand(InteractionHand.MAIN_HAND, stack);
              player.setItemInHand(
                  InteractionHand.OFF_HAND, c.dual ? stack.copy() : ItemStack.EMPTY);
            });
  }

  private static void startAttack(Minecraft mc) {
    var c = CASES.get(index);
    check(
        BuiltInRegistries.ITEM
            .getKey(mc.player.getMainHandItem().getItem())
            .equals(ResourceLocation.parse(c.item)),
        "equipped registered item");
    ((PlayerAttackProperties) mc.player).setComboCount(c.combo);
    var attack = PlayerAttackHelper.getCurrentAttack(mc.player, c.combo);
    if (!check(attack != null, "loaded actual Better Combat combo")) return;
    cooldown = PlayerAttackHelper.getAttackCooldownTicksCapped(mc.player);
    check(Float.isFinite(cooldown) && cooldown > 0, "uses actual equipped attack cooldown");
    finishStep = 9 + (int) Math.ceil(cooldown * 1.5) + 20;
    ((PlayerAttackAnimatable) mc.player)
        .playAttackAnimation(
            attack.attack().animation(),
            AnimatedHand.from(attack.isOffHand(), attack.attributes().isTwoHanded()),
            cooldown,
            (float) attack.upswingRate());
    var motion = CombatTracesApi.motion(mc.player, 0).orElse(null);
    if (!check(motion != null, "animation provider starts current attack")) return;
    window = motion.swingWindow();
    if (!check(window != null, "supplies strike interval")) return;
    attackId = motion.attackId();
    hand = motion.hand();
    check(
        (hand == InteractionHand.OFF_HAND) == (c.dual && (c.combo & 1) != 0),
        "uses selected combo hand");
    var model = mc.getItemRenderer().getModel(motion.weapon(), mc.level, mc.player, 0);
    resolved = WeaponResolver.resolve(mc.player, motion, model);
    if (c.emitterCount > 0)
      check(
          resolved.emitters().size() == c.emitterCount,
          "resolves " + c.emitterCount + " weapon emitters");
    if (c.family != null) check(resolved.weaponClass() == c.family, "classifies " + c.family);
    check(
        resolved.emitters().stream()
            .allMatch(
                e ->
                    TrailHistory.finite(e.origin())
                        && TrailHistory.finite(e.tip())
                        && e.origin().distanceTo(e.tip()) > .015),
        "all emitters have finite nonzero model spans");
    if (c.emitterCount > 1) {
      check(
          ModelEmitters.analyze(motion.weapon(), model, resolved.weaponClass()).isPresent(),
          "emitter geometry comes from actual baked model");
      if (c.emitterCount == 2 && resolved.emitters().size() == 2) {
        var a = resolved.emitters().get(0);
        var b = resolved.emitters().get(1);
        if (resolved.weaponClass() == WeaponClass.BLUNT) {
          double separation = a.origin().lerp(a.tip(), .5).distanceTo(b.origin().lerp(b.tip(), .5));
          check(
              separation
                  > Math.max(a.origin().distanceTo(a.tip()), b.origin().distanceTo(b.tip())) * 2,
              "blunt end caps lie at separate shaft ends");
        } else {
          check(
              a.tip().subtract(a.origin()).normalize().dot(b.tip().subtract(b.origin()).normalize())
                  < -.2,
              "two emitters extend toward opposing ends");
          check(
              a.tip().distanceTo(b.tip()) > a.origin().distanceTo(b.origin()),
              "outer ends are separated across the weapon");
        }
      } else if (c.emitterCount == 8 && resolved.emitters().size() == 8) {
        boolean closed = true;
        for (int i = 0; i < 8; i++)
          closed &=
              resolved
                      .emitters()
                      .get(i)
                      .origin()
                      .distanceTo(resolved.emitters().get((i + 1) % 8).tip())
                  < 1e-6;
        check(closed, "circular segments join into a closed rim");
        long distinct = resolved.emitters().stream().map(TrailEmitter::origin).distinct().count();
        check(distinct == 8, "rim has eight distinct boundary points");
      }
    }
    if (c.fire)
      check(
          resolved.elements().contains(ResourceLocation.parse("combattraces:fire")),
          "Fire Aspect resolves fire overlay");
    ANIMATIONS.add(attack.attack().animation());
    DETAILS.add(
        label()
            + " animation="
            + attack.attack().animation()
            + " hand="
            + hand
            + " category="
            + motion.category()
            + " cooldown="
            + cooldown
            + " upswing="
            + attack.upswingRate()
            + " window="
            + window
            + " hitbox="
            + motion.hitbox()
            + " emitters="
            + resolved.emitters());
    playing = true;
  }

  @SubscribeEvent
  public static void render(RenderLevelStageEvent event) {
    if (!enabled() || !playing || event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL)
      return;
    var mc = Minecraft.getInstance();
    if (mc.player == null || window == null || resolved == null) return;
    frames++;
    float progress = previousProgress;
    double speed = 0;
    for (int i = 0; i < resolved.emitters().size(); i++) {
      var observation = WeaponMotionTracker.latest(mc.player.getId(), hand, i);
      if (observation == null || observation.motion().attackId() != attackId) continue;
      progress = observation.motion().progress();
      speed =
          Math.max(
              speed,
              Math.max(
                  observation.state().tipVelocity().length(),
                  observation.state().originVelocity().length()));
      if (i == 0 && observation.sample().time() != previousTime) {
        if (previousTime >= 0)
          maximumFrameStep = Math.max(maximumFrameStep, progress - previousProgress);
        previousTime = observation.sample().time();
        previousProgress = progress;
        DETAILS.add(
            String.format(
                Locale.ROOT,
                "%s frame=%d progress=%.5f speed=%.3f",
                label(),
                frames,
                progress,
                speed));
      }
    }
    if (speed >= 4 && progress >= window.start() && progress <= window.end())
      firstFastProgress = Math.min(firstFastProgress, progress);
    var entries = trailEntries();
    var currentBase = new HashMap<Integer, TrailInstance>();
    var currentFire = new HashMap<Integer, TrailInstance>();
    int currentTrails = 0;
    boolean recovering = progress > window.end() + 1e-5;
    for (var entry : entries.entrySet()) {
      var key = entry.getKey();
      var trail = entry.getValue();
      if (key.entity() != mc.player.getId() || key.attack() != attackId) continue;
      currentTrails++;
      if (recovering) {
        Double old = RECOVERY.put(trail, trail.touched);
        if (sawRecovery && old != null) recoveryQuiet &= old.doubleValue() == trail.touched;
      }
      var target =
          key.layer().equals("base") ? BASE : key.layer().equals("combattraces:fire") ? FIRE : null;
      if (target == null) continue;
      var coverage = target.computeIfAbsent(key.emitter(), ignored -> new Coverage());
      coverage.strokes.add(trail);
      int count = trail.history.size();
      boolean improved = count > coverage.maximumSamples;
      coverage.maximumSamples = Math.max(coverage.maximumSamples, count);
      for (int n = 0; n < count; n++) {
        var sample = trail.history.get(n);
        insideWindow &=
            sample.progress() >= window.start() - 1e-4 && sample.progress() <= window.end() + 1e-4;
        coverage.finite &=
            TrailHistory.finite(sample.origin()) && TrailHistory.finite(sample.tip());
        coverage.first = Math.min(coverage.first, sample.progress());
        coverage.last = Math.max(coverage.last, sample.progress());
      }
      if (count >= 2) {
        coverage.retained =
            Math.max(
                coverage.retained,
                trail.history.get(count - 1).progress() - trail.history.get(0).progress());
        if (improved || coverage.renderedFrames == 0) {
          var vertices =
              TrailGeometryValidation.vertices(trail, event.getCamera().getPosition(), 1);
          if (!vertices.isEmpty() && vertices.stream().allMatch(TrailHistory::finite))
            coverage.renderedFrames++;
        }
      }
      var motion = WeaponMotionTracker.latest(mc.player.getId(), hand, key.emitter());
      if (motion != null) {
        boolean stab =
            trail.family != WeaponClass.BLUNT
                && HitboxImpact.kind(motion.motion().hitbox()) == HitboxImpact.Kind.STAB;
        shapeCorrect &= stab == trail.thrust;
      }
      if (key.layer().equals("base")) currentBase.put(key.emitter(), trail);
      if (key.layer().equals("combattraces:fire")) currentFire.put(key.emitter(), trail);
    }
    if (progress < window.start()) {
      sawWindup = true;
      windupQuiet &= currentTrails == 0;
    }
    if (recovering) sawRecovery = true;
    if (CASES.get(index).fire)
      for (var entry : currentFire.entrySet()) {
        var base = currentBase.get(entry.getKey());
        var fire = entry.getValue();
        coherent &=
            base != null
                && base.family == fire.family
                && base.thrust == fire.thrust
                && base.history.size() == fire.history.size()
                && base.style.swept()
                && fire.style.swept()
                && base.style.color() != fire.style.color()
                && !fire.enchanted;
        if (base != null && base.history.size() == fire.history.size())
          for (int n = 0; n < base.history.size(); n++)
            coherent &= base.history.get(n).equals(fire.history.get(n));
      }
    float phase = (progress - window.start()) / (window.end() - window.start());
    if (!BASE.isEmpty() && BASE.values().stream().anyMatch(c -> c.renderedFrames > 0)) {
      if (!earlyShot) {
        screenshot("early");
        earlyShot = true;
      }
      if (!peakShot && phase >= .5) {
        screenshot("peak");
        peakShot = true;
      }
      if (!endShot && phase >= .9) {
        screenshot("end");
        endShot = true;
      }
    }
  }

  @SuppressWarnings("unchecked")
  private static Map<TrailManager.Key, TrailInstance> trailEntries() {
    try {
      if (trailsField == null) {
        trailsField = TrailManager.class.getDeclaredField("TRAILS");
        trailsField.setAccessible(true);
      }
      return new LinkedHashMap<>((Map<TrailManager.Key, TrailInstance>) trailsField.get(null));
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException(e);
    }
  }

  private static void finishCase() {
    if (!playing || window == null || resolved == null) {
      check(false, "case produced an inspectable attack");
      return;
    }
    check(frames > 0, "observed actual rendered attack frames");
    check(!sawWindup || windupQuiet, "windup emits no trail");
    check(recoveryQuiet, "recovery appends no samples");
    check(insideWindow, "every base and elemental pose stays in strike interval");
    check(shapeCorrect, "depth-dominant attacks keep stabs and blunt weapons keep blunt geometry");
    check(
        BASE.size() == resolved.emitters().size(),
        "every weapon end or rim segment emits a base trail");
    float span = window.end() - window.start();
    float endTolerance = Math.max(.02f, span * .08f);
    for (int i = 0; i < resolved.emitters().size(); i++) {
      var coverage = BASE.get(i);
      if (!check(coverage != null, "emitter " + i + " exists")) continue;
      check(
          coverage.maximumSamples >= 2 && coverage.renderedFrames > 0 && coverage.finite,
          "emitter " + i + " generates finite visible mesh at " + CASES.get(index).fps + " FPS");
      check(
          coverage.retained >= span * .5f,
          "emitter " + i + " retains at least half of complete striking motion");
      check(
          coverage.last >= window.end() - endTolerance, "emitter " + i + " reaches end of strike");
      if (Float.isFinite(firstFastProgress)
          && firstFastProgress <= window.start() + maximumFrameStep + .02f)
        check(
            coverage.first
                <= window.start() + Math.max(.02f, Math.min(span * .25f, maximumFrameStep)),
            "emitter " + i + " starts promptly when weapon reaches configured speed");
      check(coverage.strokes.size() == 1, "emitter " + i + " remains one continuous strike");
      DETAILS.add(
          label()
              + " emitter="
              + i
              + " emitted="
              + coverage.first
              + ".."
              + coverage.last
              + " retained="
              + coverage.retained
              + " span="
              + span
              + " maximumSamples="
              + coverage.maximumSamples
              + " strokes="
              + coverage.strokes.size()
              + " maximumFrameStep="
              + maximumFrameStep
              + " firstFastProgress="
              + firstFastProgress);
    }
    if (CASES.get(index).fire) {
      check(
          FIRE.size() == resolved.emitters().size(),
          "every end or rim segment receives elemental overlay");
      check(coherent, "Fire Aspect shares base motion, geometry and timing with its own color");
      check(
          FIRE.values().stream().allMatch(c -> c.renderedFrames > 0 && c.retained >= span * .5f),
          "all elemental strips render throughout the striking motion");
    }
    flush();
  }

  private static boolean check(boolean success, String message) {
    String line = (success ? "PASS " : "FAIL ") + label() + " " + message;
    RESULTS.add(line);
    if (!success) failures++;
    System.out.println("COMBAT TRACES MULTI " + line);
    return success;
  }

  private static String label() {
    if (index >= CASES.size()) return "suite";
    var c = CASES.get(index);
    return "["
        + index
        + "] "
        + c.item
        + " combo "
        + c.combo
        + (c.dual ? " dual" : "")
        + " "
        + c.fps
        + "FPS"
        + (c.fire ? " FireAspect" : "")
        + (c.first ? " firstPerson" : "");
  }

  private static void screenshot(String suffix) {
    var mc = Minecraft.getInstance();
    Screenshot.grab(
        mc.gameDirectory,
        "mw-" + index + "-" + suffix + ".png",
        mc.getMainRenderTarget(),
        ignored -> {});
  }

  private static void flush() {
    try {
      Files.writeString(Path.of("multi-weapon-validation.txt"), String.join("\n", RESULTS) + "\n");
      Files.writeString(
          Path.of("multi-weapon-validation-detail.txt"), String.join("\n", DETAILS) + "\n");
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  private static void finish(Minecraft mc) {
    DETAILS.add(
        "cases="
            + CASES.size()
            + " uniqueAnimations="
            + ANIMATIONS.size()
            + " animations="
            + ANIMATIONS);
    flush();
    if (failures > 0)
      throw new IllegalStateException(
          "COMBAT TRACES MULTI-WEAPON ACCEPTANCE FAIL: " + failures + " / " + RESULTS.size());
    System.out.println(
        "COMBAT TRACES MULTI-WEAPON ACCEPTANCE PASS: "
            + RESULTS.size()
            + " checks across "
            + CASES.size()
            + " cases");
    mc.stop();
  }
}

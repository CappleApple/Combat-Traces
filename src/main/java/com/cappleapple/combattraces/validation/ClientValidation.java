package com.cappleapple.combattraces.validation;

import com.cappleapple.combattraces.api.*;
import com.cappleapple.combattraces.client.*;
import com.cappleapple.combattraces.client.element.ElementResolver;
import com.cappleapple.combattraces.client.impact.*;
import com.cappleapple.combattraces.client.material.MaterialResolver;
import com.cappleapple.combattraces.client.trail.*;
import com.cappleapple.combattraces.client.weapon.WeaponResolver;
import com.cappleapple.combattraces.config.ClientConfig;
import com.cappleapple.combattraces.motion.*;
import com.mojang.authlib.GameProfile;
import java.nio.file.*;
import java.util.*;
import net.bettercombat.client.animation.PlayerAttackAnimatable;
import net.bettercombat.logic.AnimatedHand;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Development-only acceptance harness. Excluded from the shipped JAR along with its animation
 * fixture.
 */
@EventBusSubscriber(modid = "combattraces", value = Dist.CLIENT)
public final class ClientValidation {
  private static boolean started;
  private static int ticks;
  private static volatile int targetId;
  private static RemotePlayer remote;
  private static final List<String> results = new ArrayList<>();
  private static final List<TrailSample> third = new ArrayList<>(),
      first = new ArrayList<>(),
      custom = new ArrayList<>();
  private static long oldRevision;
  private static boolean fastShot;

  @SubscribeEvent
  public static void tick(ClientTickEvent.Post event) {
    if (!Boolean.getBoolean("combattraces.validate")) return;
    var mc = Minecraft.getInstance();
    if (!started && mc.screen != null && mc.getOverlay() == null) {
      started = true;
      mc.options.pauseOnLostFocus = false;
      mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
      mc.options.renderDistance().set(4);
      mc.options.framerateLimit().set(120);
      mc.createWorldOpenFlows()
          .createFreshLevel(
              "ct-validation-" + System.currentTimeMillis(),
              new LevelSettings(
                  "Combat Traces acceptance",
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
    if (ticks == 25) {
      var uuid = mc.player.getUUID();
      mc.getSingleplayerServer()
          .execute(
              () -> {
                var player = mc.getSingleplayerServer().getPlayerList().getPlayer(uuid);
                player.serverLevel().setDayTime(6000);
                var target = EntityType.VILLAGER.create(player.serverLevel());
                target.setNoAi(true);
                target.moveTo(player.getX() + .7, player.getY(), player.getZ() + 1.4, 180, 0);
                player.serverLevel().addFreshEntity(target);
                targetId = target.getId();
                var weapon = new ItemStack(Items.DIAMOND_SWORD);
                weapon.enchant(
                    player
                        .registryAccess()
                        .registryOrThrow(Registries.ENCHANTMENT)
                        .getHolderOrThrow(Enchantments.FIRE_ASPECT),
                    2);
                player.setItemInHand(InteractionHand.MAIN_HAND, weapon);
              });
    }
    if (ticks == 40) {
      mc.setScreen(null);
      mc.player.setYRot(0);
      mc.player.setXRot(0);
      attack(mc.player, "bettercombat:one_handed_slash_horizontal_right", 30);
    }
    if (ticks > 40 && ticks < 80) collect(third, mc.player.getId());
    if (ticks == 52) {
      var motion = CombatTracesApi.motion(mc.player, 0).orElseThrow();
      check(
          ElementResolver.resolve(mc.player, motion, List.of()).contains(id("fire")),
          "Fire Aspect resolves fire from synced enchantment");
      check(
          ClientDefinitions.current.trails().size() >= 7
              && ClientDefinitions.current.materials().size() >= 10,
          "Client receives server effect definitions");
    }
    if (!fastShot
        && ticks > 44
        && ticks < 75
        && DebugState.state != null
        && DebugState.state.speed() > 7) {
      screenshot("fast-ribbon.png");
      fastShot = true;
    }
    if (ticks == 59 || ticks == 63 || ticks == 68) screenshot("third-person-" + ticks + ".png");
    if (ticks == 60) damageTarget();
    if (ticks == 80) {
      check(
          third.size() >= 8 && travel(third) > .4,
          "Actual third-person animated poses: "
              + third.size()
              + " tick observations, "
              + String.format(Locale.ROOT, "%.2f", travel(third))
              + " blocks of tip travel");
      check(ImpactController.receivedHits >= 1, "Server-confirmed entity hit reached client");
      check(
          MaterialResolver.block(Blocks.STONE.defaultBlockState()).equals(id("stone")),
          "Stone material via loaded tags");
      check(
          MaterialResolver.block(Blocks.OAK_LOG.defaultBlockState()).equals(id("wood")),
          "Wood material via loaded tags");
      check(
          MaterialResolver.block(Blocks.IRON_BLOCK.defaultBlockState()).equals(id("metal")),
          "Metal material via loaded tags");
      check(
          MaterialResolver.block(Blocks.ICE.defaultBlockState()).equals(id("ice")),
          "Ice material via loaded tags");
      mc.options.setCameraType(CameraType.FIRST_PERSON);
    }
    if (ticks == 85) attack(mc.player, "combattraces_validation:diagonal_probe", 26);
    if (ticks > 85 && ticks < 119) collect(first, mc.player.getId());
    if (ticks == 98 || ticks == 106) screenshot("first-person-" + ticks + ".png");
    if (ticks == 120) {
      check(
          first.size() >= 8 && travel(first) > .4,
          "Actual first-person custom-animation poses: " + first.size() + " tick observations");
      check(
          first.stream().anyMatch(s -> s.progress() > 0.3 && s.progress() < 0.8),
          "Custom animation progress comes from Player Animator");
      mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
    }
    if (ticks == 125) {
      remote = new RemotePlayer(mc.level, new GameProfile(UUID.randomUUID(), "TracePartner"));
      remote.setId(100001);
      remote.moveTo(mc.player.getX() - 1.4, mc.player.getY(), mc.player.getZ() + .5, 0, 0);
      remote.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_SWORD));
      remote.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
      mc.level.addEntity(remote);
      attack(remote, "combattraces_validation:diagonal_probe", 28);
    }
    if (ticks > 125 && ticks < 160) collect(custom, 100001);
    if (ticks == 140) screenshot("remote-custom-animation.png");
    if (ticks == 160) {
      check(
          custom.size() >= 8 && travel(custom) > .4,
          "RemotePlayer rendered custom animation: " + custom.size() + " tick observations");
      check(
          MaterialResolver.entity(remote, false).equals(id("metal")),
          "Worn metal armor changes entity material");
      budgetCheck(mc);
      oldRevision = ClientDefinitions.appliedRevision;
      installReloadPack(mc);
    }
    if (ticks == 195) {
      check(
          ClientDefinitions.appliedRevision > oldRevision,
          "Datapack enable/reload atomically synchronizes definitions");
      var rule = WeaponResolver.override(mc.player.getMainHandItem());
      check(
          rule != null && rule.emitters().size() == 2,
          "Reloaded explicit twin emitters override model inference");
      mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
    }
    if (ticks == 200) attack(mc.player, "combattraces_validation:diagonal_probe", 28);
    if (ticks == 215) {
      check(
          TrailManager.trails().stream().filter(t -> t.owner == mc.player).count() >= 2,
          "Multiple emitters create independent ribbons");
      screenshot("twin-emitters.png");
    }
    if (ticks == 230) {
      var uuid = mc.player.getUUID();
      mc.getSingleplayerServer()
          .execute(
              () ->
                  mc.getSingleplayerServer()
                      .getPlayerList()
                      .getPlayer(uuid)
                      .setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.IRON_SWORD)));
      mc.options.setCameraType(CameraType.FIRST_PERSON);
      mc.options.fov().set(110);
      mc.options.framerateLimit().set(20);
    }
    if (ticks == 240)
      ((PlayerAttackAnimatable) mc.player)
          .playAttackAnimation(
              "combattraces_validation:diagonal_probe", AnimatedHand.OFF_HAND, 26, .5f);
    if (ticks == 252) {
      var offhand = WeaponMotionTracker.latest(mc.player.getId(), InteractionHand.OFF_HAND, 0);
      check(
          offhand != null && offhand.motion().hand() == InteractionHand.OFF_HAND,
          "Offhand animation is sampled at 20 FPS and 110 FOV");
      screenshot("offhand-first-person-low-fps.png");
    }
    if (ticks == 275) {
      try {
        Files.writeString(
            Path.of("client-validation.txt"),
            String.join("\n", results) + "\n",
            java.nio.charset.StandardCharsets.UTF_8);
        System.out.println("COMBAT TRACES CLIENT ACCEPTANCE PASS: " + results.size() + " checks");
        mc.stop();
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }

  private static void installReloadPack(Minecraft mc) {
    var server = mc.getSingleplayerServer();
    server.execute(
        () -> {
          try {
            Path pack = server.getWorldPath(LevelResource.DATAPACK_DIR).resolve("ct-validation");
            Path data =
                pack.resolve("data/combattraces_validation/combat_traces/weapons/twin.json");
            Files.createDirectories(data.getParent());
            Files.writeString(
                pack.resolve("pack.mcmeta"),
                "{\"pack\":{\"pack_format\":48,\"description\":\"Combat Traces"
                    + " runtime validation\"}}");
            Files.writeString(
                data,
                "{\"priority\":10000,\"items\":[\"minecraft:diamond_sword\"],\"weapon_class\":\"combattraces:slash\",\"emitters\":[{\"origin\":[-0.1,-0.1,0],\"tip\":[0.45,0.45,0]},{\"origin\":[0.1,0.1,0],\"tip\":[-0.45,-0.45,0]}]}");
            server.getPackRepository().reload();
            server
                .getCommands()
                .performPrefixedCommand(
                    server.createCommandSourceStack(), "datapack enable \"file/ct-validation\"");
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
        });
  }

  private static void budgetCheck(Minecraft mc) {
    int trails = ClientConfig.MAX_TRAILS.get(), samples = ClientConfig.TOTAL_SAMPLES.get();
    ClientConfig.MAX_TRAILS.set(10);
    ClientConfig.TOTAL_SAMPLES.set(64);
    TrailManager.clear();
    var motion =
        new CombatMotion(
            999,
            mc.player.getMainHandItem(),
            InteractionHand.MAIN_HAND,
            id("budget"),
            .5f,
            .5f,
            0,
            1,
            1,
            "sword",
            "");
    double now = VisualClock.now();
    for (int n = 0; n < 25; n++)
      for (int i = 0; i < 12; i++) {
        var owner = n == 24 ? mc.player : remote;
        var point = owner.position().add(i * .1, 1, 0);
        TrailManager.sample(
            owner,
            motion,
            n,
            false,
            new TrailSample(point, point.add(0, 1, 0), now + i * .01, .5f),
            ClientDefinitions.current.trail(id("slash")),
            "base",
            1);
      }
    check(TrailManager.trails().size() <= 10, "Hard trail count survives 25-emitter burst");
    check(
        TrailManager.trails().stream().mapToInt(t -> t.history.size()).sum() <= 64,
        "Hard total-sample budget survives burst");
    check(
        TrailManager.trails().stream().anyMatch(t -> t.owner == mc.player),
        "Budget retains local-player effects");
    ClientConfig.MAX_TRAILS.set(trails);
    ClientConfig.TOTAL_SAMPLES.set(samples);
    TrailManager.clear();
  }

  private static void damageTarget() {
    var mc = Minecraft.getInstance();
    var uuid = mc.player.getUUID();
    mc.getSingleplayerServer()
        .execute(
            () -> {
              var player = mc.getSingleplayerServer().getPlayerList().getPlayer(uuid);
              var target = player.serverLevel().getEntity(targetId);
              if (target != null) target.hurt(player.damageSources().playerAttack(player), 4);
            });
  }

  private static void attack(
      net.minecraft.client.player.AbstractClientPlayer player, String animation, float length) {
    ((PlayerAttackAnimatable) player)
        .playAttackAnimation(animation, AnimatedHand.MAIN_HAND, length, .5f);
  }

  private static void collect(List<TrailSample> samples, int id) {
    var sample = HeldItemCapture.LATEST.get(id);
    if (sample != null && (samples.isEmpty() || samples.getLast().time() != sample.time()))
      samples.add(sample);
  }

  private static double travel(List<TrailSample> samples) {
    double d = 0;
    for (int i = 1; i < samples.size(); i++)
      d += samples.get(i).tip().distanceTo(samples.get(i - 1).tip());
    return d;
  }

  private static void screenshot(String name) {
    var mc = Minecraft.getInstance();
    Screenshot.grab(mc.gameDirectory, name, mc.getMainRenderTarget(), ignored -> {});
  }

  private static ResourceLocation id(String path) {
    return ResourceLocation.fromNamespaceAndPath("combattraces", path);
  }

  private static void check(boolean condition, String name) {
    if (!condition) throw new IllegalStateException("COMBAT TRACES ACCEPTANCE FAILED: " + name);
    results.add("PASS " + name);
    System.out.println("COMBAT TRACES PASS: " + name);
  }
}

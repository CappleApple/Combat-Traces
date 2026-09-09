package com.cappleapple.combattraces.validation;

import com.cappleapple.combattraces.api.*;
import com.cappleapple.combattraces.data.*;
import com.cappleapple.combattraces.network.*;
import com.cappleapple.combattraces.server.ServerDefinitions;
import io.netty.buffer.Unpooled;
import java.util.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Enchantments;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("combattraces")
@PrefixGameTestTemplate(false)
public final class CombatTracesGameTests {
  @GameTest(templateNamespace = "minecraft", template = "bastion/mobs/empty")
  public static void serverDatapackCatalogLoads(GameTestHelper helper) {
    helper.assertTrue(
        ServerDefinitions.current.trails().size() >= 7,
        "Trail definitions loaded on dedicated server");
    helper.assertTrue(
        ServerDefinitions.current.materials().size() >= 10, "Material mappings loaded");
    helper.assertTrue(
        ServerDefinitions.current.elements().size() >= 8, "Element definitions loaded");
    helper.succeed();
  }

  @GameTest(templateNamespace = "minecraft", template = "bastion/mobs/empty")
  public static void fireAspectPredicateUsesRealRegistry(GameTestHelper helper) {
    var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
    var stack = new ItemStack(Items.DIAMOND_SWORD);
    var motion =
        new CombatMotion(
            1,
            stack,
            InteractionHand.MAIN_HAND,
            ResourceLocation.parse("test:diagonal"),
            .4f,
            .5f,
            0,
            1,
            1,
            "sword",
            "");
    var fire =
        ServerDefinitions.current.elements().stream()
            .filter(e -> e.id().equals(ResourceLocation.parse("combattraces:fire")))
            .findFirst()
            .orElseThrow();
    helper.assertFalse(fire.condition().test(player, stack, motion), "Plain sword is physical");
    stack.enchant(
        helper
            .getLevel()
            .registryAccess()
            .registryOrThrow(Registries.ENCHANTMENT)
            .getHolderOrThrow(Enchantments.FIRE_ASPECT),
        1);
    helper.assertTrue(
        fire.condition().test(player, stack, motion), "Fire Aspect selects the fire element");
    helper.succeed();
  }

  @GameTest(templateNamespace = "minecraft", template = "bastion/mobs/empty")
  public static void hitPacketRoundTripPreservesAuthority(GameTestHelper helper) {
    var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
    try {
      var original = new HitPayload(42, 95, 7.25f, true, true, 12345678);
      HitPayload.CODEC.encode(buffer, original);
      helper.assertTrue(
          original.equals(HitPayload.CODEC.decode(buffer)), "Hit metadata round trips exactly");
    } finally {
      buffer.release();
    }
    helper.succeed();
  }

  @GameTest(templateNamespace = "minecraft", template = "bastion/mobs/empty")
  public static void definitionPacketAndCatalogRoundTrip(GameTestHelper helper) {
    var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
    try {
      var packet = new DefinitionPayload(8, 1, 3, "{\"test:trails/slash\":{\"opacity\":0.75}}");
      DefinitionPayload.CODEC.encode(buffer, packet);
      helper.assertTrue(
          packet.equals(DefinitionPayload.CODEC.decode(buffer)),
          "Config chunks round trip exactly");
      var decoded = Definitions.compile(DefinitionWire.decode(packet.json()));
      helper.assertTrue(decoded.trails().size() == 1, "Decoded config compiles");
    } finally {
      buffer.release();
    }
    helper.succeed();
  }

  @GameTest(templateNamespace = "minecraft", template = "bastion/mobs/empty")
  public static void swordTagsClassifyWithoutCombatDependency(GameTestHelper helper) {
    var stack = new ItemStack(Items.DIAMOND_SWORD);
    helper.assertTrue(
        ServerDefinitions.current.classifications().stream()
            .anyMatch(rule -> rule.matches(stack) && rule.weaponClass() == WeaponClass.SLASH),
        "Vanilla sword tag is a slash family");
    helper.succeed();
  }
}

package com.cappleapple.combattraces.motion;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class WeaponTopologyTest {
  @Test
  void bladedBattlestaffNameAndTemplateTakePrecedenceOverGenericStaffCategory() {
    assertEquals(WeaponTopology.DOUBLE_BLADE, detect("battlestaff", "diamond_twinblade"));
    assertEquals(
        WeaponTopology.DOUBLE_BLADE,
        WeaponTopology.detect(
            "battlestaff",
            "stars_edge",
            List.of(),
            List.of("item/template_twinblade", "item/generated")));
  }

  @Test
  void quarterstaffRemainsBluntWhenItInheritsTwinbladeDisplayTemplate() {
    assertEquals(
        WeaponTopology.DOUBLE_BLUNT,
        WeaponTopology.detect(
            "quarterstaff", "diamond_quarterstaff", List.of(), List.of("item/template_twinblade")));
    assertEquals(
        WeaponTopology.DOUBLE_BLUNT,
        WeaponTopology.detect("", "stasis", List.of("weapon_types/quarterstaffs"), List.of()));
  }

  @Test
  void implicitTagsCoverUniqueWarglaivesAndChakrams() {
    assertEquals(
        WeaponTopology.DOUBLE_BLADE,
        WeaponTopology.detect("", "mystery", List.of("implicit/warglaive"), List.of()));
    assertEquals(
        WeaponTopology.CIRCULAR,
        WeaponTopology.detect("", "tempest", List.of("weapon_types/chakrams"), List.of()));
    assertEquals(WeaponTopology.CIRCULAR, detect("chakram", "unknown"));
  }

  @Test
  void singleBackhandBladeDoesNotBecomeDoubleEndedByReusingWarglaiveAttacks() {
    assertEquals(
        WeaponTopology.SINGLE,
        WeaponTopology.detect(
            "warglaive", "diamond_backhand_blade", List.of(), List.of("item/template_longdagger")));
    assertEquals(
        WeaponTopology.SINGLE,
        WeaponTopology.detect(
            "warglaive", "unique_weapon", List.of(), List.of("item/template_longdagger")));
  }

  @Test
  void ordinaryWeaponsAndUnrelatedNamesRetainSingleEmitter() {
    for (var name :
        List.of(
            "diamond_longsword",
            "diamond_greathammer",
            "diamond_spear",
            "staff_of_healing",
            "chakramatic_wand")) assertEquals(WeaponTopology.SINGLE, detect("", name));
  }

  @Test
  void nullCategoryAndPluralTagsAreSafe() {
    assertEquals(
        WeaponTopology.DOUBLE_BLADE,
        WeaponTopology.detect(null, "unknown", List.of("weapon_types/twinblades"), List.of()));
    assertEquals(
        WeaponTopology.SINGLE, WeaponTopology.detect(null, "unknown", List.of(), List.of()));
  }

  private static WeaponTopology detect(String category, String item) {
    return WeaponTopology.detect(category, item, List.of(), List.of());
  }
}

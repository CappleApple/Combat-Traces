package com.cappleapple.combattraces.motion;

import java.util.Collection;
import java.util.Locale;
import java.util.stream.Stream;

/** Geometry hints supplement the silhouette without supplying item-specific coordinates. */
public enum WeaponTopology {
  SINGLE,
  DOUBLE_BLADE,
  DOUBLE_BLUNT,
  CIRCULAR;

  public static WeaponTopology detect(
      String category, String item, Collection<String> tags, Collection<String> modelParents) {
    var names =
        Stream.concat(Stream.of(item), Stream.concat(tags.stream(), modelParents.stream()))
            .filter(java.util.Objects::nonNull)
            .map(s -> s.toLowerCase(Locale.ROOT))
            .toList();
    if (names.stream().anyMatch(s -> has(s, "chakram"))) return CIRCULAR;
    // A quarterstaff can inherit a twinblade display transform while retaining blunt ends.
    if (names.stream().anyMatch(s -> has(s, "quarterstaff"))
        || has(normalize(category), "quarterstaff")) return DOUBLE_BLUNT;
    if (names.stream().anyMatch(s -> has(s, "twinblade") || has(s, "warglaive")))
      return DOUBLE_BLADE;
    // Some reverse-grip single blades reuse warglaive attacks. Their model remains authoritative.
    if (names.stream()
        .anyMatch(s -> has(s, "longdagger") || has(s, "dagger") || s.contains("backhand_blade")))
      return SINGLE;
    String attackCategory = normalize(category);
    if (has(attackCategory, "chakram")) return CIRCULAR;
    if (has(attackCategory, "twinblade") || has(attackCategory, "warglaive")) return DOUBLE_BLADE;
    if (has(attackCategory, "battlestaff")
        || names.stream().anyMatch(s -> has(s, "battlestaff") || has(s, "double_ended")))
      return DOUBLE_BLUNT;
    return SINGLE;
  }

  private static String normalize(String value) {
    return value == null ? "" : value.toLowerCase(Locale.ROOT);
  }

  private static boolean has(String value, String token) {
    for (String part : value.split("[:/_. -]+"))
      if (part.equals(token) || part.equals(token + "s")) return true;
    return token.equals("double_ended") && value.contains("double_ended");
  }
}

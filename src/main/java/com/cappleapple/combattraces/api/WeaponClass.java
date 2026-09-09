package com.cappleapple.combattraces.api;

public enum WeaponClass {
  SLASH,
  CLEAVE,
  BLUNT,
  PIERCE,
  CLAW,
  WHIP,
  MAGIC,
  GENERIC;

  public static WeaponClass parse(String id) {
    return valueOf(id.substring(id.indexOf(':') + 1).toUpperCase(java.util.Locale.ROOT));
  }

  public String path() {
    return name().toLowerCase(java.util.Locale.ROOT);
  }
}

package com.cappleapple.combattraces.api;

@FunctionalInterface
public interface ImpactModifier {
  ImpactContext modify(ImpactContext context);
}

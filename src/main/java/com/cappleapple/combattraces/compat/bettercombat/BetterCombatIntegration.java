package com.cappleapple.combattraces.compat.bettercombat;

import com.cappleapple.combattraces.api.CombatTracesApi;
import java.util.Optional;

public final class BetterCombatIntegration {
  private BetterCombatIntegration() {}

  public static void register() {
    net.bettercombat.api.client.BetterCombatClientEvents.ATTACK_HIT.register(
        (player, hand, targets, cursor) ->
            com.cappleapple.combattraces.client.impact.ClientHitFallback.candidates(
                player, targets));
    CombatTracesApi.registerMotionProvider(
        0,
        (entity, partial) ->
            entity instanceof BetterCombatState state
                ? state.combatTraces$motion(partial)
                : Optional.empty());
  }
}

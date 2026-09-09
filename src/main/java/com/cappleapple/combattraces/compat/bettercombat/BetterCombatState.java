package com.cappleapple.combattraces.compat.bettercombat;

import com.cappleapple.combattraces.api.CombatMotion;
import java.util.Optional;

public interface BetterCombatState {
  Optional<CombatMotion> combatTraces$motion(float partialTick);
}

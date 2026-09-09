package com.cappleapple.combattraces.api;

import java.util.List;

/** Optional interface for custom baked models. Coordinates are relative to the model center. */
public interface TrailEmitterModel {
  List<TrailEmitter> combatTracesEmitters();
}

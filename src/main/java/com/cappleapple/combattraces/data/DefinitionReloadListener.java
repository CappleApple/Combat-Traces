package com.cappleapple.combattraces.data;

import com.google.gson.*;
import java.io.*;
import java.util.*;
import java.util.function.Consumer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.*;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.LoggerFactory;

/** Bounded readers protect reloads from accidentally enormous definition files. */
public final class DefinitionReloadListener
    extends SimplePreparableReloadListener<Map<ResourceLocation, JsonElement>> {
  private final Consumer<Map<ResourceLocation, JsonElement>> apply;

  public DefinitionReloadListener(Consumer<Map<ResourceLocation, JsonElement>> apply) {
    this.apply = apply;
  }

  @Override
  protected Map<ResourceLocation, JsonElement> prepare(
      ResourceManager manager, ProfilerFiller profiler) {
    Map<ResourceLocation, JsonElement> result = new TreeMap<>();
    int total = 0;
    for (var entry :
        manager
            .listResources("combat_traces", path -> path.getPath().endsWith(".json"))
            .entrySet()) {
      var file = entry.getKey();
      String path =
          file.getPath().substring("combat_traces/".length(), file.getPath().length() - 5);
      int slash = path.indexOf('/');
      if (slash < 0 || !Definitions.SECTIONS.contains(path.substring(0, slash))) continue;
      try (var reader = entry.getValue().openAsReader()) {
        char[] data = new char[16385];
        int read = 0, n;
        while (read < data.length && (n = reader.read(data, read, data.length - read)) > 0)
          read += n;
        if (read > 16384 || result.size() >= 2048 || total + read > 400000)
          throw new IOException("Definition size budget exceeded");
        var parsed = JsonParser.parseString(new String(data, 0, read));
        if (!parsed.isJsonObject()) throw new IOException("Expected JSON object");
        result.put(ResourceLocation.fromNamespaceAndPath(file.getNamespace(), path), parsed);
        total += read;
      } catch (Exception ex) {
        LoggerFactory.getLogger("Combat Traces").warn("Cannot load {}: {}", file, ex.getMessage());
      }
    }
    return Map.copyOf(result);
  }

  @Override
  protected void apply(
      Map<ResourceLocation, JsonElement> result, ResourceManager manager, ProfilerFiller profiler) {
    apply.accept(result);
  }
}

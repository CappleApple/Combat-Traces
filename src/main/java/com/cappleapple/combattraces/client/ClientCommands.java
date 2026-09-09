package com.cappleapple.combattraces.client;

import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

public final class ClientCommands {
  public static void register(RegisterClientCommandsEvent event) {
    event
        .getDispatcher()
        .register(
            Commands.literal("combattraces")
                .then(
                    Commands.literal("debug")
                        .then(
                            Commands.literal("on")
                                .executes(
                                    ctx -> {
                                      DebugRenderer.enabled = true;
                                      com.cappleapple.combattraces.config.ClientConfig.DEBUG.set(
                                          true);
                                      return 1;
                                    }))
                        .then(
                            Commands.literal("off")
                                .executes(
                                    ctx -> {
                                      DebugRenderer.enabled = false;
                                      com.cappleapple.combattraces.config.ClientConfig.DEBUG.set(
                                          false);
                                      return 1;
                                    })))
                .then(
                    Commands.literal("reload")
                        .executes(
                            ctx -> {
                              Minecraft.getInstance().reloadResourcePacks();
                              ctx.getSource()
                                  .sendSuccess(
                                      () ->
                                          Component.literal(
                                              "Reloading"
                                                  + " Combat"
                                                  + " Traces"
                                                  + " client"
                                                  + " resources."
                                                  + " Server"
                                                  + " datapacks"
                                                  + " refresh"
                                                  + " with"
                                                  + " /reload."),
                                      false);
                              return 1;
                            })));
  }
}

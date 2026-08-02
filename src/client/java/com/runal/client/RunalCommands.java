package com.runal.client;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class RunalCommands {
    private RunalCommands() {
    }

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(createRoot("runal"));
            dispatcher.register(createRoot("ru"));
        });
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> createRoot(String name) {
        return ClientCommands.literal(name)
                .executes(context -> openScreen(new RunalScreen()))
                .then(ClientCommands.literal("gui")
                        .executes(context -> openScreen(new RunalScreen())))
                .then(ClientCommands.literal("edithud")
                        .executes(context -> openScreen(new HudEditorScreen())))
                .then(ClientCommands.literal("waypoints")
                        .executes(context -> openScreen(new WaypointManagerScreen())))
                .then(ClientCommands.literal("team")
                        .executes(context -> openScreen(new TeamTrackerScreen())))
                .then(ClientCommands.literal("help")
                        .executes(context -> showHelp()))
                .then(ClientCommands.literal("ping")
                        .executes(context -> showPing()))
                .then(ClientCommands.literal("version")
                        .executes(context -> showVersion()))
                .then(ClientCommands.literal("modules")
                        .executes(context -> showEnabledModules()))
                .then(ClientCommands.literal("reload")
                        .executes(context -> reloadConfig()))
                .then(ClientCommands.literal("toggle")
                        .then(moduleArgument().executes(context ->
                                toggleModule(StringArgumentType.getString(context, "module")))))
                .then(ClientCommands.literal("reset")
                        .then(ClientCommands.literal("module")
                                .then(moduleArgument().executes(context ->
                                        resetModule(StringArgumentType.getString(context, "module")))))
                        .then(ClientCommands.literal("gui")
                                .executes(context -> resetGui()))
                        .then(ClientCommands.literal("hud")
                                .executes(context -> resetHud()))
                        .then(ClientCommands.literal("all")
                                .executes(context -> resetAll())));
    }

    private static com.mojang.brigadier.builder.RequiredArgumentBuilder<FabricClientCommandSource, String> moduleArgument() {
        return ClientCommands.argument("module", StringArgumentType.word())
                .suggests((context, builder) -> {
                    for (Module module : ModuleManager.getModules()) {
                        builder.suggest(commandName(module));
                    }
                    return builder.buildFuture();
                });
    }

    private static int openScreen(net.minecraft.client.gui.screens.Screen screen) {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> mc.setScreen(screen));
        return 1;
    }

    private static int showHelp() {
        Message.commandInfo("Command Help:");
        Message.commandInfo("/runal, /ru - Open the ClickGUI");
        Message.commandInfo("/runal edithud - Open the HUD editor");
        Message.commandInfo("/runal waypoints - Open the waypoint manager");
        Message.commandInfo("/runal team - Open the team selector");
        Message.commandInfo("/runal ping - Show your current ping");
        Message.commandInfo("/runal modules - List enabled modules");
        Message.commandInfo("/runal toggle <module> - Toggle a module");
        Message.commandInfo("/runal reset module <module> - Reset one module's settings");
        Message.commandInfo("/runal reset <gui|hud|all> - Reset layouts/settings");
        Message.commandInfo("/runal reload - Reload runal.properties");
        Message.commandInfo("/runal version - Show the installed version");
        return 1;
    }

    private static int showPing() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.getConnection() == null) {
            Message.commandError("Ping is unavailable while disconnected.");
            return 0;
        }
        var info = mc.getConnection().getPlayerInfo(mc.player.getUUID());
        if (info == null) {
            Message.commandError("Ping is unavailable right now.");
            return 0;
        }
        Message.commandSuccess("Ping: " + info.getLatency() + "ms");
        return 1;
    }

    private static int showVersion() {
        String version = FabricLoader.getInstance().getModContainer("runal")
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElse("unknown");
        Message.commandSuccess("Version: " + version);
        return 1;
    }

    private static int showEnabledModules() {
        List<String> enabled = new ArrayList<>();
        for (Module module : ModuleManager.getModules()) {
            if (module.isEnabled()) enabled.add(module.getName());
        }
        Message.commandInfo(enabled.isEmpty()
                ? "No modules are enabled."
                : "Enabled modules (" + enabled.size() + "): " + String.join(", ", enabled));
        return 1;
    }

    private static int toggleModule(String input) {
        Module module = findModule(input);
        if (module == null) return moduleNotFound(input);
        module.toggle();
        ModuleConfig.save();
        Message.commandSuccess(module.getName() + (module.isEnabled() ? " enabled." : " disabled."));
        return 1;
    }

    private static int resetModule(String input) {
        Module module = findModule(input);
        if (module == null) return moduleNotFound(input);
        module.resetSettings();
        ModuleConfig.save();
        Message.commandSuccess("Reset " + module.getName() + " settings.");
        return 1;
    }

    private static int resetGui() {
        RunalScreen.resetPanelPositions();
        Message.commandSuccess("Reset ClickGUI positions.");
        return 1;
    }

    private static int resetHud() {
        resetHudPositions();
        ModuleConfig.save();
        Message.commandSuccess("Reset HUD positions.");
        return 1;
    }

    private static void resetHudPositions() {
        SessionManagerState.x = 8;
        SessionManagerState.y = 8;
        PerformanceHudState.x = 8;
        PerformanceHudState.y = 28;
        ArmorHudState.x = 8;
        ArmorHudState.y = 72;
        InventoryHudState.x = 8;
        InventoryHudState.y = 104;
        EventTrackerState.x = 8;
        EventTrackerState.y = 90;
        ItemCooldownHudState.x = 8;
        ItemCooldownHudState.y = 140;
        DungeonTrackerState.x = 8;
        DungeonTrackerState.y = 300;
        BossDefeatState.x = 8;
        BossDefeatState.y = 350;
        LowHealthWarning.lowTitleX = 400;
        LowHealthWarning.lowTitleY = 40;
        LowHealthWarning.midTitleX = 400;
        LowHealthWarning.midTitleY = 90;
        BossTitleState.x = Integer.MIN_VALUE;
        BossTitleState.y = Integer.MIN_VALUE;
    }

    private static int resetAll() {
        for (Module module : ModuleManager.getModules()) module.resetSettings();
        resetHudPositions();
        RunalScreen.resetPanelPositions();
        ModuleConfig.save();
        Message.commandSuccess("Reset all Runal settings and layouts.");
        return 1;
    }

    private static int reloadConfig() {
        ModuleConfig.load();
        Message.commandSuccess("Reloaded runal.properties.");
        return 1;
    }

    private static Module findModule(String input) {
        String wanted = normalize(input);
        for (Module module : ModuleManager.getModules()) {
            if (normalize(module.getName()).equals(wanted)) return module;
        }
        return null;
    }

    private static int moduleNotFound(String input) {
        Message.commandError("Unknown module: " + input.replace('_', ' '));
        return 0;
    }

    private static String commandName(Module module) {
        return normalize(module.getName());
    }

    private static String normalize(String value) {
        return value.toLowerCase(Locale.ROOT).trim().replace(' ', '_');
    }
}

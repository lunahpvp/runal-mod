package com.runal.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.runal.AutoSprintState;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
//? if 1.21.4 || 1.21.11 {
/*import net.fabricmc.fabric.api.client.keybinding.v1.KeyMappingHelper;
*///?} else {
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
//?}
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
//? if 26.1.2 {
import net.fabricmc.fabric.api.client.rendering.v1.PictureInPictureRendererRegistry;
//?}
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public class RunalClient implements ClientModInitializer {

	private static KeyMapping autoSprintKey;
	private static KeyMapping lowHealthToggleKey;
	private static KeyMapping fullbrightKey;
	private static KeyMapping hidePlayersKey;
	private static KeyMapping hideArmorKey;
	private static KeyMapping healthBarKey;
	private static KeyMapping playerScaleKey;
	private static KeyMapping hitboxesKey;
	private static KeyMapping teamTrackerKey;
	private static KeyMapping waypointManagerKey;
	private static KeyMapping newWaypointKey;
	private static KeyMapping scaleUpKey;
	private static KeyMapping scaleDownKey;
	private static KeyMapping openMenuKey;
	private static KeyMapping openMenuAltKey;
	private static KeyMapping autoGGKey;
	private static KeyMapping autoTrashKey;
	private static final KeyMapping[] hotbarSwapKeys = new KeyMapping[HotbarSwapState.SLOT_COUNT];
	private static final KeyMapping[] commandBindKeys = new KeyMapping[CommandBindState.SLOT_COUNT];

	//? if 1.21.4 {
	/*private static final String CATEGORY = "key.categories.runal.general";
	*///?} else {
	private static final KeyMapping.Category CATEGORY =
			KeyMapping.Category.register(Identifier.fromNamespaceAndPath("runal", "general"));
	//?}

	public static KeyMapping getAutoSprintKey() {
		return autoSprintKey;
	}

	public static KeyMapping getLowHealthToggleKey() {
		return lowHealthToggleKey;
	}

	public static KeyMapping getFullbrightKey() {
		return fullbrightKey;
	}

	public static KeyMapping getHidePlayersKey() {
		return hidePlayersKey;
	}

	public static KeyMapping getHideArmorKey() {
		return hideArmorKey;
	}

	public static KeyMapping getHealthBarKey() {
		return healthBarKey;
	}

	public static KeyMapping getPlayerScaleKey() {
		return playerScaleKey;
	}

	public static KeyMapping getHitboxesKey() {
		return hitboxesKey;
	}

	public static KeyMapping getTeamTrackerKey() {
		return teamTrackerKey;
	}

	public static KeyMapping getWaypointManagerKey() {
		return waypointManagerKey;
	}

	public static KeyMapping getNewWaypointKey() {
		return newWaypointKey;
	}

	public static KeyMapping getOpenMenuKey() {
		return openMenuKey;
	}

	public static KeyMapping getOpenMenuAltKey() {
		return openMenuAltKey;
	}

	public static KeyMapping getAutoGGKey() {
		return autoGGKey;
	}

	public static KeyMapping getAutoTrashKey() {
		return autoTrashKey;
	}

	public static KeyMapping getHotbarSwapKey(int index) {
		return hotbarSwapKeys[index];
	}

	public static KeyMapping getCommandBindKey(int index) {
		return commandBindKeys[index];
	}

	@Override
	public void onInitializeClient() {
		autoSprintKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.runal.autosprint",
				InputConstants.Type.KEYSYM,
				GLFW.GLFW_KEY_UNKNOWN,
				CATEGORY
		));

		lowHealthToggleKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.runal.lowhealthtoggle",
				InputConstants.Type.KEYSYM,
				GLFW.GLFW_KEY_UNKNOWN,
				CATEGORY
		));

		fullbrightKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.runal.fullbright",
				InputConstants.Type.KEYSYM,
				GLFW.GLFW_KEY_UNKNOWN,
				CATEGORY
		));

		hidePlayersKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.runal.hideplayers",
				InputConstants.Type.KEYSYM,
				GLFW.GLFW_KEY_UNKNOWN,
				CATEGORY
		));

		hideArmorKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.runal.hidearmor",
				InputConstants.Type.KEYSYM,
				GLFW.GLFW_KEY_UNKNOWN,
				CATEGORY
		));

		healthBarKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.runal.healthbar",
				InputConstants.Type.KEYSYM,
				GLFW.GLFW_KEY_UNKNOWN,
				CATEGORY
		));

		playerScaleKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.runal.playerscale",
				InputConstants.Type.KEYSYM,
				GLFW.GLFW_KEY_UNKNOWN,
				CATEGORY
		));

		scaleUpKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.runal.scaleup",
				InputConstants.Type.KEYSYM,
				GLFW.GLFW_KEY_UNKNOWN,
				CATEGORY
		));

		scaleDownKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.runal.scaledown",
				InputConstants.Type.KEYSYM,
				GLFW.GLFW_KEY_UNKNOWN,
				CATEGORY
		));

		openMenuKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.runal.openmenu",
				InputConstants.Type.KEYSYM,
				GLFW.GLFW_KEY_RIGHT_SHIFT,
				CATEGORY
		));

		openMenuAltKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.runal.openmenualt",
				InputConstants.Type.KEYSYM,
				GLFW.GLFW_KEY_LEFT_ALT,
				CATEGORY
		));

		hitboxesKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.runal.hitboxes",
				InputConstants.Type.KEYSYM,
				GLFW.GLFW_KEY_UNKNOWN,
				CATEGORY
		));

		teamTrackerKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.runal.teamtracker",
				InputConstants.Type.KEYSYM,
				GLFW.GLFW_KEY_UNKNOWN,
				CATEGORY
		));

		waypointManagerKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.runal.waypointmanager",
				InputConstants.Type.KEYSYM,
				GLFW.GLFW_KEY_UNKNOWN,
				CATEGORY
		));

		newWaypointKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.runal.newwaypoint",
				InputConstants.Type.KEYSYM,
				GLFW.GLFW_KEY_UNKNOWN,
				CATEGORY
		));

		for (int i = 0; i < HotbarSwapState.SLOT_COUNT; i++) {
			hotbarSwapKeys[i] = KeyMappingHelper.registerKeyMapping(new KeyMapping(
					"key.runal.hotbarswap" + (i + 1),
					InputConstants.Type.KEYSYM,
					GLFW.GLFW_KEY_UNKNOWN,
					CATEGORY
			));
		}

		for (int i = 0; i < CommandBindState.SLOT_COUNT; i++) {
			commandBindKeys[i] = KeyMappingHelper.registerKeyMapping(new KeyMapping(
					"key.runal.commandbind" + (i + 1),
					InputConstants.Type.KEYSYM,
					GLFW.GLFW_KEY_UNKNOWN,
					CATEGORY
			));
		}

		autoGGKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.runal.autogg",
				InputConstants.Type.KEYSYM,
				GLFW.GLFW_KEY_UNKNOWN,
				CATEGORY
		));

		autoTrashKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.runal.autotrash",
				InputConstants.Type.KEYSYM,
				GLFW.GLFW_KEY_UNKNOWN,
				CATEGORY
		));

		//? if 26.1.2 {
		// Without this, guiRenderState.addPicturesInPictureState(...) (called from
		// NVGPIPRenderer.draw, used by RunalScreen's NanoVG ClickGUI chrome) queues render
		// states that nothing ever processes - Minecraft needs to know which renderer
		// instance handles that state class before it'll actually draw anything.
		PictureInPictureRendererRegistry.register(context -> new NVGPIPRenderer(context.bufferSource()));
		//?}

		BuiltinModules.registerAll();
		ModuleConfig.load();
		TeamTrackerState.INSTANCE.load();
		WaypointManagerState.INSTANCE.load();
		BossDefeatState.load();

		LowHealthWarning.register();
		HealthBarRenderer.register();
		UtilityHudRenderer.register();
		HitboxRenderer.register();
		TeamTrackerRenderer.register();
		WaypointRenderer.register();
		AutoGGController.register();
		AutoTrashController.register();
		PartyController.register();
		EventTrackerController.register();
		RealPlayerTracker.register();
		BossTitleController.register();
		DiscordPresenceController.register();
		ArmorCooldownController.register();
		AccessoryCooldownController.register();
		DungeonTrackerController.register();
		BossDefeatController.register();

		ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
			ModuleConfig.save();
			TeamTrackerState.INSTANCE.save();
			WaypointManagerState.INSTANCE.save();
		});

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (autoSprintKey.consumeClick()) toggleAutoSprint(client);
			while (lowHealthToggleKey.consumeClick()) toggleLowHealth(client);
			while (fullbrightKey.consumeClick()) toggleFullbright(client);
			while (hidePlayersKey.consumeClick()) toggleHidePlayers(client);
			while (hideArmorKey.consumeClick()) toggleHideArmor(client);
			while (healthBarKey.consumeClick()) toggleHealthBar(client);
			while (playerScaleKey.consumeClick()) togglePlayerScale();
			while (hitboxesKey.consumeClick()) toggleHitboxes(client);
			while (teamTrackerKey.consumeClick()) toggleTeamTracker(client);
			while (waypointManagerKey.consumeClick()) client.setScreen(new WaypointManagerScreen());
			while (newWaypointKey.consumeClick()) createWaypointHere(client);

			while (scaleUpKey.consumeClick()) {
				float newScale = Math.min(PlayerScaleState.INSTANCE.getScale() + 0.1f, 3.0f);
				PlayerScaleState.INSTANCE.setScale(newScale);
				Message.success("Player scale: " + String.format("%.1f", newScale));
			}

			while (scaleDownKey.consumeClick()) {
				float newScale = Math.max(PlayerScaleState.INSTANCE.getScale() - 0.1f, 0.1f);
				PlayerScaleState.INSTANCE.setScale(newScale);
				Message.success("Player scale: " + String.format("%.1f", newScale));
			}

			while (openMenuKey.consumeClick()) {
				Minecraft.getInstance().setScreen(new RunalScreen());
			}

			while (openMenuAltKey.consumeClick()) {
				Minecraft.getInstance().setScreen(new RunalScreen());
			}

			for (int i = 0; i < HotbarSwapState.SLOT_COUNT; i++) {
				while (hotbarSwapKeys[i].consumeClick()) {
					HotbarSwapController.swap(HotbarSwapState.INSTANCE.rows[i]);
				}
			}

			for (int i = 0; i < CommandBindState.SLOT_COUNT; i++) {
				while (commandBindKeys[i].consumeClick()) {
					runCommandBind(client, CommandBindState.INSTANCE.commands[i]);
				}
			}

			while (autoGGKey.consumeClick()) toggleAutoGG(client);
			while (autoTrashKey.consumeClick()) toggleAutoTrash(client);
		});

		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
			Message.info("Press Right Shift or Left Alt to open the Runal menu");
		});
	}

	private void toggleAutoSprint(Minecraft client) {
		AutoSprintState.INSTANCE.toggle();
		ModuleConfig.save();
		messageToggle(client, "Auto Sprint", AutoSprintState.INSTANCE.isEnabled());
	}

	private void toggleLowHealth(Minecraft client) {
		LowHealthWarning.toggle();
		ModuleConfig.save();
		messageToggle(client, "Low Health Warning", LowHealthWarning.isEnabled());
	}

	private void toggleFullbright(Minecraft client) {
		FullbrightState.INSTANCE.toggle();
		ModuleConfig.save();
		messageToggle(client, "Fullbright", FullbrightState.INSTANCE.isEnabled());
	}

	private void toggleHidePlayers(Minecraft client) {
		HidePlayersState.INSTANCE.toggle();
		ModuleConfig.save();
		messageToggle(client, "Hide Players", HidePlayersState.INSTANCE.isEnabled());
	}

	private void toggleHideArmor(Minecraft client) {
		HideArmorState.INSTANCE.toggle();
		ModuleConfig.save();
		messageToggle(client, "Hide Armor", HideArmorState.INSTANCE.isEnabled());
	}

	private void toggleHealthBar(Minecraft client) {
		HealthBarState.INSTANCE.toggle();
		ModuleConfig.save();
		messageToggle(client, "Health Bar", HealthBarState.INSTANCE.isEnabled());
	}

	private void toggleHitboxes(Minecraft client) {
		HitboxesState.INSTANCE.toggle();
		ModuleConfig.save();
		messageToggle(client, "Hitboxes", HitboxesState.INSTANCE.isEnabled());
	}

	private void toggleTeamTracker(Minecraft client) {
		TeamTrackerState.INSTANCE.toggle();
		ModuleConfig.save();
		messageToggle(client, "Team Tracker", TeamTrackerState.INSTANCE.isEnabled());
	}

	private void toggleAutoGG(Minecraft client) {
		AutoGGState.INSTANCE.toggle();
		ModuleConfig.save();
		messageToggle(client, "Auto GG", AutoGGState.INSTANCE.isEnabled());
	}

	private void toggleAutoTrash(Minecraft client) {
		AutoTrashState.INSTANCE.toggle();
		ModuleConfig.save();
		messageToggle(client, "Auto Trash", AutoTrashState.INSTANCE.isEnabled());
	}

	private void runCommandBind(Minecraft client, String command) {
		if (client.player == null || client.getConnection() == null) return;
		String trimmed = command.trim();
		if (trimmed.isEmpty()) return;
		if (trimmed.startsWith("/")) trimmed = trimmed.substring(1);
		client.getConnection().sendCommand(trimmed);
	}

	private void createWaypointHere(Minecraft client) {
		if (client.player == null) return;
		String dimensionKey = WaypointManagerState.currentDimensionKey(client);
		Waypoint waypoint = WaypointManagerState.INSTANCE.create(
				"Waypoint",
				(int) client.player.getX(),
				(int) client.player.getY(),
				(int) client.player.getZ(),
				dimensionKey
		);
		client.setScreen(new WaypointEditScreen(null, waypoint));
	}

	private void togglePlayerScale() {
		boolean reset = Math.abs(PlayerScaleState.INSTANCE.getScale() - 1.0f) > 0.01f;
		PlayerScaleState.INSTANCE.setScale(reset ? 1.0f : 1.2f);
		Message.success("Player scale: " + String.format("%.1f", PlayerScaleState.INSTANCE.getScale()));
	}

	private void messageToggle(Minecraft client, String name, boolean enabled) {
		if (client.player == null) return;
		if (enabled) Message.success(name + " enabled");
		else Message.error(name + " disabled");
	}
}


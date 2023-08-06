package net.gordyjack.gordyjackbotinterface;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class BotInterface implements ModInitializer {
	public static final Logger LOGGER = LoggerFactory.getLogger("gordyjackbotinterface");
	private static MinecraftServer minecraftServer;
	private static ServerCommandSource source;

	@Override
	public void onInitialize() {
		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			minecraftServer = server;
			source = minecraftServer.getCommandSource()
					.withPosition(new Vec3d(0, 0,0))
					.withEntity(minecraftServer.getCommandSource().getPlayer())
					.withMaxLevel(4)
					.withSilent();
			startSocketServer();
		});
	}

	private void startSocketServer() {
		new Thread(() -> {
			try (ServerSocket serverSocket = new ServerSocket(8000)) {
				LOGGER.info("Server started");

				while (true) {
					Socket clientSocket = serverSocket.accept();
					BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
					String base_command = in.readLine();

					List<String> possiblePlayers = new ArrayList<>(Arrays.asList(minecraftServer.getPlayerNames()));
					possiblePlayers.addAll(Arrays.asList("@p", "@e", "@a"));
					String playerName = base_command.contains("set_player") ? base_command.substring(base_command.indexOf(' ')+1) : possiblePlayers.get(0);

					LOGGER.debug("possiblePlayers: " + possiblePlayers + "\nplayerName: " + playerName);

					LOGGER.info("Command received: " + base_command);
					if (base_command.startsWith("/")) {
						LOGGER.debug("Removing prefix");
						base_command = base_command.substring(1);
					}

					ServerPlayerEntity playerEntity = minecraftServer.getPlayerManager().getPlayer(possiblePlayers.get(0));
					if (playerEntity == null) {
						LOGGER.error("playerEntity is null. Continuing.");
						continue;
					}
					switch (base_command) {
						case "chorus" -> {
							LOGGER.info("Executing command: chorus");
							LOGGER.debug("Teleport result:" + ChatEffects.teleportLikeChorusFruit(playerEntity));
						}
						default -> {
							String command = "execute as " + playerEntity.getName().getString() + " at @s run " + base_command;
							LOGGER.info("Executing command: " + command);
							try {
								if (playerEntity != null) {
									playerEntity.sendMessage(Text.literal("Twitch chat executed: " + base_command));
								}
								minecraftServer.getCommandManager().executeWithPrefix(source, command);
							} catch (Exception e) {
								LOGGER.error("Error executing command: /" + command, e);
							}
						}
					}
				}
			} catch (IOException e) {
				LOGGER.error("Exception caught when trying to listen on port 8000 or listening for a connection", e);
			}
		}).start();
	}

	private static class ChatEffects {
		private static final int MAX_TRIES = 128;
		private static final Random random = new Random();

		private static boolean teleportLikeChorusFruit(PlayerEntity player, int... tries) {
			int tries_num = tries.length == 0 ? 0 : tries[0];
			LOGGER.debug("Starting teleport attempt: " + tries_num);
			if (tries_num >= MAX_TRIES) return false;

			// Get a random offset between -8 and 8 for x, y, and z
			int x = player.getBlockPos().getX() + random.nextInt(17) - 8;
			int y = player.getBlockPos().getY() + random.nextInt(17) - 8;
			int z = player.getBlockPos().getZ() + random.nextInt(17) - 8;

			if (isSafeToTeleport(player, new BlockPos(x, y, z))) {
				player.teleport(x, y, z);
				return true;  // Successful teleportation
			} else {
				return teleportLikeChorusFruit(player, tries_num + 1);
			}
		}

		private static boolean isSafeToTeleport(PlayerEntity player, BlockPos pos) {
			// Check the block and the block above are air (or replaceable) and the block below is solid
			World world = player.getWorld();
			LOGGER.debug(pos.toShortString()  + " | " + world.isAir(pos) + " | " + world.isAir(pos.up()) + " | " + world.isAir(pos.down()));
			return world.isAir(pos) && world.isAir(pos.up()) && !world.isAir(pos.down());
		}
	}
}

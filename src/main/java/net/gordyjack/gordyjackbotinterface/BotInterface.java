package net.gordyjack.gordyjackbotinterface;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import net.minecraft.util.math.Vec3d;
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
				boolean first = true;
				String player = "@p";

				while (true) {
					Socket clientSocket = serverSocket.accept();
					BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
					String base_command = in.readLine();

					List<String> possiblePlayers = new ArrayList<>(Arrays.asList(minecraftServer.getPlayerNames()));
					possiblePlayers.addAll(Arrays.asList("@p", "@e", "@a"));

					LOGGER.debug("possiblePlayers: " + possiblePlayers);
					if (first && possiblePlayers.contains(base_command)) {
						player = base_command;
						first = false;
						continue;
					}

					LOGGER.info("Command received: " + base_command);
					if (base_command.startsWith("/")) {
						base_command = base_command.substring(1);
					}

					String command = "execute as " + player + " at @s run " + base_command;
					LOGGER.info("Executing command: /" + command);

					try {
						ServerPlayerEntity playerEntity = minecraftServer.getPlayerManager().getPlayer(player);
						if (playerEntity != null) {
							playerEntity.sendMessage(Text.literal("Twitch chat executed: " + base_command));
						}
						minecraftServer.getCommandManager().executeWithPrefix(source, command);
					} catch (Exception e) {
						LOGGER.error("Error executing command: /" + command, e);
					}
				}
			} catch (IOException e) {
				LOGGER.error("Exception caught when trying to listen on port 8000 or listening for a connection", e);
			}
		}).start();
	}
}

package com.github.steventn96.dstudy;

import java.util.*;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.track.playback.NonAllocatingAudioFrameBuffer;

import discord4j.core.DiscordClientBuilder;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.VoiceState;
import discord4j.core.object.entity.channel.VoiceChannel;
import discord4j.discordjson.json.gateway.MessageCreate;
import discord4j.voice.AudioProvider;

public class DStudy {
	
	//imperative approach to execute commands when messages are created 
	interface Command {
	    void execute(MessageCreateEvent event);
	}
	
	//data structure to hold all of our commands in one place
	private static final Map<String, Command> commands = new HashMap<>();
	private static PomoTimer currentTimer;

	private static void timerFunc(Timer timer, MessageCreateEvent event, TimerTask task) {
		//parse input
		final String content = event.getMessage().getContent();
		final List<String> command = Arrays.asList(content.split(" "));
		int timerSeconds;
		try {
			timerSeconds = Integer.parseInt(command.get(1));
		}
		catch (NumberFormatException e) {
			event.getMessage().getChannel().block()
					.createMessage("argument [" + command.get(1) + "] not an integer").block();
			return;
		}
		catch (Exception e) {
			event.getMessage().getChannel().block()
					.createMessage("something went wrong [!timer #SECS]").block();
			return;
		}

		if (task == null)
			task = new TimerTask() {
				@Override
				public void run() {
					event.getMessage().getChannel().block()
							.createMessage(timerSeconds + "s timer done!").block();
				}
			};

		// execute task when timerSeconds * 1000 runs out
		timer.schedule(task, timerSeconds * 1000);
		event.getMessage().getChannel().block()
				.createMessage(timerSeconds + "s timer started").block();
	}

	private static GatewayDiscordClient initClient(String token) {
		final GatewayDiscordClient client = DiscordClientBuilder.create(token).build()
				.login()
				.block();

		//hooks up command system to Discord4J's event system
		//EventDispatcher has single method "on" that determines type of event dispatcher provides
		//after event happens, iterates through all commands and checks content of message starts
		///with a prefix + command checking against, and if so, execute the command
		client.getEventDispatcher().on(MessageCreateEvent.class)
				.subscribe(event -> {
					final String content = event.getMessage().getContent(); // 3.1 Message.getContent() is a String
					for (final Map.Entry<String, Command> entry : commands.entrySet()) {
						if (content.startsWith('!' + entry.getKey())) {
							entry.getValue().execute(event);
							break;
						}
					}
				});

		return client;
	}

	private static void initSimpleCommands() {
		// simple commands here
		commands.put("ping", event -> event.getMessage()
				.getChannel().block()
				.createMessage("Pong!").block());
		commands.put("swag", event -> event.getMessage()
				.getChannel().block()
				.createMessage("Yeet!").block());
	}

	private static void initPlayerCommands(AudioPlayerManager playerManager, AudioPlayer player, TrackScheduler scheduler) {
		commands.put("play", event -> {
			final String content = event.getMessage().getContent();
			final List<String> command = Arrays.asList(content.split(" "));
			playerManager.loadItem(command.get(1), scheduler);
		});

		commands.put("pause", event -> {
			player.setPaused(true);
			event.getMessage().getChannel().block()
					.createMessage("resume with [!resume]").block();
		});

		commands.put("resume", event -> {
			if (player.isPaused())
				player.setPaused(false);
			else if (player.getPlayingTrack() == null)
				event.getMessage().getChannel().block()
						.createMessage("no music paused, play with [!play]").block();
		});

		commands.put("stop", event ->
			player.stopTrack()
		);
	}

	public static void main(String[] args) {
		currentTimer = null;
		initSimpleCommands();

		/* begin complex audio commands -- we'll want to encapsulate these later probably */
		// Utilizing LavaPlayer, creates AudioPlayer instances and translates URLs to AudioTrack instances
		final AudioPlayerManager playerManager = new DefaultAudioPlayerManager();
		playerManager.getConfiguration().setFrameBufferFactory(NonAllocatingAudioFrameBuffer::new);
		//lets playerManager parse remote sources like youtube and other audio source links
		AudioSourceManagers.registerRemoteSources(playerManager);
		//creates AudioPlayer so Discord4J can receive the audio data
		final AudioPlayer player = playerManager.createPlayer();
		//creates LavaPlayerAudioProvider in next step
		AudioProvider provider = new LavaPlayerAudioProvider(player);

		final TrackScheduler scheduler = new TrackScheduler(player);

		initPlayerCommands(playerManager, player, scheduler);

		//bot is able to join voice channel to play music
		commands.put("join", event -> {
			final discord4j.core.object.entity.Member member = event.getMember().orElse(null);
			if (member != null) {
				final VoiceState voiceState = member.getVoiceState().block();
				if (voiceState != null) {
					final VoiceChannel channel = voiceState.getChannel().block();
					if (channel != null) {
						// join returns a VoiceConnection which would be required if we were
						// adding disconnection features, but for now we are just ignoring it.
						channel.join(spec -> spec.setProvider(provider)).block();
					}
				}
			}
		});

		// timer stuff
		// regular timer
		Timer timer = new Timer();
		commands.put("timer", event -> timerFunc(timer, event, null));
		// pause or resume music after timer; just experimental
		commands.put("toggletimer", event -> {
			if (player.getPlayingTrack() == null) {
				event.getMessage().getChannel().block()
						.createMessage("no music paused, play with [!play]").block();
				return;
			}
			timerFunc(timer, event, new TimerTask() {
				@Override
				public void run() {
					player.setPaused(!player.isPaused());
				}
			});
		});

		// pomo test commands
		commands.put("pomo", event -> {
			final String content = event.getMessage().getContent();
			final List<String> command = Arrays.asList(content.split(" "));
			try {
				if (currentTimer != null && !currentTimer.hasEnded()) {
					event.getMessage().getChannel().block().createMessage("Pomo Exists already").block();
					return;
				}
				currentTimer = new PomoTimer(command.get(1), event.getMessage().getChannel());
				currentTimer.startPomo();
			}
			catch (IllegalArgumentException e) {
				event.getMessage().getChannel().block().createMessage("Error Creating Pomo Object");
			}
		});

		commands.put("ppause", event -> currentTimer.pause());
		commands.put("presume", event -> currentTimer.resume());
		commands.put("pkill", event -> currentTimer.endPomo());

		/* create and connect the client -- args[0] has Discord key */
		final GatewayDiscordClient client = initClient(args[0]);

		client.onDisconnect().block();
	}

}

package com.github.steventn96.dstudy;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import discord4j.voice.AudioProvider;

public class DStudy {
	
	interface Command {
	    void execute(MessageCreateEvent event);
	}
	
	private static final Map<String, Command> commands = new HashMap<>();
	
	public static void main(String[] args) {
		
		final AudioPlayerManager playerManager = new DefaultAudioPlayerManager();
		playerManager.getConfiguration().setFrameBufferFactory(NonAllocatingAudioFrameBuffer::new);
		AudioSourceManagers.registerRemoteSources(playerManager);
		final AudioPlayer player = playerManager.createPlayer();
		AudioProvider provider = new LavaPlayerAudioProvider(player);
		
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
		
		final TrackScheduler scheduler = new TrackScheduler(player);
		commands.put("play", event -> {
		    final String content = event.getMessage().getContent();
		    final List<String> command = Arrays.asList(content.split(" "));
		    playerManager.loadItem(command.get(1), scheduler);
		});
		
		//args[0] has Discord key
		final GatewayDiscordClient client = DiscordClientBuilder.create(args[0]).build()
			    .login()
			    .block();
		
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
		
		client.onDisconnect().block();

	}
	
	static {
	    commands.put("ping", event -> event.getMessage()
	        .getChannel().block()
	        .createMessage("Pong!").block());
	}
	
	static {
	    commands.put("swag", event -> event.getMessage()
	        .getChannel().block()
	        .createMessage("Yeet!").block());
	}

}

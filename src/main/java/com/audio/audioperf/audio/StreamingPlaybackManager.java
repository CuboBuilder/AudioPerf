package com.audio.audioperf.audio;

import java.util.HashMap;
import java.util.Map;

public abstract class StreamingPlaybackManager {
    private final boolean isClient;
    private int currentId;
    private final Map<Integer, StreamingAudioPlayer> players = new HashMap<>();

    public StreamingPlaybackManager(boolean isClient) {
        this.isClient = isClient;
    }

    public abstract StreamingAudioPlayer create();

    public int newPlayer() {
        int id = currentId++;
        players.put(id, create());
        return id;
    }

    /**
     * Creates (or re-uses) a player registered under the given id. This is
     * used on the client side, where the codec id originates from the server.
     */
    public StreamingAudioPlayer createPlayer(int id) {
        StreamingAudioPlayer player = players.get(id);
        if (player == null) {
            player = create();
            players.put(id, player);
        }
        return player;
    }

    public void removePlayer(int id) {
        StreamingAudioPlayer player = players.remove(id);
        if (player != null) {
            player.stop();
        }
    }

    public StreamingAudioPlayer getPlayer(int id) {
        return players.get(id);
    }

    public boolean exists(int id) {
        return players.containsKey(id);
    }

    public void removeAll() {
        for (StreamingAudioPlayer player : players.values()) {
            player.stop();
        }
        players.clear();
    }
}

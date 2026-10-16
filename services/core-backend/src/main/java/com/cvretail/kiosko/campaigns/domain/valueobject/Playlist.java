package com.cvretail.kiosko.campaigns.domain.valueobject;

import com.cvretail.kiosko.campaigns.domain.exception.CampaignViolationException;
import com.cvretail.kiosko.campaigns.domain.exception.ViolationCodes;
import java.util.ArrayList;
import java.util.List;

/** Ordered list of at most 50 items, without gaps. Every operation returns a new playlist. */
public record Playlist(List<PlaylistItem> items) {

    public static final int MAX_ITEMS = 50;

    public Playlist {
        Require.notNull(items, "playlist");
        items = List.copyOf(items);
        if (items.size() > MAX_ITEMS) {
            throw new CampaignViolationException("playlist", ViolationCodes.PLAYLIST_TOO_MANY_ITEMS,
                    "a playlist has at most " + MAX_ITEMS + " items");
        }
    }

    public static Playlist empty() {
        return new Playlist(List.of());
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    public int size() {
        return items.size();
    }

    public PlaylistItem get(int position) {
        requirePosition(position, items.size() - 1);
        return items.get(position);
    }

    public Playlist add(PlaylistItem item) {
        return insert(items.size(), item);
    }

    public Playlist insert(int position, PlaylistItem item) {
        Require.notNull(item, "item");
        requirePosition(position, items.size());
        List<PlaylistItem> copy = new ArrayList<>(items);
        copy.add(position, item);
        return new Playlist(copy);
    }

    public Playlist remove(int position) {
        requirePosition(position, items.size() - 1);
        List<PlaylistItem> copy = new ArrayList<>(items);
        copy.remove(position);
        return new Playlist(copy);
    }

    public Playlist move(int from, int to) {
        requirePosition(from, items.size() - 1);
        requirePosition(to, items.size() - 1);
        List<PlaylistItem> copy = new ArrayList<>(items);
        copy.add(to, copy.remove(from));
        return new Playlist(copy);
    }

    public Playlist withDuration(int position, int seconds) {
        PlaylistItem current = get(position);
        List<PlaylistItem> copy = new ArrayList<>(items);
        copy.set(position, new PlaylistItem(current.media(), seconds));
        return new Playlist(copy);
    }

    private static void requirePosition(int position, int max) {
        if (position < 0 || position > max) {
            throw new CampaignViolationException("position", ViolationCodes.PLAYLIST_POSITION_OUT_OF_RANGE,
                    "position " + position + " is outside the playlist");
        }
    }
}

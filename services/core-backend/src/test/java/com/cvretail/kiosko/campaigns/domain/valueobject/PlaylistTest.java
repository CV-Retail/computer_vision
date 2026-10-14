package com.cvretail.kiosko.campaigns.domain.valueobject;

import static com.cvretail.kiosko.campaigns.support.CampaignBuilder.item;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cvretail.kiosko.campaigns.domain.exception.CampaignViolationException;
import com.cvretail.kiosko.campaigns.domain.exception.ViolationCodes;
import com.cvretail.kiosko.campaigns.support.CampaignBuilder;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PlaylistTest {

    private static Playlist of(int count) {
        Playlist playlist = Playlist.empty();
        for (int i = 1; i <= count; i++) {
            playlist = playlist.add(item(i));
        }
        return playlist;
    }

    private static void assertCode(Runnable action, String code) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(CampaignViolationException.class,
                e -> assertThat(e.hasCode(code)).isTrue());
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 3600})
    void itemDurationInRangeIsAccepted(int seconds) {
        assertThat(new PlaylistItem(CampaignBuilder.media(1), seconds).durationSeconds()).isEqualTo(seconds);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -5, 3601})
    void itemDurationOutOfRangeIsRejected(int seconds) {
        assertCode(() -> new PlaylistItem(CampaignBuilder.media(1), seconds),
                ViolationCodes.PLAYLIST_ITEM_DURATION_OUT_OF_RANGE);
    }

    @Test
    void addAppendsAndInsertPlacesAtPosition() {
        Playlist playlist = of(2).insert(1, item(9));
        assertThat(playlist.items()).containsExactly(item(1), item(9), item(2));
        assertThat(of(2).insert(2, item(9)).get(2)).isEqualTo(item(9));
    }

    @Test
    void removeAndMoveKeepTheOrderWithoutGaps() {
        assertThat(of(3).remove(1).items()).containsExactly(item(1), item(3));
        assertThat(of(3).move(0, 2).items()).containsExactly(item(2), item(3), item(1));
        assertThat(of(3).move(2, 0).items()).containsExactly(item(3), item(1), item(2));
    }

    @Test
    void sameMediaMayAppearTwice() {
        assertThat(of(1).add(item(1)).size()).isEqualTo(2);
    }

    @Test
    void withDurationReplacesOnlyTheDuration() {
        Playlist changed = of(2).withDuration(1, 30);
        assertThat(changed.get(1).durationSeconds()).isEqualTo(30);
        assertThat(changed.get(1).media()).isEqualTo(item(2).media());
        assertCode(() -> of(2).withDuration(1, 0), ViolationCodes.PLAYLIST_ITEM_DURATION_OUT_OF_RANGE);
    }

    @Test
    void positionsOutsideThePlaylistAreRejected() {
        assertCode(() -> of(2).insert(3, item(5)), ViolationCodes.PLAYLIST_POSITION_OUT_OF_RANGE);
        assertCode(() -> of(2).insert(-1, item(5)), ViolationCodes.PLAYLIST_POSITION_OUT_OF_RANGE);
        assertCode(() -> of(2).remove(2), ViolationCodes.PLAYLIST_POSITION_OUT_OF_RANGE);
        assertCode(() -> Playlist.empty().remove(0), ViolationCodes.PLAYLIST_POSITION_OUT_OF_RANGE);
        assertCode(() -> of(2).move(0, 2), ViolationCodes.PLAYLIST_POSITION_OUT_OF_RANGE);
        assertCode(() -> of(2).move(2, 0), ViolationCodes.PLAYLIST_POSITION_OUT_OF_RANGE);
        assertCode(() -> of(2).withDuration(5, 10), ViolationCodes.PLAYLIST_POSITION_OUT_OF_RANGE);
    }

    @Test
    void holdsAtMost50Items() {
        assertThat(of(50).size()).isEqualTo(50);
        assertCode(() -> of(50).add(item(51)), ViolationCodes.PLAYLIST_TOO_MANY_ITEMS);
        List<PlaylistItem> tooMany = new ArrayList<>(of(50).items());
        tooMany.add(item(51));
        assertCode(() -> new Playlist(tooMany), ViolationCodes.PLAYLIST_TOO_MANY_ITEMS);
    }

    @Test
    void operationsDoNotChangeTheOriginal() {
        Playlist original = of(2);
        original.add(item(3));
        original.remove(0);
        assertThat(original.items()).containsExactly(item(1), item(2));
    }
}

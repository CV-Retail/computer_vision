package com.cvretail.kiosko.campaigns.domain.exception;

import static com.cvretail.kiosko.campaigns.support.CampaignBuilder.aDefaultCampaign;
import static com.cvretail.kiosko.campaigns.support.CampaignBuilder.aRegularCampaign;
import static com.cvretail.kiosko.campaigns.support.CampaignBuilder.item;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cvretail.kiosko.campaigns.domain.entity.Campaign;
import com.cvretail.kiosko.campaigns.domain.valueobject.AudienceRuleId;
import com.cvretail.kiosko.campaigns.domain.valueobject.CampaignName;
import com.cvretail.kiosko.campaigns.domain.valueobject.CampaignStatus;
import com.cvretail.kiosko.campaigns.domain.valueobject.KioskId;
import com.cvretail.kiosko.campaigns.domain.valueobject.MediaRef;
import com.cvretail.kiosko.campaigns.domain.valueobject.MinimumOnScreenTime;
import com.cvretail.kiosko.campaigns.domain.valueobject.Playlist;
import com.cvretail.kiosko.campaigns.domain.valueobject.PlaylistItem;
import com.cvretail.kiosko.campaigns.domain.valueobject.Priority;
import com.cvretail.kiosko.campaigns.domain.valueobject.Schedule;
import com.cvretail.kiosko.campaigns.domain.valueobject.Scope;
import com.cvretail.kiosko.campaigns.domain.valueobject.StoreId;
import com.cvretail.kiosko.campaigns.domain.valueobject.TimeSlot;
import com.cvretail.kiosko.campaigns.domain.valueobject.VetoId;
import java.lang.reflect.Modifier;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/** Every code of the catalog is raised by a real action, so a code cannot be dropped or orphaned silently. */
class ViolationCodesTest {

    private static final Map<String, Runnable> TRIGGERS = Map.ofEntries(
            Map.entry(ViolationCodes.STORE_INVALID, () -> new StoreId("bad store")),
            Map.entry(ViolationCodes.KIOSK_INVALID, () -> new KioskId("")),
            Map.entry(ViolationCodes.MEDIA_REF_INVALID, () -> new MediaRef("sha256:short")),
            Map.entry(ViolationCodes.NAME_BLANK, () -> new CampaignName(" ")),
            Map.entry(ViolationCodes.NAME_TOO_LONG, () -> new CampaignName("x".repeat(101))),
            Map.entry(ViolationCodes.MINIMUM_ON_SCREEN_OUT_OF_RANGE, () -> new MinimumOnScreenTime(0)),
            Map.entry(ViolationCodes.PLAYLIST_ITEM_DURATION_OUT_OF_RANGE, () -> new PlaylistItem(item(1).media(), 0)),
            Map.entry(ViolationCodes.PLAYLIST_TOO_MANY_ITEMS,
                    () -> new Playlist(java.util.Collections.nCopies(51, item(1)))),
            Map.entry(ViolationCodes.PLAYLIST_POSITION_OUT_OF_RANGE, () -> Playlist.empty().remove(0)),
            Map.entry(ViolationCodes.SCHEDULE_END_BEFORE_START, () -> Schedule.always().withDates(
                    Optional.of(LocalDate.of(2030, 2, 1)), Optional.of(LocalDate.of(2030, 1, 1)))),
            Map.entry(ViolationCodes.SCHEDULE_DAYS_EMPTY, () -> Schedule.always().withDays(Set.of())),
            Map.entry(ViolationCodes.SCHEDULE_SLOT_EMPTY_RANGE, () -> new TimeSlot(LocalTime.NOON, LocalTime.NOON)),
            Map.entry(ViolationCodes.SCOPE_KIOSKS_EMPTY, () -> new Scope.SelectedKiosks(Set.of())),
            Map.entry(ViolationCodes.SCOPE_KIOSKS_DUPLICATE, () -> Scope.SelectedKiosks.of(
                    List.of(new KioskId("a"), new KioskId("a")))),
            Map.entry(ViolationCodes.VETO_DUPLICATE, () -> {
                Campaign c = aRegularCampaign().build();
                VetoId veto = new VetoId(UUID.randomUUID());
                c.addManualVeto(veto);
                c.addManualVeto(veto);
            }),
            Map.entry(ViolationCodes.VETO_NOT_FOUND,
                    () -> aRegularCampaign().build().removeManualVeto(new VetoId(UUID.randomUUID()))),
            Map.entry(ViolationCodes.PRIORITY_NOT_ALLOWED_FOR_DEFAULT,
                    () -> aDefaultCampaign().build().changePriority(Priority.LOW)),
            Map.entry(ViolationCodes.AUDIENCE_RULE_NOT_ALLOWED_FOR_DEFAULT,
                    () -> aDefaultCampaign().build().assignAudienceRule(new AudienceRuleId(UUID.randomUUID()))),
            Map.entry(ViolationCodes.MINIMUM_ON_SCREEN_NOT_ALLOWED_FOR_DEFAULT,
                    () -> aDefaultCampaign().build().changeMinimumOnScreenTime(MinimumOnScreenTime.defaultValue())),
            Map.entry(ViolationCodes.VETOES_NOT_ALLOWED_FOR_DEFAULT,
                    () -> aDefaultCampaign().build().addManualVeto(new VetoId(UUID.randomUUID()))),
            Map.entry(ViolationCodes.SCHEDULE_NOT_CHANGEABLE_FOR_DEFAULT, () -> aDefaultCampaign().build()
                    .reschedule(Schedule.always().withDays(Set.of(DayOfWeek.MONDAY)))),
            Map.entry(ViolationCodes.PLAYLIST_EMPTY, () -> aRegularCampaign().withAudienceRule().build().activate()),
            Map.entry(ViolationCodes.AUDIENCE_RULE_REQUIRED,
                    () -> aRegularCampaign().withPlaylistItems(1).build().activate()),
            Map.entry(ViolationCodes.SCHEDULE_START_DATE_REQUIRED, () -> aRegularCampaign().withPlaylistItems(1)
                    .withAudienceRule().build().schedule()),
            Map.entry(ViolationCodes.STATUS_TRANSITION_NOT_ALLOWED, () -> aRegularCampaign().build().pause()),
            Map.entry(ViolationCodes.STATUS_FINISHED,
                    () -> aRegularCampaign().inStatus(CampaignStatus.FINISHED).build().rename(new CampaignName("x"))),
            Map.entry(ViolationCodes.PRIORITY_REQUIRED, () -> Campaign.restore(
                    aRegularCampaign().build().snapshot().withPriority(Optional.empty()))),
            Map.entry(ViolationCodes.MINIMUM_ON_SCREEN_REQUIRED, () -> Campaign.restore(
                    aRegularCampaign().build().snapshot().withMinimumOnScreenTime(Optional.empty()))));

    private static Set<String> catalog() {
        return java.util.Arrays.stream(ViolationCodes.class.getDeclaredFields())
                .filter(f -> Modifier.isStatic(f.getModifiers()) && f.getType() == String.class)
                .map(f -> {
                    try {
                        return (String) f.get(null);
                    } catch (IllegalAccessException e) {
                        throw new IllegalStateException(e);
                    }
                })
                .collect(Collectors.toSet());
    }

    @Test
    void everyCodeOfTheCatalogHasATrigger() {
        assertThat(TRIGGERS.keySet()).isEqualTo(catalog());
    }

    @Test
    void everyTriggerRaisesItsCode() {
        TRIGGERS.forEach((code, trigger) -> assertThatThrownBy(trigger::run)
                .as("trigger of %s", code)
                .isInstanceOfSatisfying(CampaignViolationException.class,
                        e -> assertThat(e.hasCode(code)).as("codes of %s: %s", code, e.violations()).isTrue()));
    }

    @Test
    void codesAreUniqueLowercaseIdentifiers() {
        Set<String> seen = new HashSet<>();
        for (String code : catalog()) {
            assertThat(code).matches("[a-z_]+(\\.[a-z_]+)+");
            assertThat(seen.add(code)).isTrue();
        }
    }

    @Test
    void aMissingValueIsReportedAsFieldDotRequired() {
        assertThat(ViolationCodes.required("name")).isEqualTo("name.required");
        assertThatThrownBy(() -> new CampaignName(null)).isInstanceOfSatisfying(CampaignViolationException.class,
                e -> assertThat(e.violations().get(0).field()).isEqualTo("name"));
    }

    @Test
    void anExceptionNeedsAtLeastOneViolation() {
        assertThatThrownBy(() -> new CampaignViolationException(List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

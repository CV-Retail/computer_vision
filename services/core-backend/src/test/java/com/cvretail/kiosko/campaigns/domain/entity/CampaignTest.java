package com.cvretail.kiosko.campaigns.domain.entity;

import static com.cvretail.kiosko.campaigns.support.CampaignBuilder.aDefaultCampaign;
import static com.cvretail.kiosko.campaigns.support.CampaignBuilder.aRegularCampaign;
import static com.cvretail.kiosko.campaigns.support.CampaignBuilder.item;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cvretail.kiosko.campaigns.domain.event.CampaignActivated;
import com.cvretail.kiosko.campaigns.domain.event.CampaignCreated;
import com.cvretail.kiosko.campaigns.domain.event.CampaignFinished;
import com.cvretail.kiosko.campaigns.domain.event.CampaignPaused;
import com.cvretail.kiosko.campaigns.domain.event.CampaignScheduled;
import com.cvretail.kiosko.campaigns.domain.exception.CampaignViolationException;
import com.cvretail.kiosko.campaigns.domain.exception.ViolationCodes;
import com.cvretail.kiosko.campaigns.domain.valueobject.AudienceRuleId;
import com.cvretail.kiosko.campaigns.domain.valueobject.CampaignId;
import com.cvretail.kiosko.campaigns.domain.valueobject.CampaignName;
import com.cvretail.kiosko.campaigns.domain.valueobject.CampaignSnapshot;
import com.cvretail.kiosko.campaigns.domain.valueobject.CampaignStatus;
import com.cvretail.kiosko.campaigns.domain.valueobject.CampaignType;
import com.cvretail.kiosko.campaigns.domain.valueobject.KioskId;
import com.cvretail.kiosko.campaigns.domain.valueobject.MinimumOnScreenTime;
import com.cvretail.kiosko.campaigns.domain.valueobject.Priority;
import com.cvretail.kiosko.campaigns.domain.valueobject.Schedule;
import com.cvretail.kiosko.campaigns.domain.valueobject.Scope;
import com.cvretail.kiosko.campaigns.domain.valueobject.StoreId;
import com.cvretail.kiosko.campaigns.domain.valueobject.VetoId;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CampaignTest {

    private static void assertCode(Runnable action, String code) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(CampaignViolationException.class,
                e -> assertThat(e.hasCode(code)).isTrue());
    }

    @Test
    void aNewRegularCampaignIsADraftWithDefaults() {
        Campaign c = Campaign.createRegular(CampaignId.generate(), new StoreId("s1"), new CampaignName("A"));
        assertThat(c.status()).isEqualTo(CampaignStatus.DRAFT);
        assertThat(c.type()).isEqualTo(CampaignType.REGULAR);
        assertThat(c.priority()).contains(Priority.MEDIUM);
        assertThat(c.minimumOnScreenTime()).contains(MinimumOnScreenTime.defaultValue());
        assertThat(c.scope()).isEqualTo(Scope.all());
        assertThat(c.runSchedule()).isEqualTo(Schedule.always());
        assertThat(c.playlist().isEmpty()).isTrue();
        assertThat(c.audienceRuleId()).isEmpty();
        assertThat(c.manualVetoes()).isEmpty();
    }

    @Test
    void aNewDefaultCampaignHasNoRegularOnlyFields() {
        Campaign c = aDefaultCampaign().build();
        assertThat(c.type()).isEqualTo(CampaignType.DEFAULT);
        assertThat(c.priority()).isEmpty();
        assertThat(c.minimumOnScreenTime()).isEmpty();
        assertThat(c.audienceRuleId()).isEmpty();
    }

    @Test
    void equalityIsByIdentity() {
        CampaignId id = CampaignId.generate();
        Campaign one = aRegularCampaign().withId(id).build();
        Campaign other = aRegularCampaign().withId(id).named("Other name").build();
        assertThat(one).isEqualTo(other).hasSameHashCodeAs(other);
        assertThat(one).isNotEqualTo(aRegularCampaign().build());
    }

    @Test
    void editingChangesTheFields() {
        Campaign c = aRegularCampaign().build();
        AudienceRuleId rule = new AudienceRuleId(UUID.randomUUID());
        VetoId veto = new VetoId(UUID.randomUUID());
        c.rename(new CampaignName("New"));
        c.changePriority(Priority.HIGH);
        c.assignAudienceRule(rule);
        c.changeMinimumOnScreenTime(new MinimumOnScreenTime(20));
        c.changeScope(Scope.SelectedKiosks.of(List.of(new KioskId("k1"))));
        c.addManualVeto(veto);
        c.addPlaylistItem(item(1));
        c.insertPlaylistItem(0, item(2));
        c.movePlaylistItem(0, 1);
        c.changePlaylistItemDuration(0, 33);
        assertThat(c.name().value()).isEqualTo("New");
        assertThat(c.priority()).contains(Priority.HIGH);
        assertThat(c.audienceRuleId()).contains(rule);
        assertThat(c.minimumOnScreenTime()).contains(new MinimumOnScreenTime(20));
        assertThat(c.manualVetoes()).containsExactly(veto);
        assertThat(c.playlist().items()).extracting("durationSeconds").containsExactly(33, 10);
        c.removeManualVeto(veto);
        c.clearAudienceRule();
        c.removePlaylistItem(0);
        assertThat(c.manualVetoes()).isEmpty();
        assertThat(c.audienceRuleId()).isEmpty();
        assertThat(c.playlist().size()).isEqualTo(1);
    }

    @Test
    void theSameVetoTwiceAndAnUnknownVetoAreRejected() {
        Campaign c = aRegularCampaign().build();
        VetoId veto = new VetoId(UUID.randomUUID());
        c.addManualVeto(veto);
        assertCode(() -> c.addManualVeto(veto), ViolationCodes.VETO_DUPLICATE);
        assertCode(() -> c.removeManualVeto(new VetoId(UUID.randomUUID())), ViolationCodes.VETO_NOT_FOUND);
    }

    @Test
    void aDefaultCampaignRejectsRegularOnlyEdits() {
        Campaign c = aDefaultCampaign().build();
        assertCode(() -> c.changePriority(Priority.LOW), ViolationCodes.PRIORITY_NOT_ALLOWED_FOR_DEFAULT);
        assertCode(() -> c.assignAudienceRule(new AudienceRuleId(UUID.randomUUID())),
                ViolationCodes.AUDIENCE_RULE_NOT_ALLOWED_FOR_DEFAULT);
        assertCode(() -> c.changeMinimumOnScreenTime(MinimumOnScreenTime.defaultValue()),
                ViolationCodes.MINIMUM_ON_SCREEN_NOT_ALLOWED_FOR_DEFAULT);
        assertCode(() -> c.addManualVeto(new VetoId(UUID.randomUUID())), ViolationCodes.VETOES_NOT_ALLOWED_FOR_DEFAULT);
        assertCode(() -> c.reschedule(Schedule.always().withDates(Optional.of(LocalDate.of(2030, 1, 1)), Optional.empty())),
                ViolationCodes.SCHEDULE_NOT_CHANGEABLE_FOR_DEFAULT);
        assertThat(c.snapshot()).isEqualTo(aDefaultCampaign().withId(c.id()).named(c.name().value()).build().snapshot());
    }

    @Test
    void transitionsRecordTheirEventsInOrderAndPullForgetsThem() {
        CampaignId id = CampaignId.generate();
        StoreId store = new StoreId("s1");
        Campaign c = Campaign.createRegular(id, store, new CampaignName("A"));
        c.addPlaylistItem(item(1));
        c.assignAudienceRule(new AudienceRuleId(UUID.randomUUID()));
        c.reschedule(Schedule.always().withDates(Optional.of(LocalDate.of(2030, 1, 1)), Optional.empty()));
        c.schedule();
        c.activate();
        c.pause();
        c.activate();
        c.finish();
        assertThat(c.pullEvents()).containsExactly(
                new CampaignCreated(id, store, CampaignType.REGULAR),
                new CampaignScheduled(id, store),
                new CampaignActivated(id, store),
                new CampaignPaused(id, store),
                new CampaignActivated(id, store),
                new CampaignFinished(id, store));
        assertThat(c.pullEvents()).isEmpty();
    }

    @Test
    void aRejectedChangeLeavesStateAndEventsUntouchedForEveryMutator() {
        Campaign c = aRegularCampaign().inStatus(CampaignStatus.ACTIVE).build();
        CampaignSnapshot before = c.snapshot();
        List<Runnable> rejected = List.of(
                () -> c.removePlaylistItem(0),
                () -> c.removePlaylistItem(5),
                () -> c.clearAudienceRule(),
                () -> c.insertPlaylistItem(9, item(2)),
                () -> c.movePlaylistItem(0, 4),
                () -> c.changePlaylistItemDuration(0, 0),
                () -> c.removeManualVeto(new VetoId(UUID.randomUUID())),
                () -> c.schedule(),
                () -> c.activate());
        for (Runnable action : rejected) {
            assertThatThrownBy(action::run).isInstanceOf(CampaignViolationException.class);
            assertThat(c.snapshot()).isEqualTo(before);
            assertThat(c.pullEvents()).isEmpty();
        }
    }

    @Test
    void aFinishedCampaignAcceptsNoChangeAndNoTransition() {
        Campaign c = aRegularCampaign().inStatus(CampaignStatus.FINISHED).build();
        List<Runnable> actions = List.of(
                () -> c.rename(new CampaignName("X")),
                () -> c.changePriority(Priority.LOW),
                () -> c.addPlaylistItem(item(7)),
                () -> c.changeScope(Scope.all()),
                () -> c.activate(),
                () -> c.pause(),
                () -> c.finish(),
                () -> c.schedule());
        for (Runnable action : actions) {
            assertCode(action, ViolationCodes.STATUS_FINISHED);
        }
        assertThat(c.pullEvents()).isEmpty();
    }

    @Test
    void anIncompleteCampaignCannotBeScheduledOrActivated() {
        Campaign c = aRegularCampaign().build();
        assertCode(c::activate, ViolationCodes.PLAYLIST_EMPTY);
        assertCode(c::schedule, ViolationCodes.AUDIENCE_RULE_REQUIRED);
        assertThat(c.status()).isEqualTo(CampaignStatus.DRAFT);
    }

    @Test
    void aPausedCampaignMayBecomeIncompleteButNotBeReactivated() {
        Campaign c = aRegularCampaign().inStatus(CampaignStatus.PAUSED).build();
        c.removePlaylistItem(0);
        assertCode(c::activate, ViolationCodes.PLAYLIST_EMPTY);
        assertThat(c.status()).isEqualTo(CampaignStatus.PAUSED);
    }

    @Test
    void aScheduledCampaignCannotLoseItsStartDate() {
        Campaign c = aRegularCampaign().inStatus(CampaignStatus.SCHEDULED).build();
        assertCode(() -> c.reschedule(Schedule.always()), ViolationCodes.SCHEDULE_START_DATE_REQUIRED);
    }
}

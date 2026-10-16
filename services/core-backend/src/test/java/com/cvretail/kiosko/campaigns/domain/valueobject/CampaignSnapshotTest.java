package com.cvretail.kiosko.campaigns.domain.valueobject;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cvretail.kiosko.campaigns.domain.entity.Campaign;
import com.cvretail.kiosko.campaigns.domain.exception.CampaignViolationException;
import com.cvretail.kiosko.campaigns.domain.exception.ViolationCodes;
import com.cvretail.kiosko.campaigns.support.CampaignBuilder;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CampaignSnapshotTest {

    private final CampaignSnapshot base = CampaignBuilder.aRegularCampaign().build().snapshot();

    @Test
    void withMethodsReturnAModifiedCopyAndKeepTheOriginal() {
        CampaignSnapshot renamed = base.withName(new CampaignName("Other"));
        assertThat(renamed.name().value()).isEqualTo("Other");
        assertThat(base.name().value()).isEqualTo("Summer sale");
        assertThat(renamed.withName(base.name())).isEqualTo(base);
        assertThat(base.withStatus(CampaignStatus.PAUSED).status()).isEqualTo(CampaignStatus.PAUSED);
        assertThat(base.withPriority(Optional.of(Priority.HIGH)).priority()).contains(Priority.HIGH);
        assertThat(base.withPlaylist(Playlist.empty().add(CampaignBuilder.item(1))).playlist().size()).isEqualTo(1);
        assertThat(base.withScope(new Scope.AllKiosks()).scope()).isEqualTo(Scope.all());
        assertThat(base.withSchedule(Schedule.always())).isEqualTo(base);
        AudienceRuleId rule = new AudienceRuleId(UUID.randomUUID());
        assertThat(base.withAudienceRuleId(Optional.of(rule)).audienceRuleId()).contains(rule);
        assertThat(base.withMinimumOnScreenTime(Optional.empty()).minimumOnScreenTime()).isEmpty();
        VetoId veto = new VetoId(UUID.randomUUID());
        assertThat(base.withManualVetoes(Set.of(veto)).manualVetoes()).containsExactly(veto);
    }

    @Test
    void requiredComponentsCannotBeNull() {
        assertThatThrownBy(() -> base.withName(null)).hasMessageContaining("name.required");
        assertThatThrownBy(() -> base.withPlaylist(null)).hasMessageContaining("playlist.required");
        assertThatThrownBy(() -> base.withStatus(null)).hasMessageContaining("status.required");
    }

    @Test
    void restoreRoundTripsAValidSnapshot() {
        Campaign campaign = CampaignBuilder.aRegularCampaign().inStatus(CampaignStatus.ACTIVE).build();
        Campaign restored = Campaign.restore(campaign.snapshot());
        assertThat(restored.snapshot()).isEqualTo(campaign.snapshot());
        assertThat(restored.pullEvents()).isEmpty();
    }

    @Test
    void restoreRejectsInvalidPersistedStates() {
        CampaignSnapshot activeWithoutPlaylist = base.withStatus(CampaignStatus.ACTIVE);
        assertThatThrownBy(() -> Campaign.restore(activeWithoutPlaylist)).isInstanceOfSatisfying(
                CampaignViolationException.class,
                e -> assertThat(e.hasCode(ViolationCodes.PLAYLIST_EMPTY)).isTrue());
        CampaignSnapshot defaultWithPriority =
                CampaignBuilder.aDefaultCampaign().build().snapshot().withPriority(Optional.of(Priority.LOW));
        assertThatThrownBy(() -> Campaign.restore(defaultWithPriority)).isInstanceOf(CampaignViolationException.class);
        assertThatThrownBy(() -> Campaign.restore(null)).isInstanceOf(NullPointerException.class);
    }
}

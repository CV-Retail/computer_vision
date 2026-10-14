package com.cvretail.kiosko.campaigns.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cvretail.kiosko.campaigns.domain.exception.CampaignViolationException;
import com.cvretail.kiosko.campaigns.domain.exception.ViolationCodes;
import com.cvretail.kiosko.campaigns.domain.valueobject.AudienceRuleId;
import com.cvretail.kiosko.campaigns.domain.valueobject.CampaignSnapshot;
import com.cvretail.kiosko.campaigns.domain.valueobject.CampaignStatus;
import com.cvretail.kiosko.campaigns.domain.valueobject.MinimumOnScreenTime;
import com.cvretail.kiosko.campaigns.domain.valueobject.Playlist;
import com.cvretail.kiosko.campaigns.domain.valueobject.Priority;
import com.cvretail.kiosko.campaigns.domain.valueobject.Schedule;
import com.cvretail.kiosko.campaigns.domain.valueobject.VetoId;
import com.cvretail.kiosko.campaigns.support.CampaignBuilder;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CampaignInvariantsTest {

    private final CampaignSnapshot regular = CampaignBuilder.aRegularCampaign().build().snapshot();
    private final CampaignSnapshot defaultCampaign = CampaignBuilder.aDefaultCampaign().build().snapshot();

    private static List<String> codes(CampaignSnapshot state) {
        try {
            CampaignInvariants.validate(state);
            return List.of();
        } catch (CampaignViolationException e) {
            return e.violations().stream().map(v -> v.code()).toList();
        }
    }

    @Test
    void aDraftMayBeIncomplete() {
        assertThat(codes(regular)).isEmpty();
        assertThat(codes(defaultCampaign)).isEmpty();
        assertThat(codes(regular.withStatus(CampaignStatus.PAUSED))).isEmpty();
    }

    @Test
    void scheduledAndActiveMustBeCompleteAndAllMissingPiecesAreReportedTogether() {
        assertThat(codes(regular.withStatus(CampaignStatus.SCHEDULED))).containsExactlyInAnyOrder(
                ViolationCodes.PLAYLIST_EMPTY, ViolationCodes.AUDIENCE_RULE_REQUIRED,
                ViolationCodes.SCHEDULE_START_DATE_REQUIRED);
        assertThat(codes(regular.withStatus(CampaignStatus.ACTIVE))).containsExactlyInAnyOrder(
                ViolationCodes.PLAYLIST_EMPTY, ViolationCodes.AUDIENCE_RULE_REQUIRED);
        assertThat(codes(defaultCampaign.withStatus(CampaignStatus.ACTIVE)))
                .containsExactly(ViolationCodes.PLAYLIST_EMPTY);
    }

    @Test
    void aCompleteCampaignPasses() {
        CampaignSnapshot complete = CampaignBuilder.aRegularCampaign().inStatus(CampaignStatus.SCHEDULED).build()
                .snapshot();
        assertThat(codes(complete)).isEmpty();
    }

    @Test
    void aRegularCampaignKeepsItsPriorityAndMinimumTime() {
        assertThat(codes(regular.withPriority(Optional.empty()))).containsExactly(ViolationCodes.PRIORITY_REQUIRED);
        assertThat(codes(regular.withMinimumOnScreenTime(Optional.empty())))
                .containsExactly(ViolationCodes.MINIMUM_ON_SCREEN_REQUIRED);
    }

    @Test
    void theDefaultCampaignRejectsEachRegularOnlyField() {
        assertThat(codes(defaultCampaign.withPriority(Optional.of(Priority.LOW))))
                .containsExactly(ViolationCodes.PRIORITY_NOT_ALLOWED_FOR_DEFAULT);
        assertThat(codes(defaultCampaign.withAudienceRuleId(Optional.of(new AudienceRuleId(UUID.randomUUID())))))
                .containsExactly(ViolationCodes.AUDIENCE_RULE_NOT_ALLOWED_FOR_DEFAULT);
        assertThat(codes(defaultCampaign.withMinimumOnScreenTime(Optional.of(MinimumOnScreenTime.defaultValue()))))
                .containsExactly(ViolationCodes.MINIMUM_ON_SCREEN_NOT_ALLOWED_FOR_DEFAULT);
        assertThat(codes(defaultCampaign.withManualVetoes(Set.of(new VetoId(UUID.randomUUID())))))
                .containsExactly(ViolationCodes.VETOES_NOT_ALLOWED_FOR_DEFAULT);
        assertThat(codes(defaultCampaign.withSchedule(Schedule.always().withDays(Set.of(DayOfWeek.MONDAY)))))
                .containsExactly(ViolationCodes.SCHEDULE_NOT_CHANGEABLE_FOR_DEFAULT);
        assertThat(codes(defaultCampaign.withSchedule(
                Schedule.always().withDates(Optional.of(LocalDate.of(2030, 1, 1)), Optional.empty()))))
                .containsExactly(ViolationCodes.SCHEDULE_NOT_CHANGEABLE_FOR_DEFAULT);
    }

    @Test
    void requireNotFinishedOnlyRejectsFinished() {
        assertThatCode(() -> CampaignInvariants.requireNotFinished(regular)).doesNotThrowAnyException();
        assertThatThrownBy(() -> CampaignInvariants.requireNotFinished(regular.withStatus(CampaignStatus.FINISHED)))
                .isInstanceOfSatisfying(CampaignViolationException.class,
                        e -> assertThat(e.hasCode(ViolationCodes.STATUS_FINISHED)).isTrue());
        assertThat(codes(regular.withPlaylist(Playlist.empty()))).isEmpty();
    }
}

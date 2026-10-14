package com.cvretail.kiosko.campaigns.application;

import static com.cvretail.kiosko.campaigns.support.CampaignBuilder.aDefaultCampaign;
import static com.cvretail.kiosko.campaigns.support.CampaignBuilder.aRegularCampaign;
import static org.assertj.core.api.Assertions.assertThat;

import com.cvretail.kiosko.campaigns.CampaignView;
import com.cvretail.kiosko.campaigns.domain.entity.Campaign;
import com.cvretail.kiosko.campaigns.domain.valueobject.CampaignStatus;
import com.cvretail.kiosko.campaigns.domain.valueobject.CampaignType;
import com.cvretail.kiosko.campaigns.domain.valueobject.KioskId;
import com.cvretail.kiosko.campaigns.domain.valueobject.Priority;
import com.cvretail.kiosko.campaigns.domain.valueobject.Schedule;
import com.cvretail.kiosko.campaigns.domain.valueobject.Scope;
import com.cvretail.kiosko.campaigns.domain.valueobject.TimeSlot;
import com.cvretail.kiosko.campaigns.domain.valueobject.VetoId;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CampaignViewsTest {

    @Test
    void mapsEveryFieldOfARegularCampaign() {
        Campaign c = aRegularCampaign().inStore("store-9").withPlaylistItems(2).withAudienceRule().build();
        VetoId veto = new VetoId(UUID.randomUUID());
        c.addManualVeto(veto);
        c.changePriority(Priority.HIGH);
        c.changeScope(Scope.SelectedKiosks.of(List.of(new KioskId("k1"), new KioskId("k2"))));
        c.reschedule(new Schedule(Optional.of(LocalDate.of(2030, 1, 1)), Optional.of(LocalDate.of(2030, 2, 1)),
                Set.of(DayOfWeek.MONDAY), Optional.of(new TimeSlot(LocalTime.of(9, 0), LocalTime.of(18, 0)))));

        CampaignView view = CampaignViews.from(c);

        assertThat(view.id()).isEqualTo(c.id().value());
        assertThat(view.storeId()).isEqualTo("store-9");
        assertThat(view.type()).isEqualTo(CampaignView.Type.REGULAR);
        assertThat(view.status()).isEqualTo(CampaignView.Status.DRAFT);
        assertThat(view.priority()).contains(CampaignView.Priority.HIGH);
        assertThat(view.minimumOnScreenSeconds()).contains(8);
        assertThat(view.audienceRuleId()).contains(c.audienceRuleId().orElseThrow().value());
        assertThat(view.manualVetoIds()).containsExactly(veto.value());
        assertThat(view.playlist()).hasSize(2);
        assertThat(view.playlist().get(0).durationSeconds()).isEqualTo(10);
        assertThat(view.playlist().get(0).mediaRef()).startsWith("sha256:");
        assertThat(view.scope().allKiosks()).isFalse();
        assertThat(view.scope().kioskIds()).containsExactlyInAnyOrder("k1", "k2");
        assertThat(view.schedule().startDate()).contains(LocalDate.of(2030, 1, 1));
        assertThat(view.schedule().endDate()).contains(LocalDate.of(2030, 2, 1));
        assertThat(view.schedule().days()).containsExactly(DayOfWeek.MONDAY);
        assertThat(view.schedule().slot()).contains(new CampaignView.TimeSlot(LocalTime.of(9, 0), LocalTime.of(18, 0)));
    }

    @Test
    void mapsADefaultCampaign() {
        CampaignView view = CampaignViews.from(aDefaultCampaign().withPlaylistItems(1).build());
        assertThat(view.type()).isEqualTo(CampaignView.Type.DEFAULT);
        assertThat(view.priority()).isEmpty();
        assertThat(view.minimumOnScreenSeconds()).isEmpty();
        assertThat(view.audienceRuleId()).isEmpty();
        assertThat(view.manualVetoIds()).isEmpty();
        assertThat(view.scope().allKiosks()).isTrue();
        assertThat(view.scope().kioskIds()).isEmpty();
    }

    @Test
    void viewEnumsHaveTheSameNamesAsTheDomainEnums() {
        assertThat(names(CampaignView.Type.values())).isEqualTo(names(CampaignType.values()));
        assertThat(names(CampaignView.Status.values())).isEqualTo(names(CampaignStatus.values()));
        assertThat(names(CampaignView.Priority.values())).isEqualTo(names(Priority.values()));
    }

    @Test
    void mapsEveryStatus() {
        for (CampaignStatus status : CampaignStatus.values()) {
            Campaign c = aRegularCampaign().inStatus(status).build();
            assertThat(CampaignViews.from(c).status().name()).isEqualTo(status.name());
        }
    }

    private static List<String> names(Enum<?>[] values) {
        return Arrays.stream(values).map(Enum::name).toList();
    }
}

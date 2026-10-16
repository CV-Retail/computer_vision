package com.cvretail.kiosko.rules.application;

import static com.cvretail.kiosko.rules.support.CampaignViewBuilder.aCampaign;
import static org.assertj.core.api.Assertions.assertThat;

import com.cvretail.kiosko.campaigns.CampaignView;
import com.cvretail.kiosko.rules.domain.valueobject.CampaignLifecycle;
import com.cvretail.kiosko.rules.domain.valueobject.CampaignRunPlan;
import com.cvretail.kiosko.rules.domain.valueobject.DailySlot;
import com.cvretail.kiosko.rules.domain.valueobject.KioskId;
import com.cvretail.kiosko.rules.domain.valueobject.KioskSelection;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CampaignRunPlansTest {

    @Test
    void mapsEveryField() {
        CampaignView view = aCampaign().startingOn("2030-01-10").endingOn("2030-01-20")
                .onDays(DayOfWeek.MONDAY, DayOfWeek.FRIDAY).between("22:00", "02:00").build();

        CampaignRunPlan plan = CampaignRunPlans.from(view);

        assertThat(plan.id()).isEqualTo(view.id());
        assertThat(plan.lifecycle()).isEqualTo(CampaignLifecycle.ACTIVE);
        assertThat(plan.window().startDate()).contains(LocalDate.of(2030, 1, 10));
        assertThat(plan.window().endDate()).contains(LocalDate.of(2030, 1, 20));
        assertThat(plan.window().days()).containsExactlyInAnyOrder(DayOfWeek.MONDAY, DayOfWeek.FRIDAY);
        assertThat(plan.window().slot()).contains(new DailySlot(LocalTime.of(22, 0), LocalTime.of(2, 0)));
        assertThat(plan.window().slot().orElseThrow().crossesMidnight()).isTrue();
    }

    @Test
    void mapsEveryStatusByName() {
        for (CampaignView.Status status : CampaignView.Status.values()) {
            CampaignRunPlan plan = CampaignRunPlans.from(aCampaign().withStatus(status).build());
            assertThat(plan.lifecycle().name()).isEqualTo(status.name());
        }
        assertThat(Arrays.stream(CampaignLifecycle.values()).map(Enum::name))
                .containsExactlyInAnyOrderElementsOf(
                        Arrays.stream(CampaignView.Status.values()).map(Enum::name).toList());
    }

    @Test
    void mapsAllKiosksAndSelectedKiosks() {
        assertThat(CampaignRunPlans.from(aCampaign().build()).kiosks()).isEqualTo(new KioskSelection.AllKiosks());

        KioskSelection one = CampaignRunPlans.from(aCampaign().onKiosks("kiosk-a").build()).kiosks();
        assertThat(one).isEqualTo(new KioskSelection.SelectedKiosks(Set.of(new KioskId("kiosk-a"))));

        KioskSelection several = CampaignRunPlans.from(aCampaign().onKiosks("kiosk-a", "kiosk-b").build()).kiosks();
        assertThat(((KioskSelection.SelectedKiosks) several).kiosks())
                .containsExactlyInAnyOrder(new KioskId("kiosk-a"), new KioskId("kiosk-b"));
    }

    @Test
    void aCampaignWithoutSlotHasNoSlot() {
        assertThat(CampaignRunPlans.from(aCampaign().build()).window().slot()).isEmpty();
    }
}

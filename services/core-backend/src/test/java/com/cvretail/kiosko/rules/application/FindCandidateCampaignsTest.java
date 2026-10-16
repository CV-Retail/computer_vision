package com.cvretail.kiosko.rules.application;

import static com.cvretail.kiosko.rules.support.CampaignViewBuilder.aCampaign;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cvretail.kiosko.campaigns.CampaignView;
import com.cvretail.kiosko.rules.domain.valueobject.Availability;
import com.cvretail.kiosko.rules.support.InMemoryCampaignCatalog;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class FindCandidateCampaignsTest {

    private static final ZoneId MADRID = ZoneId.of("Europe/Madrid");
    private static final ZoneId LOS_ANGELES = ZoneId.of("America/Los_Angeles");

    /** 2030-01-14 12:00 in Madrid (a Monday, UTC+1). */
    private static final Instant MONDAY_NOON_MADRID = Instant.parse("2030-01-14T11:00:00Z");

    private static Clock clock(String instant, ZoneId zone) {
        return Clock.fixed(Instant.parse(instant), zone);
    }

    private static FindCandidateCampaigns useCase(InMemoryCampaignCatalog catalog, Clock clock) {
        return new FindCandidateCampaigns(catalog, clock);
    }

    private static UUID id(int n) {
        return new UUID(0, n);
    }

    private static List<UUID> ids(List<CampaignView> views) {
        return views.stream().map(CampaignView::id).toList();
    }

    @Test
    void onlyActiveAndScheduledCampaignsAreCandidates() {
        InMemoryCampaignCatalog catalog = new InMemoryCampaignCatalog();
        for (CampaignView.Status status : CampaignView.Status.values()) {
            catalog.add(aCampaign().withId(id(status.ordinal())).withStatus(status).build());
        }
        List<CampaignView> candidates = useCase(catalog, Clock.fixed(MONDAY_NOON_MADRID, MADRID))
                .candidatesFor("store-1", "kiosk-a");
        assertThat(ids(candidates)).containsExactlyInAnyOrder(
                id(CampaignView.Status.ACTIVE.ordinal()), id(CampaignView.Status.SCHEDULED.ordinal()));
    }

    @Test
    void activeCampaignsComeBeforeScheduledOnesAndEachGroupKeepsTheCatalogOrder() {
        InMemoryCampaignCatalog catalog = new InMemoryCampaignCatalog()
                .add(aCampaign().withId(id(1)).withStatus(CampaignView.Status.SCHEDULED).build())
                .add(aCampaign().withId(id(2)).withStatus(CampaignView.Status.ACTIVE).build())
                .add(aCampaign().withId(id(3)).withStatus(CampaignView.Status.SCHEDULED).build())
                .add(aCampaign().withId(id(4)).withStatus(CampaignView.Status.ACTIVE).build());
        assertThat(ids(useCase(catalog, Clock.fixed(MONDAY_NOON_MADRID, MADRID)).candidatesFor("store-1", "kiosk-a")))
                .containsExactly(id(2), id(4), id(1), id(3));
    }

    @Test
    void aScheduledCampaignBeforeItsStartDateIsNotACandidate() {
        InMemoryCampaignCatalog catalog = new InMemoryCampaignCatalog()
                .add(aCampaign().withStatus(CampaignView.Status.SCHEDULED).startingOn("2030-02-01").build());
        assertThat(useCase(catalog, Clock.fixed(MONDAY_NOON_MADRID, MADRID)).candidatesFor("store-1", "kiosk-a"))
                .isEmpty();
    }

    @Test
    void campaignsOfAnotherStoreAreIgnored() {
        InMemoryCampaignCatalog catalog = new InMemoryCampaignCatalog()
                .add(aCampaign().withId(id(1)).inStore("store-1").build())
                .add(aCampaign().withId(id(2)).inStore("store-2").build());
        assertThat(ids(useCase(catalog, Clock.fixed(MONDAY_NOON_MADRID, MADRID)).candidatesFor("store-1", "kiosk-a")))
                .containsExactly(id(1));
    }

    @Test
    void aCatalogThatReturnsAForeignStoreViewIsNotTrusted() {
        CampaignView foreign = aCampaign().inStore("store-2").build();
        com.cvretail.kiosko.campaigns.CampaignCatalog sloppy = new com.cvretail.kiosko.campaigns.CampaignCatalog() {
            @Override
            public List<CampaignView> findByStatus(String storeId, CampaignView.Status status) {
                return status == CampaignView.Status.ACTIVE ? List.of(foreign) : List.of();
            }

            @Override
            public java.util.Optional<CampaignView> findDefault(String storeId) {
                return java.util.Optional.empty();
            }
        };
        assertThat(new FindCandidateCampaigns(sloppy, Clock.fixed(MONDAY_NOON_MADRID, MADRID))
                .candidatesFor("store-1", "kiosk-a")).isEmpty();
    }

    @Test
    void theKioskScopeIsRespected() {
        InMemoryCampaignCatalog catalog = new InMemoryCampaignCatalog()
                .add(aCampaign().withId(id(1)).onKiosks("kiosk-a").build())
                .add(aCampaign().withId(id(2)).onKiosks("kiosk-b").build())
                .add(aCampaign().withId(id(3)).build());
        assertThat(ids(useCase(catalog, Clock.fixed(MONDAY_NOON_MADRID, MADRID)).candidatesFor("store-1", "kiosk-a")))
                .containsExactly(id(1), id(3));
    }

    @Test
    void anEmptyStoreGivesAnEmptyListAndNoFailure() {
        assertThat(useCase(new InMemoryCampaignCatalog(), Clock.fixed(MONDAY_NOON_MADRID, MADRID))
                .candidatesFor("store-1", "kiosk-a")).isEmpty();
    }

    @Test
    void theResultCannotBeModified() {
        InMemoryCampaignCatalog catalog = new InMemoryCampaignCatalog().add(aCampaign().build());
        List<CampaignView> result = useCase(catalog, Clock.fixed(MONDAY_NOON_MADRID, MADRID))
                .candidatesFor("store-1", "kiosk-a");
        assertThatThrownBy(() -> result.add(aCampaign().build())).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void blankArgumentsAreRejected() {
        FindCandidateCampaigns useCase = useCase(new InMemoryCampaignCatalog(), Clock.fixed(MONDAY_NOON_MADRID, MADRID));
        assertThatThrownBy(() -> useCase.candidatesFor(" ", "kiosk-a")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> useCase.candidatesFor(null, "kiosk-a")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> useCase.candidatesFor("store-1", "")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> useCase.candidatesFor("store-1", null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void theCatalogIsOnlyReadByStatus() {
        InMemoryCampaignCatalog catalog = new InMemoryCampaignCatalog();
        useCase(catalog, Clock.fixed(MONDAY_NOON_MADRID, MADRID)).candidatesFor("store-1", "kiosk-a");
        assertThat(catalog.queries()).containsExactly("store-1:ACTIVE", "store-1:SCHEDULED");
    }

    @Test
    void theSameInstantGivesDifferentLocalDatesInDifferentZones() {
        // 2030-01-14T23:30Z is Tuesday 00:30 in Madrid (UTC+1) and Monday 15:30 in Los Angeles (UTC-8).
        Instant instant = Instant.parse("2030-01-14T23:30:00Z");
        InMemoryCampaignCatalog catalog = new InMemoryCampaignCatalog()
                .add(aCampaign().withId(id(1)).onDays(DayOfWeek.MONDAY).build())
                .add(aCampaign().withId(id(2)).onDays(DayOfWeek.TUESDAY).build())
                .add(aCampaign().withId(id(3)).endingOn("2030-01-14").build());
        assertThat(ids(useCase(catalog, Clock.fixed(instant, MADRID)).candidatesFor("store-1", "kiosk-a")))
                .containsExactly(id(2));
        assertThat(ids(useCase(catalog, Clock.fixed(instant, LOS_ANGELES)).candidatesFor("store-1", "kiosk-a")))
                .containsExactly(id(1), id(3));
    }

    @Test
    void aSlotFollowsTheLocalTimeOfTheClockZone() {
        InMemoryCampaignCatalog catalog = new InMemoryCampaignCatalog().add(aCampaign().between("09:00", "18:00").build());
        // 2030-01-14T08:30Z is 09:30 in Madrid and 00:30 in Los Angeles.
        Instant instant = Instant.parse("2030-01-14T08:30:00Z");
        assertThat(useCase(catalog, Clock.fixed(instant, MADRID)).candidatesFor("store-1", "kiosk-a")).hasSize(1);
        assertThat(useCase(catalog, Clock.fixed(instant, LOS_ANGELES)).candidatesFor("store-1", "kiosk-a")).isEmpty();
    }

    @Test
    void whenTheClockMovesForwardAnInstantInTheSkippedHourNeverExists() {
        // Madrid, 2030-03-31: at 01:00Z the local clock jumps from 02:00 to 03:00.
        InMemoryCampaignCatalog catalog = new InMemoryCampaignCatalog()
                .add(aCampaign().withId(id(1)).between("02:00", "04:00").build())
                .add(aCampaign().withId(id(2)).between("02:30", "02:45").build());
        Instant before = Instant.parse("2030-03-31T00:59:59Z"); // 01:59:59 local
        Instant after = Instant.parse("2030-03-31T01:00:00Z");  // 03:00:00 local
        assertThat(useCase(catalog, Clock.fixed(before, MADRID)).candidatesFor("store-1", "kiosk-a")).isEmpty();
        assertThat(ids(useCase(catalog, Clock.fixed(after, MADRID)).candidatesFor("store-1", "kiosk-a")))
                .containsExactly(id(1));
        // The slot 02:30-02:45 never applies on this day, at any instant.
        for (int minute = 0; minute < 24 * 60; minute++) {
            Instant instant = Instant.parse("2030-03-30T23:00:00Z").plusSeconds(minute * 60L);
            assertThat(ids(useCase(catalog, Clock.fixed(instant, MADRID)).candidatesFor("store-1", "kiosk-a")))
                    .doesNotContain(id(2));
        }
    }

    @Test
    void whenTheClockMovesBackTheRepeatedHourGivesTheSameAnswerTwice() {
        // Madrid, 2030-10-27: at 01:00Z the local clock goes back from 03:00 to 02:00, so 02:30 happens twice.
        InMemoryCampaignCatalog catalog = new InMemoryCampaignCatalog().add(aCampaign().between("02:00", "03:00").build());
        Instant first = Instant.parse("2030-10-27T00:30:00Z");  // 02:30 CEST
        Instant second = Instant.parse("2030-10-27T01:30:00Z"); // 02:30 CET
        assertThat(useCase(catalog, Clock.fixed(first, MADRID)).candidatesFor("store-1", "kiosk-a")).hasSize(1);
        assertThat(useCase(catalog, Clock.fixed(second, MADRID)).candidatesFor("store-1", "kiosk-a")).hasSize(1);
        Instant after = Instant.parse("2030-10-27T02:00:00Z"); // 03:00 CET
        assertThat(useCase(catalog, Clock.fixed(after, MADRID)).candidatesFor("store-1", "kiosk-a")).isEmpty();
    }

    @Test
    void availabilityOfExplainsWhyACampaignDoesNotApply() {
        FindCandidateCampaigns useCase = useCase(new InMemoryCampaignCatalog(), Clock.fixed(MONDAY_NOON_MADRID, MADRID));
        assertThat(useCase.availabilityOf(aCampaign().build(), "kiosk-a")).isEqualTo(Availability.APPLIES);
        assertThat(useCase.availabilityOf(aCampaign().withStatus(CampaignView.Status.PAUSED).build(), "kiosk-a"))
                .isEqualTo(Availability.STATUS_NOT_ELIGIBLE);
        assertThat(useCase.availabilityOf(aCampaign().endingOn("2030-01-13").build(), "kiosk-a"))
                .isEqualTo(Availability.AFTER_END_DATE);
        assertThat(useCase.availabilityOf(aCampaign().onKiosks("kiosk-b").build(), "kiosk-a"))
                .isEqualTo(Availability.KIOSK_NOT_INCLUDED);
    }

    @Test
    void theMomentIsReadOnceSoEveryCampaignIsJudgedAtTheSameInstant() {
        AtomicInteger reads = new AtomicInteger();
        Clock counting = new Clock() {
            @Override
            public ZoneId getZone() {
                return MADRID;
            }

            @Override
            public Clock withZone(ZoneId zone) {
                return this;
            }

            @Override
            public Instant instant() {
                reads.incrementAndGet();
                return MONDAY_NOON_MADRID;
            }
        };
        InMemoryCampaignCatalog catalog = new InMemoryCampaignCatalog()
                .add(aCampaign().build()).add(aCampaign().build()).add(aCampaign().build());
        useCase(catalog, counting).candidatesFor("store-1", "kiosk-a");
        assertThat(reads.get()).isEqualTo(1);
    }
}

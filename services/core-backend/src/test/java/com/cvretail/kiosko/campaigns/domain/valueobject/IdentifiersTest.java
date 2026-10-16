package com.cvretail.kiosko.campaigns.domain.valueobject;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cvretail.kiosko.campaigns.domain.exception.CampaignViolationException;
import com.cvretail.kiosko.campaigns.domain.exception.ViolationCodes;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class IdentifiersTest {

    private static final String HASH = "sha256:" + "a".repeat(64);

    @Test
    void campaignIdsAreEqualByValueAndGeneratedOnesDiffer() {
        UUID uuid = UUID.randomUUID();
        assertThat(new CampaignId(uuid)).isEqualTo(new CampaignId(uuid));
        assertThat(CampaignId.generate()).isNotEqualTo(CampaignId.generate());
    }

    @Test
    void missingIdentifiersAreReportedAsRequired() {
        assertThatThrownBy(() -> new CampaignId(null)).isInstanceOfSatisfying(CampaignViolationException.class,
                e -> assertThat(e.violations()).extracting("code", "field").containsExactly(
                        org.assertj.core.groups.Tuple.tuple("id.required", "id")));
        assertThatThrownBy(() -> new AudienceRuleId(null)).hasMessageContaining("audienceRuleId.required");
        assertThatThrownBy(() -> new VetoId(null)).hasMessageContaining("vetoId.required");
        assertThatThrownBy(() -> new StoreId(null)).hasMessageContaining("storeId.required");
    }

    @ParameterizedTest
    @ValueSource(strings = {"store-1", "A", "tienda.001_a", "0abc"})
    void validStoreAndKioskIdsAreAccepted(String value) {
        assertThat(new StoreId(value).value()).isEqualTo(value);
        assertThat(new KioskId(value).value()).isEqualTo(value);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "-start", ".start", "has space", "a/b", "ñandú"})
    void invalidStoreAndKioskIdsAreRejected(String value) {
        assertThatThrownBy(() -> new StoreId(value)).isInstanceOfSatisfying(CampaignViolationException.class,
                e -> assertThat(e.hasCode(ViolationCodes.STORE_INVALID)).isTrue());
        assertThatThrownBy(() -> new KioskId(value)).isInstanceOfSatisfying(CampaignViolationException.class,
                e -> assertThat(e.hasCode(ViolationCodes.KIOSK_INVALID)).isTrue());
    }

    @Test
    void storeIdsLongerThan64CharactersAreRejected() {
        assertThat(new StoreId("a".repeat(64)).value()).hasSize(64);
        assertThatThrownBy(() -> new StoreId("a".repeat(65))).isInstanceOf(CampaignViolationException.class);
    }

    @Test
    void mediaRefMustBeSha256WithLowercaseHex() {
        assertThat(new MediaRef(HASH).value()).isEqualTo(HASH);
        for (String bad : new String[] {"", "sha256:abc", "md5:" + "a".repeat(64), "sha256:" + "A".repeat(64),
                "sha256:" + "g".repeat(64), HASH + "0"}) {
            assertThatThrownBy(() -> new MediaRef(bad)).isInstanceOfSatisfying(CampaignViolationException.class,
                    e -> assertThat(e.hasCode(ViolationCodes.MEDIA_REF_INVALID)).isTrue());
        }
    }
}

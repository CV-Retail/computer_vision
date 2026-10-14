package com.cvretail.kiosko.campaigns.support;

import com.cvretail.kiosko.campaigns.domain.entity.Campaign;
import com.cvretail.kiosko.campaigns.domain.valueobject.AudienceRuleId;
import com.cvretail.kiosko.campaigns.domain.valueobject.CampaignId;
import com.cvretail.kiosko.campaigns.domain.valueobject.CampaignName;
import com.cvretail.kiosko.campaigns.domain.valueobject.CampaignStatus;
import com.cvretail.kiosko.campaigns.domain.valueobject.CampaignType;
import com.cvretail.kiosko.campaigns.domain.valueobject.MediaRef;
import com.cvretail.kiosko.campaigns.domain.valueobject.PlaylistItem;
import com.cvretail.kiosko.campaigns.domain.valueobject.Schedule;
import com.cvretail.kiosko.campaigns.domain.valueobject.StoreId;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

/**
 * Test data builder: a test states only what it cares about. A campaign in any status other than DRAFT is built
 * complete, and the events recorded while building are discarded.
 */
public final class CampaignBuilder {

    private CampaignType type = CampaignType.REGULAR;
    private StoreId storeId = new StoreId("store-1");
    private CampaignId id = CampaignId.generate();
    private String name = "Summer sale";
    private CampaignStatus status = CampaignStatus.DRAFT;
    private int playlistItems;
    private boolean audienceRule;
    private LocalDate startDate;

    private CampaignBuilder() {
    }

    public static CampaignBuilder aRegularCampaign() {
        return new CampaignBuilder();
    }

    public static CampaignBuilder aDefaultCampaign() {
        CampaignBuilder builder = new CampaignBuilder();
        builder.type = CampaignType.DEFAULT;
        builder.name = "Default playlist";
        return builder;
    }

    public CampaignBuilder inStore(String store) {
        this.storeId = new StoreId(store);
        return this;
    }

    public CampaignBuilder withId(CampaignId campaignId) {
        this.id = campaignId;
        return this;
    }

    public CampaignBuilder named(String campaignName) {
        this.name = campaignName;
        return this;
    }

    public CampaignBuilder withPlaylistItems(int count) {
        this.playlistItems = count;
        return this;
    }

    public CampaignBuilder withAudienceRule() {
        this.audienceRule = true;
        return this;
    }

    public CampaignBuilder startingOn(LocalDate date) {
        this.startDate = date;
        return this;
    }

    public CampaignBuilder inStatus(CampaignStatus target) {
        this.status = target;
        return this;
    }

    public Campaign build() {
        Campaign campaign = type == CampaignType.REGULAR
                ? Campaign.createRegular(id, storeId, new CampaignName(name))
                : Campaign.createDefault(id, storeId, new CampaignName(name));
        boolean complete = status != CampaignStatus.DRAFT;
        int items = complete ? Math.max(playlistItems, 1) : playlistItems;
        for (int i = 0; i < items; i++) {
            campaign.addPlaylistItem(item(i + 1));
        }
        if (type == CampaignType.REGULAR) {
            if (audienceRule || complete) {
                campaign.assignAudienceRule(new AudienceRuleId(UUID.randomUUID()));
            }
            if (startDate != null || status == CampaignStatus.SCHEDULED) {
                LocalDate start = startDate != null ? startDate : LocalDate.of(2030, 1, 1);
                campaign.reschedule(Schedule.always().withDates(Optional.of(start), Optional.empty()));
            }
        }
        switch (status) {
            case DRAFT -> { }
            case SCHEDULED -> campaign.schedule();
            case ACTIVE -> campaign.activate();
            case PAUSED -> {
                campaign.activate();
                campaign.pause();
            }
            case FINISHED -> {
                campaign.activate();
                campaign.finish();
            }
        }
        campaign.pullEvents();
        return campaign;
    }

    public static PlaylistItem item(int n) {
        return new PlaylistItem(media(n), 10);
    }

    public static MediaRef media(int n) {
        return new MediaRef("sha256:" + String.format("%064x", n));
    }
}

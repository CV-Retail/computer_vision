package com.cvretail.kiosko.campaigns.domain.valueobject;

import java.util.Optional;
import java.util.Set;

/** Immutable full state of a campaign. It is the internal state of the aggregate and its persisted form. */
public record CampaignSnapshot(
        CampaignId id,
        StoreId storeId,
        CampaignType type,
        CampaignName name,
        CampaignStatus status,
        Optional<Priority> priority,
        Optional<AudienceRuleId> audienceRuleId,
        Playlist playlist,
        Schedule schedule,
        Scope scope,
        Optional<MinimumOnScreenTime> minimumOnScreenTime,
        Set<VetoId> manualVetoes) {

    public CampaignSnapshot {
        Require.notNull(id, "id");
        Require.notNull(storeId, "storeId");
        Require.notNull(type, "type");
        Require.notNull(name, "name");
        Require.notNull(status, "status");
        Require.notNull(priority, "priority");
        Require.notNull(audienceRuleId, "audienceRuleId");
        Require.notNull(playlist, "playlist");
        Require.notNull(schedule, "schedule");
        Require.notNull(scope, "scope");
        Require.notNull(minimumOnScreenTime, "minimumOnScreenTime");
        Require.notNull(manualVetoes, "manualVetoes");
        manualVetoes = Set.copyOf(manualVetoes);
    }

    public CampaignSnapshot withName(CampaignName value) {
        return new CampaignSnapshot(id, storeId, type, value, status, priority, audienceRuleId, playlist,
                schedule, scope, minimumOnScreenTime, manualVetoes);
    }

    public CampaignSnapshot withStatus(CampaignStatus value) {
        return new CampaignSnapshot(id, storeId, type, name, value, priority, audienceRuleId, playlist,
                schedule, scope, minimumOnScreenTime, manualVetoes);
    }

    public CampaignSnapshot withPriority(Optional<Priority> value) {
        return new CampaignSnapshot(id, storeId, type, name, status, value, audienceRuleId, playlist,
                schedule, scope, minimumOnScreenTime, manualVetoes);
    }

    public CampaignSnapshot withAudienceRuleId(Optional<AudienceRuleId> value) {
        return new CampaignSnapshot(id, storeId, type, name, status, priority, value, playlist,
                schedule, scope, minimumOnScreenTime, manualVetoes);
    }

    public CampaignSnapshot withPlaylist(Playlist value) {
        return new CampaignSnapshot(id, storeId, type, name, status, priority, audienceRuleId, value,
                schedule, scope, minimumOnScreenTime, manualVetoes);
    }

    public CampaignSnapshot withSchedule(Schedule value) {
        return new CampaignSnapshot(id, storeId, type, name, status, priority, audienceRuleId, playlist,
                value, scope, minimumOnScreenTime, manualVetoes);
    }

    public CampaignSnapshot withScope(Scope value) {
        return new CampaignSnapshot(id, storeId, type, name, status, priority, audienceRuleId, playlist,
                schedule, value, minimumOnScreenTime, manualVetoes);
    }

    public CampaignSnapshot withMinimumOnScreenTime(Optional<MinimumOnScreenTime> value) {
        return new CampaignSnapshot(id, storeId, type, name, status, priority, audienceRuleId, playlist,
                schedule, scope, value, manualVetoes);
    }

    public CampaignSnapshot withManualVetoes(Set<VetoId> value) {
        return new CampaignSnapshot(id, storeId, type, name, status, priority, audienceRuleId, playlist,
                schedule, scope, minimumOnScreenTime, value);
    }
}

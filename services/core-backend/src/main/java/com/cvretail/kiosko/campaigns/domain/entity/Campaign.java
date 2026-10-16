package com.cvretail.kiosko.campaigns.domain.entity;

import com.cvretail.kiosko.campaigns.domain.event.CampaignActivated;
import com.cvretail.kiosko.campaigns.domain.event.CampaignCreated;
import com.cvretail.kiosko.campaigns.domain.event.CampaignEvent;
import com.cvretail.kiosko.campaigns.domain.event.CampaignFinished;
import com.cvretail.kiosko.campaigns.domain.event.CampaignPaused;
import com.cvretail.kiosko.campaigns.domain.event.CampaignScheduled;
import com.cvretail.kiosko.campaigns.domain.exception.CampaignViolationException;
import com.cvretail.kiosko.campaigns.domain.exception.ViolationCodes;
import com.cvretail.kiosko.campaigns.domain.service.CampaignInvariants;
import com.cvretail.kiosko.campaigns.domain.service.CampaignStatusPolicy;
import com.cvretail.kiosko.campaigns.domain.service.CampaignStatusPolicy.Transition;
import com.cvretail.kiosko.campaigns.domain.valueobject.AudienceRuleId;
import com.cvretail.kiosko.campaigns.domain.valueobject.CampaignId;
import com.cvretail.kiosko.campaigns.domain.valueobject.CampaignName;
import com.cvretail.kiosko.campaigns.domain.valueobject.CampaignSnapshot;
import com.cvretail.kiosko.campaigns.domain.valueobject.CampaignStatus;
import com.cvretail.kiosko.campaigns.domain.valueobject.CampaignType;
import com.cvretail.kiosko.campaigns.domain.valueobject.MinimumOnScreenTime;
import com.cvretail.kiosko.campaigns.domain.valueobject.Playlist;
import com.cvretail.kiosko.campaigns.domain.valueobject.PlaylistItem;
import com.cvretail.kiosko.campaigns.domain.valueobject.Priority;
import com.cvretail.kiosko.campaigns.domain.valueobject.Schedule;
import com.cvretail.kiosko.campaigns.domain.valueobject.Scope;
import com.cvretail.kiosko.campaigns.domain.valueobject.StoreId;
import com.cvretail.kiosko.campaigns.domain.valueobject.VetoId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.UnaryOperator;

/**
 * A campaign: what to play, to whom, when and where. Aggregate root.
 *
 * <p>Every change builds a candidate state, validates it and only then replaces the current state, so a
 * rejected change leaves the campaign untouched. The state is only assigned in {@link #apply}.
 */
public final class Campaign {

    private CampaignSnapshot state;
    private final List<CampaignEvent> events = new ArrayList<>();

    private Campaign(CampaignSnapshot state) {
        this.state = state;
    }

    public static Campaign createRegular(CampaignId id, StoreId storeId, CampaignName name) {
        return create(new CampaignSnapshot(id, storeId, CampaignType.REGULAR, name, CampaignStatus.DRAFT,
                Optional.of(Priority.MEDIUM), Optional.empty(), Playlist.empty(), Schedule.always(), Scope.all(),
                Optional.of(MinimumOnScreenTime.defaultValue()), Set.of()));
    }

    public static Campaign createDefault(CampaignId id, StoreId storeId, CampaignName name) {
        return create(new CampaignSnapshot(id, storeId, CampaignType.DEFAULT, name, CampaignStatus.DRAFT,
                Optional.empty(), Optional.empty(), Playlist.empty(), Schedule.always(), Scope.all(),
                Optional.empty(), Set.of()));
    }

    /** Rebuilds a stored campaign. Validates everything and records no event. */
    public static Campaign restore(CampaignSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        CampaignInvariants.validate(snapshot);
        return new Campaign(snapshot);
    }

    private static Campaign create(CampaignSnapshot initial) {
        Campaign campaign = new Campaign(initial);
        campaign.events.add(new CampaignCreated(initial.id(), initial.storeId(), initial.type()));
        return campaign;
    }

    // --- reads ---

    public CampaignId id() {
        return state.id();
    }

    public StoreId storeId() {
        return state.storeId();
    }

    public CampaignType type() {
        return state.type();
    }

    public CampaignName name() {
        return state.name();
    }

    public CampaignStatus status() {
        return state.status();
    }

    public Optional<Priority> priority() {
        return state.priority();
    }

    public Optional<AudienceRuleId> audienceRuleId() {
        return state.audienceRuleId();
    }

    public Playlist playlist() {
        return state.playlist();
    }

    /** When the campaign runs. Named apart from {@link #schedule()}, which is the status transition. */
    public Schedule runSchedule() {
        return state.schedule();
    }

    public Scope scope() {
        return state.scope();
    }

    public Optional<MinimumOnScreenTime> minimumOnScreenTime() {
        return state.minimumOnScreenTime();
    }

    public Set<VetoId> manualVetoes() {
        return state.manualVetoes();
    }

    public CampaignSnapshot snapshot() {
        return state;
    }

    /** Hands over the recorded events in order and forgets them. */
    public List<CampaignEvent> pullEvents() {
        List<CampaignEvent> pulled = List.copyOf(events);
        events.clear();
        return pulled;
    }

    // --- editing ---

    public void rename(CampaignName name) {
        apply(s -> s.withName(name));
    }

    public void changePriority(Priority priority) {
        apply(s -> s.withPriority(Optional.of(Objects.requireNonNull(priority, "priority"))));
    }

    public void assignAudienceRule(AudienceRuleId id) {
        apply(s -> s.withAudienceRuleId(Optional.of(Objects.requireNonNull(id, "audienceRuleId"))));
    }

    public void clearAudienceRule() {
        apply(s -> s.withAudienceRuleId(Optional.empty()));
    }

    public void addPlaylistItem(PlaylistItem item) {
        apply(s -> s.withPlaylist(s.playlist().add(item)));
    }

    public void insertPlaylistItem(int position, PlaylistItem item) {
        apply(s -> s.withPlaylist(s.playlist().insert(position, item)));
    }

    public void removePlaylistItem(int position) {
        apply(s -> s.withPlaylist(s.playlist().remove(position)));
    }

    public void movePlaylistItem(int from, int to) {
        apply(s -> s.withPlaylist(s.playlist().move(from, to)));
    }

    public void changePlaylistItemDuration(int position, int seconds) {
        apply(s -> s.withPlaylist(s.playlist().withDuration(position, seconds)));
    }

    public void reschedule(Schedule schedule) {
        apply(s -> s.withSchedule(Objects.requireNonNull(schedule, "schedule")));
    }

    public void changeScope(Scope scope) {
        apply(s -> s.withScope(Objects.requireNonNull(scope, "scope")));
    }

    public void changeMinimumOnScreenTime(MinimumOnScreenTime time) {
        apply(s -> s.withMinimumOnScreenTime(Optional.of(Objects.requireNonNull(time, "minimumOnScreenTime"))));
    }

    public void addManualVeto(VetoId id) {
        apply(s -> {
            Set<VetoId> vetoes = new HashSet<>(s.manualVetoes());
            if (!vetoes.add(Objects.requireNonNull(id, "vetoId"))) {
                throw new CampaignViolationException("manualVetoes", ViolationCodes.VETO_DUPLICATE,
                        "the veto is already on the campaign");
            }
            return s.withManualVetoes(vetoes);
        });
    }

    public void removeManualVeto(VetoId id) {
        apply(s -> {
            Set<VetoId> vetoes = new HashSet<>(s.manualVetoes());
            if (!vetoes.remove(id)) {
                throw new CampaignViolationException("manualVetoes", ViolationCodes.VETO_NOT_FOUND,
                        "the veto is not on the campaign");
            }
            return s.withManualVetoes(vetoes);
        });
    }

    // --- transitions ---

    public void schedule() {
        transition(Transition.SCHEDULE, new CampaignScheduled(state.id(), state.storeId()));
    }

    public void activate() {
        transition(Transition.ACTIVATE, new CampaignActivated(state.id(), state.storeId()));
    }

    public void pause() {
        transition(Transition.PAUSE, new CampaignPaused(state.id(), state.storeId()));
    }

    public void finish() {
        transition(Transition.FINISH, new CampaignFinished(state.id(), state.storeId()));
    }

    private void transition(Transition transition, CampaignEvent event) {
        CampaignStatus target = CampaignStatusPolicy.requireAllowed(state.status(), transition);
        apply(s -> s.withStatus(target));
        events.add(event);
    }

    /** The only place that replaces the state: validates the candidate first, then commits. */
    private void apply(UnaryOperator<CampaignSnapshot> edit) {
        CampaignInvariants.requireNotFinished(state);
        CampaignSnapshot candidate = edit.apply(state);
        CampaignInvariants.validate(candidate);
        state = candidate;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Campaign that && state.id().equals(that.state.id());
    }

    @Override
    public int hashCode() {
        return state.id().hashCode();
    }
}

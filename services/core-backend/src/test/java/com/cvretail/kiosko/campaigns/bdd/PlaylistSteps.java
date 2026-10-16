package com.cvretail.kiosko.campaigns.bdd;

import static com.cvretail.kiosko.campaigns.support.CampaignBuilder.aRegularCampaign;
import static com.cvretail.kiosko.campaigns.support.CampaignBuilder.item;
import static org.assertj.core.api.Assertions.assertThat;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/** Playlist editing. Items are identified by a number, so a scenario can state the order it expects. */
public class PlaylistSteps {

    private final CampaignWorld world;

    public PlaylistSteps(CampaignWorld world) {
        this.world = world;
    }

    @Given("a draft campaign with a playlist of {int} items")
    public void aDraftCampaignWithAPlaylist(int items) {
        world.campaign = aRegularCampaign().inStore(world.store).withPlaylistItems(items).build();
    }

    @When("the administrator adds the item {int} to the playlist")
    public void adds(int number) {
        world.attempt(() -> world.campaign.addPlaylistItem(item(number)));
    }

    @When("the administrator inserts the item {int} at position {int}")
    public void inserts(int number, int position) {
        world.attempt(() -> world.campaign.insertPlaylistItem(position, item(number)));
    }

    @When("the administrator moves the item at position {int} to position {int}")
    public void moves(int from, int to) {
        world.attempt(() -> world.campaign.movePlaylistItem(from, to));
    }

    @When("the administrator removes the item at position {int}")
    public void removes(int position) {
        world.attempt(() -> world.campaign.removePlaylistItem(position));
    }

    @When("the administrator removes the playlist item")
    public void removesTheOnlyItem() {
        world.attempt(() -> world.campaign.removePlaylistItem(0));
    }

    @When("the administrator changes the duration of the item at position {int} to {int} seconds")
    public void changesDuration(int position, int seconds) {
        world.attempt(() -> world.campaign.changePlaylistItemDuration(position, seconds));
    }

    @Then("the playlist order is {string}")
    public void theOrderIs(String order) {
        List<Integer> actual = world.campaign.playlist().items().stream()
                .map(item -> Integer.parseInt(item.media().value().substring("sha256:".length()), 16))
                .toList();
        List<Integer> expected = Arrays.stream(order.split(",")).map(String::trim).map(Integer::valueOf).toList();
        assertThat(actual).as("playlist order %s", actual.stream().map(String::valueOf).collect(Collectors.joining(", ")))
                .isEqualTo(expected);
    }

    @Then("the item at position {int} lasts {int} seconds")
    public void lasts(int position, int seconds) {
        assertThat(world.campaign.playlist().get(position).durationSeconds()).isEqualTo(seconds);
    }

    @Then("the playlist has {int} items")
    public void hasItems(int count) {
        assertThat(world.campaign.playlist().size()).isEqualTo(count);
    }
}

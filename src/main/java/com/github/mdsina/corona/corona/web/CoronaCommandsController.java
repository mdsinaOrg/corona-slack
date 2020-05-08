package com.github.mdsina.corona.corona.web;

import com.github.mdsina.corona.corona.CoronaSlackDataService;
import com.github.mdsina.corona.slack.SlackMessageSender;
import io.micronaut.context.annotation.Value;
import io.micronaut.core.util.StringUtils;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.client.RxHttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.uri.UriBuilder;
import io.reactivex.Flowable;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller("/corona")
public class CoronaCommandsController {

    private final CoronaSlackDataService coronaSlackDataService;
    private final SlackMessageSender slackMessageSender;
    private final String baseUrl;
    private final String clientId;
    private final String clientSecret;
    private final RxHttpClient slackHttpClient;

    public CoronaCommandsController(
        CoronaSlackDataService coronaSlackDataService,
        SlackMessageSender slackMessageSender,
        @Value("${corona.base.url:}") String baseUrl,
        @Value("${slack.client.id:}") String clientId,
        @Value("${slack.client.secret:}") String clientSecret,
        @Client("https://slack.com") RxHttpClient slackHttpClient
    ) {
        this.coronaSlackDataService = coronaSlackDataService;
        this.slackMessageSender = slackMessageSender;
        this.baseUrl = baseUrl;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.slackHttpClient = slackHttpClient;
    }

    @Post(value = "/stats", consumes = {MediaType.APPLICATION_FORM_URLENCODED, MediaType.APPLICATION_JSON})
    public String stats(@Body Map body) {
        log.debug(body.toString());

        coronaSlackDataService
            .getActualStatsBlocks(getCountriesFromBody(body))
            .map(blocks -> slackMessageSender.sendMessage((String) body.get("channel_id"), blocks))
            .subscribe();

        return "Ok, stats will be send shortly.";
    }

    @Post(value = "/image", consumes = {MediaType.APPLICATION_FORM_URLENCODED, MediaType.APPLICATION_JSON})
    public String graph(@Body Map body) {
        log.debug(body.toString());

        coronaSlackDataService
            .getHistoricalStatsBlocks(getCountriesFromBody(body))
            .map(blocks -> slackMessageSender.sendMessage((String) body.get("channel_id"), blocks))
            .subscribe();

        return "Ok, chart will be send shortly.";
    }

    private List<String> getCountriesFromBody(Map body) {
        String commandText = (String) body.get("text");

        return Optional.ofNullable(commandText)
            .map(t -> List.of(StringUtils.tokenizeToStringArray(t, ",")))
            .orElse(List.of());
    }

    @Get(value = "/callback{?code}", consumes = {MediaType.APPLICATION_FORM_URLENCODED, MediaType.APPLICATION_JSON}, produces = MediaType.TEXT_HTML)
    public Flowable<String> callback(String code) {
        URI redirectUrl = UriBuilder.of(baseUrl).path("corona/callback").build();
        var resultUrl = UriBuilder.of("/api/oauth.access")
            .queryParam("client_id", clientId)
            .queryParam("client_secret", clientSecret)
            .queryParam("code", code)
            .queryParam("redirect_uri", redirectUrl)
            .toString();

        return slackHttpClient.retrieve(resultUrl)
            .map(r -> "App installed. You can close this page.")
            .onErrorReturnItem("App installation failed. Try again");
    }
}

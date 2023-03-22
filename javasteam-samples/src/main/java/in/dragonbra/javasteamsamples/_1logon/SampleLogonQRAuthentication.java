package in.dragonbra.javasteamsamples._1logon;

import in.dragonbra.javasteam.enums.EResult;
import in.dragonbra.javasteam.steam.authentication.AuthenticationException;
import in.dragonbra.javasteam.steam.authentication.AuthPollResult;
import in.dragonbra.javasteam.steam.authentication.AuthSessionDetails;
import in.dragonbra.javasteam.steam.authentication.OnChallengeUrlChanged;
import in.dragonbra.javasteam.steam.authentication.QrAuthSession;
import in.dragonbra.javasteam.steam.authentication.SteamAuthentication;
import in.dragonbra.javasteam.steam.handlers.steamunifiedmessages.SteamUnifiedMessages;
import in.dragonbra.javasteam.steam.handlers.steamuser.LogOnDetails;
import in.dragonbra.javasteam.steam.handlers.steamuser.SteamUser;
import in.dragonbra.javasteam.steam.handlers.steamuser.callback.LoggedOffCallback;
import in.dragonbra.javasteam.steam.handlers.steamuser.callback.LoggedOnCallback;
import in.dragonbra.javasteam.steam.steamclient.SteamClient;
import in.dragonbra.javasteam.steam.steamclient.callbackmgr.CallbackManager;
import in.dragonbra.javasteam.steam.steamclient.callbacks.ConnectedCallback;
import in.dragonbra.javasteam.steam.steamclient.callbacks.DisconnectedCallback;
import in.dragonbra.javasteam.util.log.DefaultLogListener;
import in.dragonbra.javasteam.util.log.LogManager;
import org.jetbrains.annotations.NotNull;
import pro.leaco.console.qrcode.ConsoleQrcode;

import java.util.concurrent.ExecutionException;

// TODO: https://github.com/SteamRE/SteamKit/pull/1129#issuecomment-1473758793
//  CM will kick you in 60 seconds if you don't auth.
//  Steam client also has this issue, but it will reconnect and just continue polling.

/**
 * @author lossy
 * @since 2023-03-19
 */
@SuppressWarnings("FieldCanBeLocal")
public class SampleLogonQRAuthentication implements Runnable, OnChallengeUrlChanged {

    private SteamClient steamClient;

    private SteamUnifiedMessages unifiedMessages;

    private CallbackManager manager;

    private SteamUser steamUser;

    private boolean isRunning;

    public SampleLogonQRAuthentication() {
    }

    public static void main(String[] args) {
        // No username or password is required for this example.

        LogManager.addListener(new DefaultLogListener());

        new SampleLogonQRAuthentication().run();
    }

    @Override
    public void run() {

        // create our steamclient instance
        steamClient = new SteamClient();

        // create the callback manager which will route callbacks to function calls
        manager = new CallbackManager(steamClient);

        // get the steam unified messages handler, which is used for sending and receiving responses from the unified service api
        unifiedMessages = steamClient.getHandler(SteamUnifiedMessages.class);

        // get the steamuser handler, which is used for logging on after successfully connecting
        steamUser = steamClient.getHandler(SteamUser.class);

        // register a few callbacks we're interested in
        // these are registered upon creation to a callback manager, which will then route the callbacks
        // to the functions specified
        manager.subscribe(ConnectedCallback.class, this::onConnected);
        manager.subscribe(DisconnectedCallback.class, this::onDisconnected);

        manager.subscribe(LoggedOnCallback.class, this::onLoggedOn);
        manager.subscribe(LoggedOffCallback.class, this::onLoggedOff);

        isRunning = true;

        System.out.println("Connecting to steam...");

        // initiate the connection
        steamClient.connect();

        // create our callback handling loop
        while (isRunning) {
            // in order for the callbacks to get routed, they need to be handled by the manager
            manager.runWaitCallbacks(1000L);
        }
    }

    private void onConnected(ConnectedCallback callback) {
        try {
            // get the authentication handler, which used for authenticating with Steam
            SteamAuthentication auth = new SteamAuthentication(steamClient, unifiedMessages);

            QrAuthSession authSession = auth.beginAuthSessionViaQR(new AuthSessionDetails());

            // Steam will periodically refresh the challenge url, this callback allows you to draw a new qr code.
            // Note: Callback is below.
            authSession.setChallengeUrlChanged(this);

            // Draw current qr right away
            drawQRCode(authSession);

            // Starting polling Steam for authentication response
            // This response is later used to log on to Steam after connecting
            AuthPollResult pollResponse = authSession.startPolling();

            System.out.println("Connected to Steam! Logging in " + pollResponse.accountName + "...");

            LogOnDetails details = new LogOnDetails();
            details.setUsername(pollResponse.accountName);
            details.setAccessToken(pollResponse.refreshToken);

            // Set LoginID to a non-zero value if you have another client connected using the same account,
            // the same private ip, and same public ip.
            details.setLoginID(149);

            steamUser.logOn(details);
        } catch (AuthenticationException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private void onDisconnected(DisconnectedCallback callback) {
        System.out.println("Disconnected from Steam");

        isRunning = false;
    }

    private void onLoggedOn(LoggedOnCallback callback) {
        if (callback.getResult() != EResult.OK) {
            System.out.println("Unable to logon to Steam: " + callback.getResult() + " / " + callback.getExtendedResult());

            isRunning = false;
            return;
        }

        System.out.println("Successfully logged on!");

        // at this point, we'd be able to perform actions on Steam

        // for this sample we'll just log off
        steamUser.logOff();
    }

    private void onLoggedOff(LoggedOffCallback callback) {
        System.out.println("Logged off of Steam: " + callback.getResult());

        isRunning = false;
    }

    private void drawQRCode(QrAuthSession authSession) {
        String challengeURL = authSession.getChallengeUrl();
        System.out.println("Challenge URL: " + challengeURL);
        System.out.println();

        // Encode and Print the link as a QR code
        System.out.println("Use the Steam Mobile App to sign in via QR code:");
        ConsoleQrcode.INSTANCE.print(challengeURL);
    }

    @Override
    public void onChanged(@NotNull QrAuthSession qrAuthSession) {
        drawQRCode(qrAuthSession);
    }
}

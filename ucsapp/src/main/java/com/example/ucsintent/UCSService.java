package com.example.ucsintent;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.widget.Toast;

import com.example.ucsintent.properties.UCSAppProperties;
import com.fasterxml.jackson.core.JsonProcessingException;

import org.json.JSONException;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import ucs.constants.CONNECTION;
import ucs.constants.OperationName;
import ucs.core.UCSCoreServiceBuilder;
import ucs.exceptions.PolicyException;
import ucs.exceptions.RequestException;
import ucs.message.endaccess.EndAccessMessage;
import ucs.message.startaccess.StartAccessMessage;
import ucs.message.tryaccess.TryAccessMessage;
import ucs.properties.UCSProperties;
import ucs.ucs.UCSInterface;
import utility.AndroidFileUtility;
import utility.errorhandling.Reject;
import xacml.wrappers.PolicyWrapper;
import xacml.wrappers.RequestWrapper;

/**
 * Class that implements a Usage Control System endpoint for communication.
 * It handles building UCS, receiving intents from PEPs and wrapping them into a related Message data structure in
 * order to simplify implementation of PEPs.
 */
public class UCSService extends Service {
    private static Logger log = Logger.getLogger( UCSService.class.getName() );
    private static UCSInterface ucs = null;
    private static UCSProperties ucsProperties;
    private static final String INVALID_MESSAGE_ID = "INVALID_MESSAGE";
    private static final String CHANNEL_ID = "MyChannel";

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        log.info("Service started");
        try {
            buildUCS();
            executeUCSOperation(intent);
        } catch (JSONException | JsonProcessingException e) {
            e.printStackTrace();
        }
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        Notification.Builder builder = new Notification.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.app_name))
                .setContentText("Hello")
                .setAutoCancel(true);
        Notification notification = builder.build();
        startForeground(1, notification);

        log.info("Service created");
        Toast.makeText(this, "Starting UCSService", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        log.info("Service destroyed");
        Toast.makeText(this, "UCSService destroyed", Toast.LENGTH_LONG).show();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * Manages the building of UCSCore with related properties obtained from json assets/configs
     * @throws JSONException
     * @throws JsonProcessingException
     */
    private void buildUCS() throws JSONException, JsonProcessingException {
        if(ucs == null) {
            Toast.makeText(UCSApplication.getContext(), "Building UCS", Toast.LENGTH_SHORT).show();
            ucsProperties = new UCSAppProperties();
            ucs = new UCSCoreServiceBuilder().setProperties(ucsProperties).build();
        }
    }

    /**
     * Manages the wrapping of intent data into a message data structure and the execution of the requested action
     * by the intent sender.
     * @throws JSONException
     * @throws JsonProcessingException
     */
    private void executeUCSOperation(Intent intent){
        log.info("ExecuteUCSOperation");

        String requestType = intent.getStringExtra("requestType");
        if(requestType == null) {
            log.info("Request type is null, probably the service just started");
        } else if(requestType.equals("tryAccess")){
            log.info("TryAccess");
            Toast.makeText(UCSApplication.getContext(), "Request Type: " + intent.getStringExtra("requestType") + "\n"
                            + "Request path: " + intent.getStringExtra("requestPath") + "\nPolicy Path: " + intent.getStringExtra("policyPath") +
                            "\nid: " + intent.getStringExtra("id") + "\nuri: " + intent.getStringExtra("uri") + "\nRevokeType: " + intent.getStringExtra("revokeType")
                            + "\napiStatusChanged: " + intent.getStringExtra("apiStatusChanged")
                    , Toast.LENGTH_SHORT).show();
            log.info("UCSService, arrived startAccess request with sessionId: " + intent.getStringExtra("sessionId")  );
            TryAccessMessage message = (TryAccessMessage) preTryAccess(intent);
            ucs.tryAccess(message);
        } else if(requestType.equals("startAccess")){
            Toast.makeText(UCSApplication.getContext(), "Request Type: " + intent.getStringExtra("requestType") + "\n" +
                            "\nid: " + intent.getStringExtra("id") + "\nuri: " + intent.getStringExtra("uri") + "\nSession Id: " + intent.getStringExtra("sessionId")
                    , Toast.LENGTH_SHORT).show();
            log.info("UCSService, arrived startAccess request with sessionId: " + intent.getStringExtra("sessionId")  );
            StartAccessMessage message = buildStartAccessMessage(intent);
            ucs.startAccess(message);
        } else if(requestType.equals("endAccess")){
            Toast.makeText(UCSApplication.getContext(), "Request Type: " + intent.getStringExtra("requestType") + "\n" +
                            "\nid: " + intent.getStringExtra("id") + "\nuri: " + intent.getStringExtra("uri") + "\nSession Id: " + intent.getStringExtra("sessionId")
                    , Toast.LENGTH_SHORT).show();
            log.info("UCSService, arrived endAccess request with sessionId: " + intent.getStringExtra("sessionId")  );
            EndAccessMessage message = buildEndAccessMessage(intent);
            ucs.endAccess(message);
        }
    }

    /**
     * Perform fetching of request and policy xml files based by received intent metadata
     * necessary for building a TryAccessMessage
     * @throws JSONException
     * @throws JsonProcessingException
     */
    private Object preTryAccess(Intent intent) {
        Reject.ifNull(ucs);
        String policyPath = intent.getStringExtra("policyPath");
        String requestPath = intent.getStringExtra("requestPath");

        log.log( Level.INFO, "TryAccess at " + System.currentTimeMillis() + "");
        log.log(Level.INFO, "Current policy path: " + policyPath);
        log.log(Level.INFO, "Current request path: " + requestPath);
        PolicyWrapper policy = null;
        RequestWrapper request = null;
        try {
            String policyString = AndroidFileUtility.readAssetFileAsString( policyPath, UCSApplication.getContext());
            String requestString = AndroidFileUtility.readAssetFileAsString( requestPath, UCSApplication.getContext() );
            request = RequestWrapper.build(requestString);
            policy = PolicyWrapper.build(policyString);
        } catch(PolicyException | RequestException e ) {
            e.printStackTrace();
            return INVALID_MESSAGE_ID;
        }
        return buildTryAccessMessage( request, policy, intent );
    }

    private TryAccessMessage buildTryAccessMessage( RequestWrapper request, PolicyWrapper policy, Intent intent) {
        log.log(Level.INFO, "buildTryAccessMessage");
        String uri = intent.getStringExtra("uri");
        String id = intent.getStringExtra("id");
        String apiStatusChanged = intent.getStringExtra("apiStatusChanged");

        TryAccessMessage message = new TryAccessMessage( uri, id );
        message.setPepUri( buildResponseApi( apiStatusChanged, uri ) );
        message.setPolicy( policy.getPolicy() );
        message.setRequest( request.getRequest() );
        message.setCallback( buildResponseApi( OperationName.TRYACCESSRESPONSE_REST, uri ), CONNECTION.REST );
        return message;
    }

    private StartAccessMessage buildStartAccessMessage(Intent intent) {
        String uri = intent.getStringExtra("uri");
        String id = intent.getStringExtra("id");
        String sessionId = intent.getStringExtra("sessionId");
        StartAccessMessage message = new StartAccessMessage( uri, id );
        message.setSessionId( sessionId );
        message.setCallback( buildResponseApi( OperationName.STARTACCESSRESPONSE_REST, uri ), CONNECTION.REST );
        return message;
    }

    private EndAccessMessage buildEndAccessMessage(Intent intent) {
        String uri = intent.getStringExtra("uri");
        String id = intent.getStringExtra("id");
        String sessionId = intent.getStringExtra("sessionId");
        EndAccessMessage message = new EndAccessMessage( uri, id );
        message.setSessionId( sessionId );
        message.setCallback( buildResponseApi( OperationName.ENDACCESSRESPONSE_REST, uri ), CONNECTION.REST );
        return message;
    }

    private final String buildResponseApi( String name, String uri ) {
        try {
            return new URL( new URL( uri ), name ).toString();
        } catch( MalformedURLException e ) {
            return null;
        }
    }

    private void createNotificationChannel() {
        int importance = NotificationManager.IMPORTANCE_DEFAULT;
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "MyChannel", importance);
        channel.setDescription("MyChannelDescription");
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);
    }
}

package ucs.pep;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.support.test.espresso.core.internal.deps.guava.base.Throwables;
import android.widget.Toast;

import com.example.ucsintent.UCSApplication;
import com.example.ucsintent.properties.LocalPepProperties;
import com.example.ucsintent.properties.UCSAppPepProperties;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.bind.JAXBException;

import ucs.constants.CONNECTION;
import ucs.constants.OperationName;
import ucs.exceptions.PolicyException;
import ucs.exceptions.RequestException;
import ucs.contexthandler.ContextHandlerInterface;
import ucs.message.EvaluatedMessage;
import ucs.message.Message;
import ucs.message.endaccess.EndAccessMessage;
import ucs.message.reevaluation.ReevaluationResponseMessage;
import ucs.message.startaccess.StartAccessMessage;
import ucs.message.tryaccess.TryAccessMessage;
import ucs.message.tryaccess.TryAccessResponseMessage;
import ucs.pdp.PDPEvaluation;
import utility.errorhandling.Reject;
import xacml.wrappers.PolicyWrapper;
import xacml.wrappers.RequestWrapper;
import wd_17.DecisionType;
import ucs.core.UCSCoreService;
import utility.AndroidFileUtility;

/**
 * Implementation of a PEP for debugging purposes only.
 */
public class LocalPep {
    private static final Logger log = Logger.getLogger( LocalPep.class.getName() );
    private LocalPepProperties pep;

    private String sessionId = "00000";
    private String evaluation;
    public LocalPep(Context context){
        pep = new LocalPepProperties();

        HandlerThread broadcastHandlerThread = new HandlerThread("LocalPepReceiver");
        broadcastHandlerThread.start();
        Looper looper = broadcastHandlerThread.getLooper();
        Handler broadcastHandler = new Handler(looper);
        IntentFilter intentFilter = new IntentFilter("com.example.ucs.TRY_ACCESS_RESPONSE");
        intentFilter.addAction("com.example.ucs.START_ACCESS_RESPONSE");
        intentFilter.addAction("com.example.ucs.END_ACCESS_RESPONSE");
        intentFilter.addAction("com.example.ucs.ON_GOING_EVALUATION");
        context.registerReceiver(responseBroadcastReceiver, intentFilter, null, broadcastHandler);
    }

    BroadcastReceiver responseBroadcastReceiver = new BroadcastReceiver(){
        @Override
        public void onReceive(Context context, Intent intent) {
            log.info("Arrived message on LocalPepReceiver");
            String id = intent.getStringExtra("id");
            String messageType = intent.getStringExtra("messageType");
            if (!id.equals(pep.getId())) {
                log.info("[" + pep.getId() + "] Discarded message for id: " + id + ", messageType: " + messageType);
                return;
            } else {
                log.info("[" + pep.getId() + "] Accepted message with id: " + id + ", messageType: " + messageType);
            }

            switch (messageType) {
                case "TryAccessResponseMessage":
                    Toast.makeText(context, "Received TryAccess Result: " + intent.getStringExtra("evaluation") + ", sessionId: " + intent.getStringExtra("sessionId"), Toast.LENGTH_SHORT).show();
                    evaluation = intent.getStringExtra("evaluation");
                    sessionId = intent.getStringExtra("sessionId");
                    break;
                case "StartAccessResponseMessage":
                    Toast.makeText(context, "Received StartAccess Result: " + intent.getStringExtra("evaluation"), Toast.LENGTH_SHORT).show();
                    evaluation = intent.getStringExtra("evaluation");
                    break;
                case "EndAccessResponseMessage":
                    Toast.makeText(context, "Received EndAccess Result: " + intent.getStringExtra("evaluation"), Toast.LENGTH_SHORT).show();
                    evaluation = intent.getStringExtra("evaluation");
                    break;
                case "ReevaluationResponseMessage":
                    Toast.makeText(context, "Received Reevaluation Result: " + intent.getStringExtra("evaluation") + ", sessionId: " + intent.getStringExtra("sessionId"), Toast.LENGTH_SHORT).show();
                    evaluation = intent.getStringExtra("evaluation");
                    if (evaluation.equals("Deny")) {
                        endAccess(context);
                    }
                    break;
                default:
                    Toast.makeText(context, "Unrecognized message type", Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    public void setProperties(LocalPepProperties properties){
        this.pep = properties;
    }


    public String tryAccess(Context context){
        Toast.makeText(context, "sendRequestToUCS", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent("com.example.ucs.START_UCS");
        intent.setComponent(new ComponentName("com.example.ucsintent", "com.example.ucsintent.UCSService"));
        intent.putExtra("requestType", "tryAccess");
        intent.putExtra("requestPath", pep.getRequestPath());
        intent.putExtra("policyPath", pep.getPolicyPath());
        intent.putExtra("id", pep.getId());
        intent.putExtra("apiStatusChanged", pep.getApiStatusChanged());
        intent.putExtra("uri", pep.getUri());
        intent.putExtra("revokeType", pep.getRevokeType());
        context.startService(intent);
        return "OK";
    }

    public String startAccess(Context context ) {
        log.log( Level.INFO, "StartAccess at " + System.currentTimeMillis() );
        Intent intent = new Intent("com.example.ucs.START_UCS");
        intent.setComponent(new ComponentName("com.example.ucsintent", "com.example.ucsintent.UCSService"));
        intent.putExtra("requestType", "startAccess");
        intent.putExtra("sessionId", sessionId);
        intent.putExtra("id", pep.getId());
        intent.putExtra("uri", pep.getUri());
        context.startService(intent);
        return "OK";
    }

    public String endAccess( Context context ) {
        log.log( Level.INFO, "StartAccess at " + System.currentTimeMillis() );
        Intent intent = new Intent("com.example.ucs.START_UCS");
        intent.setComponent(new ComponentName("com.example.ucsintent", "com.example.ucsintent.UCSService"));
        intent.putExtra("requestType", "endAccess");
        intent.putExtra("sessionId", sessionId);
        intent.putExtra("id", pep.getId());
        intent.putExtra("uri", pep.getUri());
        context.startService(intent);
        return "OK";
    }
}

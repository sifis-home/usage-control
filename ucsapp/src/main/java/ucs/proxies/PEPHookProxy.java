package ucs.proxies;

import android.content.Context;
import android.content.Intent;

import com.example.ucsintent.UCSApplication;

import java.net.URI;
import java.util.logging.Logger;

import ucs.constants.PURPOSE;
import ucs.message.Message;
import ucs.message.endaccess.EndAccessResponseMessage;
import ucs.message.reevaluation.ReevaluationResponseMessage;
import ucs.message.startaccess.StartAccessResponseMessage;
import ucs.message.tryaccess.TryAccessResponseMessage;
import ucs.pep.PEPInterface;
import ucs.properties.components.PepProperties;
import utility.errorhandling.Reject;

/**
 * Class responsible for sending request replies from UCS to a PEP
 */
public class PEPHookProxy implements PEPInterface {
    private static final Logger log = Logger.getLogger( PEPHookProxy.class.getName() );

    private PepProperties properties;
    private URI uri;

    public PEPHookProxy( PepProperties properties ) {
        Reject.ifNull( properties );
        this.properties = properties;
    }

    @Override
    public Message onGoingEvaluation( ReevaluationResponseMessage message ) {
        Context context = UCSApplication.getContext();
        log.info("ReevaluationResponseMessage found for reply. Id: " + message.getPepId());
        Intent intent = new Intent("com.example.ucs.ON_GOING_EVALUATION");
        intent.putExtra("messageType", "ReevaluationResponseMessage");
        intent.putExtra("evaluation", message.getEvaluation().getResult());
        intent.putExtra("sessionId", message.getSessionId());
        intent.putExtra("id", message.getPepId());
        context.sendBroadcast(intent);
        return null;
    }

    @Override
    public String receiveResponse( Message message ) {
        Context context = UCSApplication.getContext();
        Intent intent = null;
        if(message instanceof TryAccessResponseMessage){
            TryAccessResponseMessage responseMessage = (TryAccessResponseMessage) message;
            log.info("TryAccessResponseMessage found for reply. Id: " + responseMessage.getSource());
            intent = new Intent("com.example.ucs.TRY_ACCESS_RESPONSE");
            intent.putExtra("id", responseMessage.getSource());
            intent.putExtra("messageType", "TryAccessResponseMessage");
            intent.putExtra("evaluation", responseMessage.getEvaluation().getResult());
            intent.putExtra("sessionId", responseMessage.getSessionId());
        } else if(message instanceof StartAccessResponseMessage){
            StartAccessResponseMessage responseMessage = (StartAccessResponseMessage) message;
            log.info("StartAccessResponseMessage found for reply. Id: " + responseMessage.getSource());
            intent = new Intent("com.example.ucs.START_ACCESS_RESPONSE");
            intent.putExtra("id", responseMessage.getSource());
            intent.putExtra("messageType", "StartAccessResponseMessage");
            intent.putExtra("evaluation", responseMessage.getEvaluation().getResult());
        } else if(message instanceof EndAccessResponseMessage){
            EndAccessResponseMessage responseMessage = (EndAccessResponseMessage) message;
            log.info("EndAccessResponseMessage found for reply. Id: " + responseMessage.getSource());
            intent = new Intent("com.example.ucs.END_ACCESS_RESPONSE");
            intent.putExtra("id", responseMessage.getSource());
            intent.putExtra("messageType", "EndAccessResponseMessage");
            intent.putExtra("evaluation", responseMessage.getEvaluation().getResult());
        }
        context.sendBroadcast(intent);
        return "OK";
    }
}

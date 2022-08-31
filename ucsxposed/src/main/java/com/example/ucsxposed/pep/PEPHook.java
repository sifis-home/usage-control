package com.example.ucsxposed.pep;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.support.annotation.RequiresApi;
import android.widget.Toast;

import com.example.ucsxposed.MainActivity;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;

import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import ucs.message.Message;
import ucs.message.reevaluation.ReevaluationResponseMessage;
import ucs.message.tryaccess.TryAccessResponseMessage;
import ucs.pep.PEPInterface;
import utility.FileLogger;

/**
 * Represent a Policy Enforcement Point with an associated Broadcast Receiver to receive replies
 * It can be called in a XPosed hook in order to implement access (usage) control.
 */
public class PEPHook implements PEPInterface {
    private CountDownLatch countDownLatch;
    private final PEPHookProperties pepProperties;
    private Context context;
    private String evaluation;
    private String sessionId;
    private final static String PERMIT = "Permit";
    private final static String DENY = "Deny";

    //Information about which method will need to be called
    private PEPHookReverter pepHookReverter = null;

    public PEPHook(PEPHookProperties pepHookProperties, Context context, PEPHookReverter pepHookReverter){
        this(pepHookProperties, context);
        this.pepHookReverter = pepHookReverter;
    }

    public PEPHook(PEPHookProperties pepHookProperties, Context context){
        this.pepProperties = pepHookProperties;
        this.context = context;
        HandlerThread broadcastHandlerThread = new HandlerThread(pepHookProperties.getId());
        broadcastHandlerThread.start();
        Looper looper = broadcastHandlerThread.getLooper();
        Handler broadcastHandler = new Handler(looper);

        IntentFilter intentFilter = new IntentFilter("com.example.ucs.TRY_ACCESS_RESPONSE");
        intentFilter.addAction("com.example.ucs.START_ACCESS_RESPONSE");
        intentFilter.addAction("com.example.ucs.END_ACCESS_RESPONSE");
        intentFilter.addAction("com.example.ucs.ON_GOING_EVALUATION");


        context.registerReceiver(responseBroadcastReceiver, intentFilter, null, broadcastHandler);
    }

    public void destroy(){
        context.unregisterReceiver(responseBroadcastReceiver);
    }

    BroadcastReceiver responseBroadcastReceiver = new BroadcastReceiver(){
        @Override
        public void onReceive(Context context, Intent intent) {
            String id = intent.getStringExtra("id");
            String messageType = intent.getStringExtra("messageType");
            if(!id.equals(pepProperties.getId())){
                XposedBridge.log("["+pepProperties.getId()+"] Discarded message for id: " + id + ", messageType: " + messageType);
                return;
            } else{
                XposedBridge.log("["+pepProperties.getId()+"] Accepted message with id: " + id + ", messageType: " + messageType);
            }

            if(messageType.equals("TryAccessResponseMessage")){
                Toast.makeText(context, "***HOOK*** \n Received TryAccess Result: " + intent.getStringExtra("evaluation") + ", sessionId: " + intent.getStringExtra("sessionId"), Toast.LENGTH_SHORT).show();
                evaluation = intent.getStringExtra("evaluation");
                sessionId = intent.getStringExtra("sessionId");
                if(countDownLatch.getCount() > 0){
                    countDownLatch.countDown();
                }
            } else if(messageType.equals("StartAccessResponseMessage")){
                Toast.makeText(context, "***HOOK*** \n Received StartAccess Result: " + intent.getStringExtra("evaluation"), Toast.LENGTH_SHORT).show();
                evaluation = intent.getStringExtra("evaluation");
                if(countDownLatch.getCount() > 0){
                    countDownLatch.countDown();
                }
            } else if(messageType.equals("EndAccessResponseMessage")){
                Toast.makeText(context, "***HOOK*** \n Received EndAccess Result: " + intent.getStringExtra("evaluation"), Toast.LENGTH_SHORT).show();
                evaluation = intent.getStringExtra("evaluation");
                Toast.makeText(context, "**HOOK**\n called Method for ending access", Toast.LENGTH_SHORT).show();
                revokeAccess();
                destroy();
            } else if(messageType.equals("ReevaluationResponseMessage")) {
                Toast.makeText(context, "***HOOK*** \n Received Reevaluation Result: " + intent.getStringExtra("evaluation") + ", sessionId: " + intent.getStringExtra("sessionId"), Toast.LENGTH_SHORT).show();
                evaluation = intent.getStringExtra("evaluation");
                if(evaluation.equals(DENY)){
                    endAccess();
                }
            } else{
                Toast.makeText(context, "***HOOK*** \n Unrecognized message type", Toast.LENGTH_SHORT).show();
            }

        }
    };

    private void revokeAccess() {
        pepHookReverter.revert();
    }

    public boolean tryAccess(){
        if(Looper.myLooper() == Looper.getMainLooper())
            Toast.makeText(context, "**Hook**\nSending Try Access with sessionId: " + sessionId, Toast.LENGTH_SHORT).show();

        Intent intent = new Intent("com.example.ucs.START_UCS");
        intent.setComponent(new ComponentName("com.example.ucsintent", "com.example.ucsintent.UCSService"));
        intent.putExtra("requestType", "tryAccess");
        intent.putExtra("requestPath", pepProperties.getRequestPath());
        intent.putExtra("policyPath", pepProperties.getPolicyPath());
        intent.putExtra("id", pepProperties.getId());
        intent.putExtra("apiStatusChanged", pepProperties.getApiStatusChanged());
        intent.putExtra("uri", pepProperties.getUri());
        intent.putExtra("revokeType", pepProperties.getRevokeType());

        context.startService(intent);
        try {
            XposedBridge.log( "["+ Thread.currentThread().getName()+"] Awaiting TryAccessReply");
            countDownLatch = new CountDownLatch(1);
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return evaluation.equals(PERMIT);
    }

    public boolean startAccess(){
        //TryAccess before to obtain the session ID
        if(!this.tryAccess())
            return false;

        Toast.makeText(context, "**Hook**\nSending Start Access with sessionId: " + sessionId, Toast.LENGTH_SHORT).show();

        XposedBridge.log("SessionID: " + sessionId);
        Intent intent = new Intent("com.example.ucs.START_UCS");
        intent.setComponent(new ComponentName("com.example.ucsintent", "com.example.ucsintent.UCSService"));
        intent.putExtra("requestType", "startAccess");
        intent.putExtra("sessionId", sessionId);
        intent.putExtra("id", pepProperties.getId());
        intent.putExtra("uri", pepProperties.getUri());
        context.startService(intent);
        try {
            XposedBridge.log("["+ Thread.currentThread().getName()+"]Awaiting StartAccessReply");
            countDownLatch = new CountDownLatch(1);
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return evaluation.equals(PERMIT);
    }

    public void endAccess(){
        Toast.makeText(context, "**Hook**\nSending End Access with sessionId: " + sessionId, Toast.LENGTH_SHORT).show();
        Intent intent = new Intent("com.example.ucs.START_UCS");
        intent.setComponent(new ComponentName("com.example.ucsintent", "com.example.ucsintent.UCSService"));
        intent.putExtra("requestType", "endAccess");
        intent.putExtra("sessionId", sessionId);
        intent.putExtra("id", pepProperties.getId());
        intent.putExtra("uri", pepProperties.getUri());
        context.startService(intent);

        XposedBridge.log("["+ Thread.currentThread().getName()+"] Skipping awaiting EndAccessReply");
    }


        @Override
    public Message onGoingEvaluation(ReevaluationResponseMessage message) {
        return null;
    }

    @Override
    public String receiveResponse(Message message) {
        return null;
    }
}

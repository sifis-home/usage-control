package it.cnr.iit.ucsdht;

public class MessageReceiver {
    private static final MessageReceiver INSTANCE = new MessageReceiver();
    private String receivedMessage = null;

    public static MessageReceiver getInstance()
    {
        return INSTANCE;
    }

    public void setReceivedMessage(String message) {
        this.receivedMessage = message;
    }

    public String getReceivedMessage() {
        return this.receivedMessage;
    }
}
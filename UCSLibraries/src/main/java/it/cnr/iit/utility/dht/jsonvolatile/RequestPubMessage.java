package it.cnr.iit.utility.dht.jsonvolatile;

public class RequestPubMessage {

    private OuterValue value;

    public RequestPubMessage() {
    }
    public RequestPubMessage(OuterValue outerValue) {
        this.value = outerValue;
    }

    public void setValue(OuterValue outerValue) {
        this.value = outerValue;
    }

    public OuterValue getValue() {
        return value;
    }
}

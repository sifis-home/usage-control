package it.cnr.iit.utility.dht.jsondht.registration;

import it.cnr.iit.ucs.constants.PURPOSE;
import it.cnr.iit.utility.dht.jsondht.MessageContent;

public class RegisterResponse implements MessageContent {
    public String purpose = PURPOSE.REGISTER_RESPONSE.name();
    private String message_id;
    private String code;

    public RegisterResponse() {
    }

    public RegisterResponse(String message_id, String code) {
        this.message_id = message_id;
        this.code = code;
    }
    @Override
    public String getMessage_id() {
        return this.message_id;
    }

    @Override
    public void setMessage_id(String message_id) {
        this.message_id = message_id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}

package it.cnr.iit.utility.dht.jsonvolatile.addpip;

import it.cnr.iit.ucs.constants.PURPOSE;
import it.cnr.iit.utility.dht.jsonvolatile.MessageContent;

import java.util.Map;

public class AddPipRequest implements MessageContent {

    public final String purpose = PURPOSE.ADD_PIP.name();
    private String message_id;
    private String pip_type;
    private String attribute_id;
    private String category;
    private String data_type;
    private long refresh_rate;
    private Map<String, String> additional_properties;

    public AddPipRequest() {
    }

    public AddPipRequest(String message_id, String pip_type, String attribute_id, String category,
                         String data_type, long refresh_rate, Map<String, String> additional_properties) {
        this.message_id = message_id;
        this.pip_type = pip_type;
        this.attribute_id = attribute_id;
        this.category = category;
        this.data_type = data_type;
        this.refresh_rate = refresh_rate;
        this.additional_properties = additional_properties;
    }
    @Override
    public String getMessage_id() {
        return message_id;
    }

    @Override
    public void setMessage_id(String message_id) {
        this.message_id = message_id;
    }


    public String getPip_type() {
        return pip_type;
    }

    public void setPip_type(String pip_type) {
        this.pip_type = pip_type;
    }

    public String getAttribute_id() {
        return attribute_id;
    }

    public void setAttribute_id(String attribute_id) {
        this.attribute_id = attribute_id;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getData_type() {
        return data_type;
    }

    public void setData_type(String data_type) {
        this.data_type = data_type;
    }

    public Map<String, String> getAdditional_properties() {
        return additional_properties;
    }

    public void setAdditional_properties(Map<String, String> additional_properties) {
        this.additional_properties = additional_properties;
    }

    public long getRefresh_rate() {
        return refresh_rate;
    }

    public void setRefresh_rate(long refresh_rate) {
        this.refresh_rate = refresh_rate;
    }
}


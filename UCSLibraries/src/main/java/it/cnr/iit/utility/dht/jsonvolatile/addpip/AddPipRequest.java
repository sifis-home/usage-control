package it.cnr.iit.utility.dht.jsonvolatile.addpip;

import it.cnr.iit.ucs.constants.PURPOSE;
import it.cnr.iit.utility.dht.jsonvolatile.MessageContent;

public class AddPipRequest implements MessageContent {

    public final String purpose = PURPOSE.ADD_PIP.name();
    private String message_id;
    private String pip_type;
    private String attribute_id;
    private String category;
    private String data_type;
    private String attribute_value;
    private String file_name;
    private long refresh_rate;

    public AddPipRequest() {
    }

    public AddPipRequest(String message_id, String pip_type, String attribute_id, String category,
                         String data_type, String attribute_value, String file_name, long refresh_rate) {
        this.message_id = message_id;
        this.pip_type = pip_type;
        this.attribute_id = attribute_id;
        this.category = category;
        this.data_type = data_type;
        this.attribute_value = attribute_value;
        this.file_name = file_name;
        this.refresh_rate = refresh_rate;
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

    public String getAttribute_value() {
        return attribute_value;
    }

    public void setAttribute_value(String attribute_value) {
        this.attribute_value = attribute_value;
    }

    public String getFile_name() {
        return file_name;
    }

    public void setFile_name(String file_name) {
        this.file_name = file_name;
    }

    public long getRefresh_rate() {
        return refresh_rate;
    }

    public void setRefresh_rate(long refresh_rate) {
        this.refresh_rate = refresh_rate;
    }
}


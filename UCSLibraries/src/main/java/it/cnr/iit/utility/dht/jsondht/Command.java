package it.cnr.iit.utility.dht.jsondht;

public class Command {

    private String command_type;
    private InnerValue value;

    public Command() {
    }

    public Command(String command_type, InnerValue innerValue) {
        this.command_type = command_type;
        this.value = innerValue;
    }

    public String getCommand_type() {
        return command_type;
    }

    public void setCommand_type(String command_type) {
        this.command_type = command_type;
    }

    public InnerValue getValue() {
        return value;
    }

    public void setValue(InnerValue value) {
        this.value = value;
    }
}

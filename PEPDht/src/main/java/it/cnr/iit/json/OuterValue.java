package it.cnr.iit.json;

public class OuterValue {

    public long timestamp;
    public Command command;

    public OuterValue(){
    }

    public OuterValue(long timestamp, Command command){
        this.timestamp = timestamp;
        this.command = command;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public Command getCommand() {
        return command;
    }

    public void setCommand(Command command) {
        this.command = command;
    }
}

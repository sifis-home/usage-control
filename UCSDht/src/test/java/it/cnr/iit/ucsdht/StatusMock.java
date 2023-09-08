package it.cnr.iit.ucsdht;

public class StatusMock {

    private static final StatusMock INSTANCE = new StatusMock();
    private String responseToDownloadStatus =
            "{\n" +
            "  \"Response\":{\n" +
            "    \"value\":{\n" +
            "      \"topic_name\":\"SIFIS:UCS\",\n" +
            "      \"topic_uuid\":\"status\",\n" +
            "      \"value\":{\n" +
            "         \"database\":\"\",\n" +
            "         \"pips\":[],\n" +
            "         \"peps\":[],\n" +
            "         \"policies\":[]\n" +
            "      }\n" +
            "    }\n" +
            "  }\n" +
            "}";

    private String responseToUploadStatus =
            "{\n" +
            "   \"Persistent\":{\n" +
            "      \"value\":{\n" +
            "         \"database\":\"\",\n" +
            "         \"pips\":[],\n" +
            "         \"peps\":[],\n" +
            "         \"policies\":[]\n" +
            "      },\n" +
            "      \"topic_name\":\"SIFIS:UCS\",\n" +
            "      \"topic_uuid\":\"status\",\n" +
            "      \"deleted\":false\n" +
            "   }\n" +
            "}";;

    public static StatusMock getInstance()
    {
        return INSTANCE;
    }

    public void setResponseToDownloadStatus(String message) {
        this.responseToDownloadStatus = message;
    }

    public String getResponseToDownloadStatus() {
        return this.responseToDownloadStatus;
    }

    public void setResponseToUploadStatus(String message) {
        this.responseToUploadStatus = message;
    }

    public String getResponseToUploadStatus() {
        return this.responseToUploadStatus;
    }
}

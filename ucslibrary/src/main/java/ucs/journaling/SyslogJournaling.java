package ucs.journaling;

import ucs.properties.base.JournalProperties;

public class SyslogJournaling implements JournalingInterface {

    @Override
    public boolean init( JournalProperties journalProperties ) {
        return false;
    }

    @Override
    public boolean logString( String message ) {
        return false;
    }

    @Override
    public boolean logMultipleStrings( String... strings ) {
        return false;
    }

}

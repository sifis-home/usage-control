package ucs.journaling;

import ucs.properties.base.JournalProperties;

public interface JournalingInterface {

    public boolean init( JournalProperties journalProperties );

    public boolean logString( String message );

    public boolean logMultipleStrings( String... strings );

}

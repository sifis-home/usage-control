package it.cnr.iit.ucs.piptime;

import it.cnr.iit.ucs.exceptions.PIPException;
import it.cnr.iit.ucs.obligationmanager.ObligationInterface;
import it.cnr.iit.ucs.pip.PIPBase;
import it.cnr.iit.ucs.properties.components.PipProperties;
import it.cnr.iit.utility.errorhandling.Reject;
import it.cnr.iit.xacml.Attribute;
import oasis.names.tc.xacml.core.schema.wd_17.RequestType;
import org.wso2.balana.attr.TimeAttribute;

import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

public class PIPTime extends PIPBase {

    private static final Logger log = Logger.getLogger(PIPTime.class.getName());

    public PIPTime(PipProperties properties) {
        super(properties);
        Reject.ifFalse(init(properties), "Error initialising pip : " + properties.getId());
    }


    /**
     * Get the current time and use the TimeAttribute class to encode it.
     * This guarantees that the value is compliant with the standard.
     * Then, this method sets the value within the attribute, and finally
     * returns a String containing such a value.
     *
     * @param attribute the attribute for which we want retrieve the value
     * @return the retrieved value
     */
    @Override
    public String retrieve(Attribute attribute) {
        System.out.println("PIPTime custom retrieve method triggered");
        // Get the current time
        long seconds = new Date().getTime() / 1000;
        long millisWithZeroedMillis = seconds * 1000;
        TimeAttribute t = new TimeAttribute(new Date(millisWithZeroedMillis));
        String value = t.encode();

        attribute.setValue(attribute.getDataType(), value);
        return value;
    }

    @Override
    public void update(String data) throws PIPException {
        log.severe("update() method not implemented");
    }

    @Override
    public void retrieve(RequestType request,
                         List<Attribute> attributeRetrievals) {
        log.severe("Multiple retrieve is unimplemented");
    }

    @Override
    public void subscribe(RequestType request,
                          List<Attribute> attributeRetrieval) {
        log.severe("Multiple subscribe is unimplemented");
    }

    @Override
    public void performObligation(ObligationInterface obligation) {
        if (obligation != null) {
            log.severe("Perform obligation is unimplemented");
        }
    }

    @Override
    protected Logger getLogger() {
        return log;
    }
}


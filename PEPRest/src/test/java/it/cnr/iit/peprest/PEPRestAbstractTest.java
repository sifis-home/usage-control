package it.cnr.iit.peprest;

import java.io.IOException;
import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import it.cnr.iit.peprest.proxy.ProxyRequestManager;
import it.cnr.iit.ucsinterface.message.Message;
import it.cnr.iit.ucsinterface.message.startaccess.StartAccessResponse;
import it.cnr.iit.ucsinterface.message.tryaccess.TryAccessResponse;
import it.cnr.iit.ucsinterface.message.tryaccess.TryAccessResponseContent;
import it.cnr.iit.ucsinterface.pdp.PDPResponse;

import oasis.names.tc.xacml.core.schema.wd_17.DecisionType;
import oasis.names.tc.xacml.core.schema.wd_17.ResponseType;
import oasis.names.tc.xacml.core.schema.wd_17.ResultType;

public abstract class PEPRestAbstractTest {

    protected static final String SESSION_ID_01 = "SessionId_01";
    protected static final String HOST = "localhost:";
    protected static final String PORT = "8081";

    protected MockMvc mvc;

    @MockBean
    protected PEPRest pepRest;

    @MockBean
    protected ProxyRequestManager proxyRequestManager;

    @Autowired
    WebApplicationContext webApplicationContext;

    protected void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup( webApplicationContext ).build();
    }

    protected MockHttpServletResponse postStringResponseToPEPRest( String jsonMessage, String uri ) throws Exception {
        MvcResult mvcResult = mvc.perform( MockMvcRequestBuilders.post( uri )
            .contentType( MediaType.TEXT_PLAIN_VALUE ).content( jsonMessage ) ).andReturn();
        return mvcResult.getResponse();
    }

    protected MockHttpServletResponse postGetRequestToPEPRest( String messageId, String uri ) throws Exception {
        MvcResult mvcResult = mvc.perform( MockMvcRequestBuilders.get( uri ).param( "messageId", messageId )
        /*.requestAttr( "messageId", messageId ) */ ).andReturn();
        return mvcResult.getResponse();
    }

    protected MockHttpServletResponse postResponseToPEPRest( Message jsonMessage, String uri ) throws Exception {
        MvcResult mvcResult = mvc
            .perform( MockMvcRequestBuilders.post( uri ).contentType( MediaType.APPLICATION_JSON_VALUE )
                .accept( MediaType.APPLICATION_JSON_VALUE )
                .content( new ObjectMapper().writeValueAsString( jsonMessage ) ) )
            .andReturn();
        return mvcResult.getResponse();
    }

    protected String mapToJson( Object obj ) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writeValueAsString( obj );
    }

    protected <T> T mapFromJson( String json, Class<T> clazz )
            throws JsonParseException, JsonMappingException, IOException {

        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue( json, clazz );
    }

    protected TryAccessResponse buildTryAccessResponseDeny() {
        PDPResponse pdpEvaluation = buildPDPResponse( DecisionType.DENY );
        TryAccessResponseContent content = new TryAccessResponseContent();
        content.setSessionId( SESSION_ID_01 );
        content.setPDPEvaluation( pdpEvaluation );
        TryAccessResponse tryAccessResponse = new TryAccessResponse( SESSION_ID_01 );
        tryAccessResponse.setTryAccessResponseContent( content );
        return tryAccessResponse;
    }

    protected StartAccessResponse buildStartAccessResponsePermit() {
        PDPResponse pdpEvaluation = buildPDPResponse( DecisionType.PERMIT );
        StartAccessResponse startAccessResponse = new StartAccessResponse( SESSION_ID_01 );
        startAccessResponse.setResponse( pdpEvaluation );
        return startAccessResponse;
    }

    protected PDPResponse buildPDPResponse( DecisionType decision ) {
        ResultType resultType = new ResultType();
        resultType.setDecision( decision );
        ResponseType responseType = new ResponseType();
        responseType.setResult( Arrays.asList( resultType ) );
        PDPResponse pdpResponse = new PDPResponse( responseType );
        return pdpResponse;
    }
}
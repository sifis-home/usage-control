/*******************************************************************************
 * Copyright 2018 IIT-CNR
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package it.cnr.iit.ucs.configuration.fields;

public class PepProperties {

    private String className;
    private String communication;
    private String ip;
    private String port;
    private String revoke = "SOFT";
    private String onGoingEvaluation;
    private String tryAccessResponse;
    private String startAccessResponse;
    private String endAccessResponse;
    private String id;

    public String getCommunication() {
        return communication;
    }

    public String getIp() {
        return ip;
    }

    public String getPort() {
        return port;
    }

    public String getClassName() {
        return className;
    }

    public String getRevoke() {
        return revoke;
    }

    public String getOnGoingEvaluation() {
        return onGoingEvaluation;
    }

    public String getTryAccessResponse() {
        return tryAccessResponse;
    }

    public String getStartAccessResponse() {
        return startAccessResponse;
    }

    public String getEndAccessResponse() {
        return endAccessResponse;
    }

    public String getId() {
        return id;
    }

}

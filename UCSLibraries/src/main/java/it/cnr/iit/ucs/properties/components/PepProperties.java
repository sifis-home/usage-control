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
package it.cnr.iit.ucs.properties.components;

import it.cnr.iit.ucs.properties.base.IdProperties;
import it.cnr.iit.ucs.properties.base.PluginProperties;
import it.cnr.iit.ucs.properties.base.UriProperties;

public interface PepProperties extends PluginProperties, UriProperties, IdProperties {

    public String getRevokeType();

    public String getApiOngoingEvaluation();

    public String getApiTryAccessResponse();

    public String getApiStartAccessResponse();

    public String getApiEndAccessResponse();

}

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

import java.util.List;
import java.util.Map;

import it.cnr.iit.ucs.properties.base.CommonProperties;
import it.cnr.iit.ucs.properties.base.JournalProperties;

public interface PipProperties extends CommonProperties, JournalProperties {
    public List<Map<String, String>> getAttributes();

    public boolean isMultiAttribute();

    public long getRefreshRate();
}

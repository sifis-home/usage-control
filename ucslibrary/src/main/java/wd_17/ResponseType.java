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
package wd_17;

import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Namespace;
import org.simpleframework.xml.Root;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

@XmlAccessorType( XmlAccessType.FIELD )
@Root( name = "Response")
@Namespace(reference = "urn:oasis:names:tc:xacml:3.0:core:schema:wd-17")
public class ResponseType {

    @ElementList(inline = true, required = true )
    protected List<ResultType> result;

    public List<ResultType> getResult() {
        if( result == null ) {
            result = new ArrayList<>();
        }
        return this.result;
    }

    public void setResult( List<ResultType> result ) {
        this.result = result;
    }

}

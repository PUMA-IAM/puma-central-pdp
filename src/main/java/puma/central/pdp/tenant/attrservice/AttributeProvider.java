/*******************************************************************************
 * Copyright 2014 KU Leuven Research and Developement - iMinds - Distrinet 
 * 
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 * 
 *        http://www.apache.org/licenses/LICENSE-2.0
 * 
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *    
 *    Administrative Contact: dnet-project-office@cs.kuleuven.be
 *    Technical Contact: maarten.decat@cs.kuleuven.be
 *    Author: maarten.decat@cs.kuleuven.be
 ******************************************************************************/
package puma.central.pdp.tenant.attrservice;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.ws.Action;
import javax.xml.ws.FaultAction;
import javax.xml.ws.RequestWrapper;
import javax.xml.ws.ResponseWrapper;

/**
 * This class was generated by Apache CXF 2.5.2
 * 2013-10-29T11:24:02.636+01:00
 * Generated source version: 2.5.2
 * 
 */
@WebService(targetNamespace = "http://attrservice.tenant.puma/", name = "AttributeProvider")
@XmlSeeAlso({ObjectFactory.class})
public interface AttributeProvider {

    @WebResult(name = "return", targetNamespace = "")
    @Action(input = "http://attrservice.tenant.puma/AttributeProvider/fetchSupportedAttributeIdsRequest", output = "http://attrservice.tenant.puma/AttributeProvider/fetchSupportedAttributeIdsResponse", fault = {@FaultAction(className = ErrorWhileProcessingException_Exception.class, value = "http://attrservice.tenant.puma/AttributeProvider/fetchSupportedAttributeIds/Fault/ErrorWhileProcessingException")})
    @RequestWrapper(localName = "fetchSupportedAttributeIds", targetNamespace = "http://attrservice.tenant.puma/", className = "puma.central.pdp.tenant.attrservice.FetchSupportedAttributeIds")
    @WebMethod
    @ResponseWrapper(localName = "fetchSupportedAttributeIdsResponse", targetNamespace = "http://attrservice.tenant.puma/", className = "puma.central.pdp.tenant.attrservice.FetchSupportedAttributeIdsResponse")
    public java.util.List<java.lang.String> fetchSupportedAttributeIds() throws ErrorWhileProcessingException_Exception;

    @WebResult(name = "return", targetNamespace = "")
    @Action(input = "http://attrservice.tenant.puma/AttributeProvider/fetchAttributeRequest", output = "http://attrservice.tenant.puma/AttributeProvider/fetchAttributeResponse", fault = {@FaultAction(className = ErrorWhileProcessingException_Exception.class, value = "http://attrservice.tenant.puma/AttributeProvider/fetchAttribute/Fault/ErrorWhileProcessingException"), @FaultAction(className = AttributeNotFoundException_Exception.class, value = "http://attrservice.tenant.puma/AttributeProvider/fetchAttribute/Fault/AttributeNotFoundException")})
    @RequestWrapper(localName = "fetchAttribute", targetNamespace = "http://attrservice.tenant.puma/", className = "puma.central.pdp.tenant.attrservice.FetchAttribute")
    @WebMethod
    @ResponseWrapper(localName = "fetchAttributeResponse", targetNamespace = "http://attrservice.tenant.puma/", className = "puma.central.pdp.tenant.attrservice.FetchAttributeResponse")
    public java.util.List<puma.central.pdp.tenant.attrservice.SimpleAttributeValue> fetchAttribute(
        @WebParam(name = "entityId", targetNamespace = "")
        java.lang.String entityId,
        @WebParam(name = "attributeId", targetNamespace = "")
        java.lang.String attributeId
    ) throws ErrorWhileProcessingException_Exception, AttributeNotFoundException_Exception;
}

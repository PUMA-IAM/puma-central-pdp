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

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the puma.central.pdp.tenant.attrservice package. 
 * <p>An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {

    private final static QName _AttributeNotFoundException_QNAME = new QName("http://attrservice.tenant.puma/", "AttributeNotFoundException");
    private final static QName _FetchAttribute_QNAME = new QName("http://attrservice.tenant.puma/", "fetchAttribute");
    private final static QName _FetchAttributeResponse_QNAME = new QName("http://attrservice.tenant.puma/", "fetchAttributeResponse");
    private final static QName _FetchSupportedAttributeIdsResponse_QNAME = new QName("http://attrservice.tenant.puma/", "fetchSupportedAttributeIdsResponse");
    private final static QName _ErrorWhileProcessingException_QNAME = new QName("http://attrservice.tenant.puma/", "ErrorWhileProcessingException");
    private final static QName _FetchSupportedAttributeIds_QNAME = new QName("http://attrservice.tenant.puma/", "fetchSupportedAttributeIds");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: puma.central.pdp.tenant.attrservice
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link FetchSupportedAttributeIdsResponse }
     * 
     */
    public FetchSupportedAttributeIdsResponse createFetchSupportedAttributeIdsResponse() {
        return new FetchSupportedAttributeIdsResponse();
    }

    /**
     * Create an instance of {@link FetchSupportedAttributeIds }
     * 
     */
    public FetchSupportedAttributeIds createFetchSupportedAttributeIds() {
        return new FetchSupportedAttributeIds();
    }

    /**
     * Create an instance of {@link ErrorWhileProcessingException }
     * 
     */
    public ErrorWhileProcessingException createErrorWhileProcessingException() {
        return new ErrorWhileProcessingException();
    }

    /**
     * Create an instance of {@link FetchAttributeResponse }
     * 
     */
    public FetchAttributeResponse createFetchAttributeResponse() {
        return new FetchAttributeResponse();
    }

    /**
     * Create an instance of {@link FetchAttribute }
     * 
     */
    public FetchAttribute createFetchAttribute() {
        return new FetchAttribute();
    }

    /**
     * Create an instance of {@link AttributeNotFoundException }
     * 
     */
    public AttributeNotFoundException createAttributeNotFoundException() {
        return new AttributeNotFoundException();
    }

    /**
     * Create an instance of {@link SimpleAttributeValue }
     * 
     */
    public SimpleAttributeValue createSimpleAttributeValue() {
        return new SimpleAttributeValue();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link AttributeNotFoundException }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://attrservice.tenant.puma/", name = "AttributeNotFoundException")
    public JAXBElement<AttributeNotFoundException> createAttributeNotFoundException(AttributeNotFoundException value) {
        return new JAXBElement<AttributeNotFoundException>(_AttributeNotFoundException_QNAME, AttributeNotFoundException.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link FetchAttribute }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://attrservice.tenant.puma/", name = "fetchAttribute")
    public JAXBElement<FetchAttribute> createFetchAttribute(FetchAttribute value) {
        return new JAXBElement<FetchAttribute>(_FetchAttribute_QNAME, FetchAttribute.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link FetchAttributeResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://attrservice.tenant.puma/", name = "fetchAttributeResponse")
    public JAXBElement<FetchAttributeResponse> createFetchAttributeResponse(FetchAttributeResponse value) {
        return new JAXBElement<FetchAttributeResponse>(_FetchAttributeResponse_QNAME, FetchAttributeResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link FetchSupportedAttributeIdsResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://attrservice.tenant.puma/", name = "fetchSupportedAttributeIdsResponse")
    public JAXBElement<FetchSupportedAttributeIdsResponse> createFetchSupportedAttributeIdsResponse(FetchSupportedAttributeIdsResponse value) {
        return new JAXBElement<FetchSupportedAttributeIdsResponse>(_FetchSupportedAttributeIdsResponse_QNAME, FetchSupportedAttributeIdsResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link ErrorWhileProcessingException }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://attrservice.tenant.puma/", name = "ErrorWhileProcessingException")
    public JAXBElement<ErrorWhileProcessingException> createErrorWhileProcessingException(ErrorWhileProcessingException value) {
        return new JAXBElement<ErrorWhileProcessingException>(_ErrorWhileProcessingException_QNAME, ErrorWhileProcessingException.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link FetchSupportedAttributeIds }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://attrservice.tenant.puma/", name = "fetchSupportedAttributeIds")
    public JAXBElement<FetchSupportedAttributeIds> createFetchSupportedAttributeIds(FetchSupportedAttributeIds value) {
        return new JAXBElement<FetchSupportedAttributeIds>(_FetchSupportedAttributeIds_QNAME, FetchSupportedAttributeIds.class, null, value);
    }

}

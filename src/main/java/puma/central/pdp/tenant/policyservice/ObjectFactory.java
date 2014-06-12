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
package puma.central.pdp.tenant.policyservice;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the puma.central.pdp.tenant.policyservice package. 
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

    private final static QName _EvaluatePolicyResponse_QNAME = new QName("http://policyservice.pdp.tenant.puma/", "evaluatePolicyResponse");
    private final static QName _GetSupportedPolicyIdsResponse_QNAME = new QName("http://policyservice.pdp.tenant.puma/", "getSupportedPolicyIdsResponse");
    private final static QName _EvaluatePolicy_QNAME = new QName("http://policyservice.pdp.tenant.puma/", "evaluatePolicy");
    private final static QName _GetSupportedPolicyIds_QNAME = new QName("http://policyservice.pdp.tenant.puma/", "getSupportedPolicyIds");
    private final static QName _ErrorWhileProcessingException_QNAME = new QName("http://policyservice.pdp.tenant.puma/", "ErrorWhileProcessingException");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: puma.central.pdp.tenant.policyservice
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link GetSupportedPolicyIds }
     * 
     */
    public GetSupportedPolicyIds createGetSupportedPolicyIds() {
        return new GetSupportedPolicyIds();
    }

    /**
     * Create an instance of {@link ErrorWhileProcessingException }
     * 
     */
    public ErrorWhileProcessingException createErrorWhileProcessingException() {
        return new ErrorWhileProcessingException();
    }

    /**
     * Create an instance of {@link EvaluatePolicyResponse }
     * 
     */
    public EvaluatePolicyResponse createEvaluatePolicyResponse() {
        return new EvaluatePolicyResponse();
    }

    /**
     * Create an instance of {@link GetSupportedPolicyIdsResponse }
     * 
     */
    public GetSupportedPolicyIdsResponse createGetSupportedPolicyIdsResponse() {
        return new GetSupportedPolicyIdsResponse();
    }

    /**
     * Create an instance of {@link EvaluatePolicy }
     * 
     */
    public EvaluatePolicy createEvaluatePolicy() {
        return new EvaluatePolicy();
    }

    /**
     * Create an instance of {@link EncodedCachedAttribute }
     * 
     */
    public EncodedCachedAttribute createEncodedCachedAttribute() {
        return new EncodedCachedAttribute();
    }

    /**
     * Create an instance of {@link SimpleAttributeValue }
     * 
     */
    public SimpleAttributeValue createSimpleAttributeValue() {
        return new SimpleAttributeValue();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link EvaluatePolicyResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://policyservice.pdp.tenant.puma/", name = "evaluatePolicyResponse")
    public JAXBElement<EvaluatePolicyResponse> createEvaluatePolicyResponse(EvaluatePolicyResponse value) {
        return new JAXBElement<EvaluatePolicyResponse>(_EvaluatePolicyResponse_QNAME, EvaluatePolicyResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetSupportedPolicyIdsResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://policyservice.pdp.tenant.puma/", name = "getSupportedPolicyIdsResponse")
    public JAXBElement<GetSupportedPolicyIdsResponse> createGetSupportedPolicyIdsResponse(GetSupportedPolicyIdsResponse value) {
        return new JAXBElement<GetSupportedPolicyIdsResponse>(_GetSupportedPolicyIdsResponse_QNAME, GetSupportedPolicyIdsResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link EvaluatePolicy }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://policyservice.pdp.tenant.puma/", name = "evaluatePolicy")
    public JAXBElement<EvaluatePolicy> createEvaluatePolicy(EvaluatePolicy value) {
        return new JAXBElement<EvaluatePolicy>(_EvaluatePolicy_QNAME, EvaluatePolicy.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetSupportedPolicyIds }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://policyservice.pdp.tenant.puma/", name = "getSupportedPolicyIds")
    public JAXBElement<GetSupportedPolicyIds> createGetSupportedPolicyIds(GetSupportedPolicyIds value) {
        return new JAXBElement<GetSupportedPolicyIds>(_GetSupportedPolicyIds_QNAME, GetSupportedPolicyIds.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link ErrorWhileProcessingException }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://policyservice.pdp.tenant.puma/", name = "ErrorWhileProcessingException")
    public JAXBElement<ErrorWhileProcessingException> createErrorWhileProcessingException(ErrorWhileProcessingException value) {
        return new JAXBElement<ErrorWhileProcessingException>(_ErrorWhileProcessingException_QNAME, ErrorWhileProcessingException.class, null, value);
    }

}

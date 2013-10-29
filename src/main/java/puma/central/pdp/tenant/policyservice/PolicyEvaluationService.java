package puma.central.pdp.tenant.policyservice;

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
 * 2013-10-29T12:20:39.432+01:00
 * Generated source version: 2.5.2
 * 
 */
@WebService(targetNamespace = "http://policyservice.pdp.tenant.puma/", name = "PolicyEvaluationService")
@XmlSeeAlso({ObjectFactory.class})
public interface PolicyEvaluationService {

    @WebResult(name = "return", targetNamespace = "")
    @Action(input = "http://policyservice.pdp.tenant.puma/PolicyEvaluationService/getSupportedPolicyIdsRequest", output = "http://policyservice.pdp.tenant.puma/PolicyEvaluationService/getSupportedPolicyIdsResponse")
    @RequestWrapper(localName = "getSupportedPolicyIds", targetNamespace = "http://policyservice.pdp.tenant.puma/", className = "puma.central.pdp.tenant.policyservice.GetSupportedPolicyIds")
    @WebMethod
    @ResponseWrapper(localName = "getSupportedPolicyIdsResponse", targetNamespace = "http://policyservice.pdp.tenant.puma/", className = "puma.central.pdp.tenant.policyservice.GetSupportedPolicyIdsResponse")
    public java.util.List<java.lang.String> getSupportedPolicyIds();

    @WebResult(name = "return", targetNamespace = "")
    @Action(input = "http://policyservice.pdp.tenant.puma/PolicyEvaluationService/evaluatePolicyRequest", output = "http://policyservice.pdp.tenant.puma/PolicyEvaluationService/evaluatePolicyResponse", fault = {@FaultAction(className = ErrorWhileProcessingException_Exception.class, value = "http://policyservice.pdp.tenant.puma/PolicyEvaluationService/evaluatePolicy/Fault/ErrorWhileProcessingException")})
    @RequestWrapper(localName = "evaluatePolicy", targetNamespace = "http://policyservice.pdp.tenant.puma/", className = "puma.central.pdp.tenant.policyservice.EvaluatePolicy")
    @WebMethod
    @ResponseWrapper(localName = "evaluatePolicyResponse", targetNamespace = "http://policyservice.pdp.tenant.puma/", className = "puma.central.pdp.tenant.policyservice.EvaluatePolicyResponse")
    public java.lang.String evaluatePolicy(
        @WebParam(name = "policyId", targetNamespace = "")
        java.lang.String policyId,
        @WebParam(name = "xacmlRequest", targetNamespace = "")
        java.lang.String xacmlRequest,
        @WebParam(name = "encodedCachedAttributes", targetNamespace = "")
        java.util.List<puma.central.pdp.tenant.policyservice.EncodedCachedAttribute> encodedCachedAttributes
    ) throws ErrorWhileProcessingException_Exception;
}
/*******************************************************************************
 * Copyright 2013 KU Leuven Research and Developement - IBBT - Distrinet 
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
package puma.central.pdp.tenant;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Node;

import puma.central.pdp.tenant.policyservice.ErrorWhileProcessingException_Exception;
import puma.central.pdp.tenant.policyservice.PolicyEvaluationService;

import com.sun.xacml.EvaluationCtx;
import com.sun.xacml.PDP;
import com.sun.xacml.ParsingException;
import com.sun.xacml.SimpleAttributeValue;
import com.sun.xacml.ctx.EncodedCachedAttribute;
import com.sun.xacml.ctx.ResponseCtx;
import com.sun.xacml.ctx.Result;
import com.sun.xacml.remote.RemotePolicyEvaluatorModule;

public class TenantPolicyEvaluatorModule extends RemotePolicyEvaluatorModule {
	
	public static URL TENANT_POLICY_EVALUATOR_WSDL = null;
	static {
		try {
			TENANT_POLICY_EVALUATOR_WSDL = new URL("http://localhost:9003/authorization-service?WSDL");
		} catch (MalformedURLException e) {
			// will not happen with this code
		}
	}

	/**
	 * Our logger
	 */
	private final Logger logger = Logger.getLogger(PDP.class.getName());

	/**
	 * The list of policy ids supported by this module (i.e., the list of policy
	 * ids supported by the tenant policy evaluation service.
	 */
	private List<String> supportedPolicyIds;

	public TenantPolicyEvaluatorModule() {
		supportedPolicyIds = null;
	}

	/**
	 * We do not support evaluation based on a request, we need an id.
	 * 
	 * @return false
	 */
	@Override
	public boolean isRequestSupported() {
		return false;
	}

	/**
	 * We do support policy evaluation based on id.
	 * 
	 * @return true
	 */
	@Override
	public boolean isIdReferenceSupported() {
		return true;
	}

	/**
	 * Returns whether this module supports the given RemotePolicyReference
	 * PolicyId. This is based on the list of ids provided by the tenant policy
	 * evaluation service.
	 * 
	 * @param id
	 *            The PolicyId attribute value of the RemotePolicyReference
	 *            element.
	 */
	@Override
	public boolean supportsId(URI id) {
		return this.supportsId(id.toString());
	}

	/**
	 * Returns whether this module supports the given RemotePolicyReference
	 * PolicyId. This is based on the list of ids provided by the tenant policy
	 * evaluation service.
	 * 
	 * @param id
	 *            The PolicyId attribute value of the RemotePolicyReference
	 *            element.
	 */
	public boolean supportsId(String id) {
		if (this.supportedPolicyIds == null) {
			PolicyEvaluationService port = PolicyEvaluationWebServiceCache.getInstance()
					.getPort(getEndpointURL("not-implemented"));
			this.supportedPolicyIds = port.getSupportedPolicyIds();
			// log the supported ids
			String supported = "Fetched supported ids from tenant policy evaluation service:\n";
			for (String s : this.supportedPolicyIds) {
				supported += "- " + s + "\n";
			}
			this.logger.info(supported);
		}
		return this.supportedPolicyIds.contains(id);
	}

	/**
	 * Not supported by this module.
	 * 
	 * @return new Result(Result.DECISION_NOT_APPLICABLE)
	 */
	@Override
	public Result findAndEvaluate(EvaluationCtx context) {
		return new Result(Result.DECISION_NOT_APPLICABLE);
	}

	/**
	 * Find the policy defined by the id and evaluates it using the context.
	 * 
	 * Returns the decision of the tenant as a XACML result. For now, only takes
	 * into account the decision itself, not any obligations etc!!!
	 */
	@Override
	public Result findAndEvaluate(URI id, EvaluationCtx context) {
		// to be sure, check whether we support the given id
		if (!supportsId(id)) {
			logger.warning("Retrieved an id which was not supported: " + id);
			return new Result(Result.DECISION_NOT_APPLICABLE);
		}

		// the actual implementation
		// 1. get the end point URL for this PolicyId
		URL endpointURL = getEndpointURL("not-implemented");
		// 2. build the service stub
		PolicyEvaluationService port = PolicyEvaluationWebServiceCache.getInstance().getPort(
				endpointURL);
		// 3. build the request
		String xacmlRequest = nodeToString(context.getRequestRoot());
		// 4. ask for a response
		String responseAsString;
		List<EncodedCachedAttribute> encodedCachedAttributes = context
				.getEncodedCachedAttributes();
		try {
			responseAsString = port.evaluatePolicy(id.toString(), xacmlRequest,
					convert(encodedCachedAttributes));
			logger.info("FLOW: evaluated #" + id.toString() + " at tenant");
		} catch (ErrorWhileProcessingException_Exception e) {
			// MessageNotSupportedException or ErrorWhileProcessingException
			logger.log(Level.WARNING,
					"Error when asking for tenant's authoriziation decision", e);
			return new Result(Result.DECISION_INDETERMINATE);
		}
		logger.fine("Tenant's authorization decision: " + responseAsString);

		// 5. decode the response
		InputStream is;
		try {
			is = new ByteArrayInputStream(responseAsString.getBytes("UTF-8"));
		} catch (UnsupportedEncodingException e) {
			logger.log(Level.WARNING, "Error when encoding response string", e);
			return new Result(Result.DECISION_INDETERMINATE);
		}
		ResponseCtx responseCtx;
		try {
			responseCtx = ResponseCtx.getInstance(is);
		} catch (ParsingException e) {
			logger.log(Level.WARNING, "Error when parsing tenant response", e);
			return new Result(Result.DECISION_INDETERMINATE);
		}
		Set<Result> results = (Set<Result>) responseCtx.getResults();
		if (results.size() != 1) {
			logger.warning("incorrect number of results retrieved from tenant: "
					+ results.size());
			return new Result(Result.DECISION_INDETERMINATE);
		}
		// we only have one result
		Result result = null;
		for (Result r : results) {
			result = r;
		}
		return result;
	}

	/**
	 * Helper method to get the location of the tenant's PDP SOAP end point (to
	 * be more precise: the location of the WSDL of the service).
	 * 
	 * For now, this is just a static mapping from tenantId to end point URL. In
	 * a real environment, the SaaS provider would of course have a database of
	 * this configuration and mapping or something similar.
	 */
	private URL getEndpointURL(String tenantId) {
		// for now: just a single endpoint
		return TENANT_POLICY_EVALUATOR_WSDL;
	}

	/**
	 * 
	 * @param node
	 * @return
	 */
	private static String nodeToString(Node node) {
		StringWriter sw = new StringWriter();
		try {
			Transformer t = TransformerFactory.newInstance().newTransformer();
			t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
			t.setOutputProperty(OutputKeys.INDENT, "yes");
			t.transform(new DOMSource(node), new StreamResult(sw));
		} catch (TransformerException te) {
			te.printStackTrace();
		}
		return sw.toString();
	}

	/**
	 * Converts classed for the web service.
	 */
	private List<puma.central.pdp.tenant.policyservice.EncodedCachedAttribute> convert(
			List<EncodedCachedAttribute> encodedCachedAttributes) {
		List<puma.central.pdp.tenant.policyservice.EncodedCachedAttribute> result = new ArrayList<puma.central.pdp.tenant.policyservice.EncodedCachedAttribute>();
		for (EncodedCachedAttribute eca : encodedCachedAttributes) {
			puma.central.pdp.tenant.policyservice.EncodedCachedAttribute newEca = new puma.central.pdp.tenant.policyservice.EncodedCachedAttribute();
			newEca.setId(eca.getId());
			newEca.setType(eca.getType());
			newEca.getEncodedBagAttribute()
					.addAll(convertSimpleAttributeValueList(eca.getEncodedBagAttribute()));
			result.add(newEca);
		}
		return result;
	}
	
	private List<puma.central.pdp.tenant.policyservice.SimpleAttributeValue> convertSimpleAttributeValueList(
			List<SimpleAttributeValue> values) {
		List<puma.central.pdp.tenant.policyservice.SimpleAttributeValue> result = new ArrayList<puma.central.pdp.tenant.policyservice.SimpleAttributeValue>();
		for(SimpleAttributeValue value: values) {
			result.add(convert(value));
		}
		return result;
	}

	private puma.central.pdp.tenant.policyservice.SimpleAttributeValue convert(
			SimpleAttributeValue value) {
		puma.central.pdp.tenant.policyservice.SimpleAttributeValue result = new puma.central.pdp.tenant.policyservice.SimpleAttributeValue();
		result.setType(value.getType());
		result.setValue(value.getValue());
		return result;
	}

}

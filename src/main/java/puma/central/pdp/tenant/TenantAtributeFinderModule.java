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

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import puma.central.pdp.tenant.attrservice.AttributeNotFoundException_Exception;
import puma.central.pdp.tenant.attrservice.AttributeProvider;
import puma.central.pdp.tenant.attrservice.AttributeProviderService;
import puma.central.pdp.tenant.attrservice.ErrorWhileProcessingException_Exception;

import com.sun.xacml.EvaluationCtx;
import com.sun.xacml.SimpleAttributeValue;
import com.sun.xacml.attr.AttributeDesignator;
import com.sun.xacml.attr.BagAttribute;
import com.sun.xacml.attr.StringAttribute;
import com.sun.xacml.cond.EvaluationResult;
import com.sun.xacml.ctx.Status;
import com.sun.xacml.finder.AttributeFinderModule;

/**
 * An attribute finder module that contacts the tenant using the SAML web
 * service.
 * 
 * @author maartend
 * 
 */
public class TenantAtributeFinderModule extends AttributeFinderModule {

	/**
	 * The logger we'll use for all messages
	 */
	private static final Logger logger = Logger
			.getLogger(TenantAtributeFinderModule.class.getName());

	private static URL TENANT_ATTRIBUTE_PROVIDER_WSDL = null;
	static {
		try {
			TENANT_ATTRIBUTE_PROVIDER_WSDL = new URL("http://localhost:9001/attribute-service?WSDL");
		} catch(MalformedURLException e) {
			// should not happen
			e.printStackTrace();
		}
	}

	/**
	 * The proxy to the tenant's SAML SOAP web service.
	 */
	private AttributeProvider tenantAS;

	/**
	 * 
	 */
	private Set<String> supportedIds;

	/**
	 * The URI identifying the subject id element.
	 */
	public static URI subjectIdIdentifier = null;
	static {
		try {
			subjectIdIdentifier = new URI(
					"urn:oasis:names:tc:xacml:1.0:subject:subject-id");
		} catch (URISyntaxException e) {
			// will not happen with this code
		}
	}

	/**
	 * The URI identifying the resource id element.
	 */
	public static URI resourceIdIdentifier = null;
	static {
		try {
			resourceIdIdentifier = new URI(
					"urn:oasis:names:tc:xacml:1.0:resource:resource-id");
		} catch (URISyntaxException e) {
			// will not happen with this code
		}
	}

	public TenantAtributeFinderModule() {

	}

	private void setUpTenant() {
		if (tenantAS == null) {
			AttributeProviderService service = new AttributeProviderService(TENANT_ATTRIBUTE_PROVIDER_WSDL);
			this.tenantAS = service.getAttributeProviderPort();
		}
	}

	/**
	 * We only support designators, not selectors.
	 */
	@Override
	public boolean isDesignatorSupported() {
		return true;
	}

	/**
	 * We only support subject attributes.
	 */
	@Override
	public Set<Integer> getSupportedDesignatorTypes() {
		Set<Integer> set = new HashSet<Integer>();
		set.add(AttributeDesignator.SUBJECT_TARGET);
		set.add(AttributeDesignator.RESOURCE_TARGET);
		set.add(AttributeDesignator.ENVIRONMENT_TARGET);
		return set;
	}

	/**
	 * Returns the one identifier this module supports.
	 */
	@Override
	public Set<String> getSupportedIds() {
		setUpTenant();
		if (this.supportedIds == null) {
			this.supportedIds = new HashSet<String>();
			try {
				this.supportedIds.addAll(tenantAS.fetchSupportedAttributeIds());
			} catch (ErrorWhileProcessingException_Exception e) {
				e.printStackTrace();
				return new HashSet<String>();
			}
		}
		return this.supportedIds;
	}

	/**
	 * Returns whether this module supports the given id.
	 */
	public boolean supportsId(String id) {
		for (String s : getSupportedIds()) {
			if (id.equals(s)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns whether this module supports the given id.
	 */
	public boolean supportsId(URI id) {
		return supportsId(id.toString());
	}

	/**
	 * Helper function for constructing an empty bag result;
	 */
	private EvaluationResult emptyBagResult(URI attributeType) {
		return new EvaluationResult(BagAttribute.createEmptyBag(attributeType));
	}

	/**
	 * Retrieve an attribute from the database.
	 * 
	 * If the attribute cannot be found (this is not an error!), returns an
	 * empty bag.
	 */
	public EvaluationResult findAttribute(URI attributeType, URI attributeId,
			URI issuer, URI subjectCategory, EvaluationCtx context,
			int designatorType) {
		// make sure we support this attribute id
		if (!supportsId(attributeId)) {
			return emptyBagResult(attributeType);
		}

//		// make sure we've been asked for a string
//		if (!attributeType.toString().equals(StringAttribute.identifier)) {
//			return emptyBagResult(attributeType);
//		}

		// make sure the tenant web service has been initialized
		setUpTenant();

		// We're OK to go, so start with fetching the
		// subject id (a lot of cruft...)
		EvaluationResult subjectIdResult = context.getSubjectAttribute(
				StringAttribute.identifierURI, subjectIdIdentifier, issuer,
				subjectCategory);
		if (subjectIdResult.indeterminate()) {
			return subjectIdResult;
		}
		// check that we succeeded in getting the subject identifier
		BagAttribute subjectIdBag = (BagAttribute) (subjectIdResult
				.getAttributeValue());
		if (subjectIdBag.isEmpty()) {
			List<String> code = new ArrayList<String>();
			code.add(Status.STATUS_MISSING_ATTRIBUTE);
			Status status = new Status(code, "missing subject-id");
			return new EvaluationResult(status);
		} else if (subjectIdBag.size() > 1) {
			List<String> code = new ArrayList<String>();
			code.add(Status.STATUS_PROCESSING_ERROR);
			Status status = new Status(code, "multiple subject ids");
			return new EvaluationResult(status);
		}
		// now get the last (and only) element in the bag
		String subjectId = null;
		for (Object o : subjectIdBag) {
			subjectId = ((StringAttribute) o).getValue();
		}
		assert (subjectId != null);
		logger.fine("Subject identifier: " + subjectId);

		// Also fetch the resource id (also a lot of cruft...)
		EvaluationResult resourceIdResult = context.getResourceAttribute(
				StringAttribute.identifierURI, resourceIdIdentifier, issuer);
		if (resourceIdResult.indeterminate()) {
			return resourceIdResult;
		}
		// check that we succeeded in getting the resource identifier
		BagAttribute resourceIdBag = (BagAttribute) (resourceIdResult
				.getAttributeValue());
		if (resourceIdBag.isEmpty()) {
			List<String> code = new ArrayList<String>();
			code.add(Status.STATUS_MISSING_ATTRIBUTE);
			Status status = new Status(code, "missing resource-id");
			return new EvaluationResult(status);
		} else if (resourceIdBag.size() > 1) {
			List<String> code = new ArrayList<String>();
			code.add(Status.STATUS_PROCESSING_ERROR);
			Status status = new Status(code, "multiple resource ids");
			return new EvaluationResult(status);
		}
		// now get the last (and only) element in the bag
		String resourceId = null;
		for (Object o : resourceIdBag) {
			resourceId = ((StringAttribute) o).getValue();
		}
		assert (resourceId != null);
		logger.fine("Resource identifier: " + resourceId);

		// now that we have the subject and resource id: retrieve the necessary
		// attributes from the tenant
		String entityId = subjectId;
		if (designatorType == AttributeDesignator.ENVIRONMENT_TARGET) {
			entityId = null;
		} else if (designatorType == AttributeDesignator.RESOURCE_TARGET) {
			entityId = resourceId;
		}

		BagAttribute bag;
		try {
			List<puma.central.pdp.tenant.attrservice.SimpleAttributeValue> response = tenantAS
					.fetchAttribute(entityId, attributeId.toString());
			bag = BagAttribute
					.fromEncodedSet(convertSimpleAttributeValues(response));
			logger.info("FLOW: Fetched " + attributeId + " for entity #"
					+ entityId + " from tenant.");
		} catch (ErrorWhileProcessingException_Exception e) {
			e.printStackTrace();
			List<String> code = new ArrayList<String>();
			code.add(Status.STATUS_PROCESSING_ERROR);
			Status status = new Status(code,
					"error while processing tenant attribute fetch");
			return new EvaluationResult(status);
		} catch (AttributeNotFoundException_Exception e) {
			e.printStackTrace();
			return emptyBagResult(attributeType);
		}

		return new EvaluationResult(bag);
	}

	private List<SimpleAttributeValue> convertSimpleAttributeValues(
			List<puma.central.pdp.tenant.attrservice.SimpleAttributeValue> values) {
		List<SimpleAttributeValue> list = new ArrayList<SimpleAttributeValue>();
		for (puma.central.pdp.tenant.attrservice.SimpleAttributeValue value : values) {
			list.add(convertSimpleAttributeValue(value));
		}
		return list;
	}

	private SimpleAttributeValue convertSimpleAttributeValue(
			puma.central.pdp.tenant.attrservice.SimpleAttributeValue value) {
		return new SimpleAttributeValue(value.getType(), value.getValue());
	}
}

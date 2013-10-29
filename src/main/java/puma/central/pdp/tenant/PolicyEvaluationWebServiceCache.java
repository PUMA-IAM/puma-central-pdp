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

import java.net.URL;
import java.util.HashMap;
import java.util.logging.Logger;

import puma.central.pdp.tenant.policyservice.PolicyEvaluationService;
import puma.central.pdp.tenant.policyservice.PolicyEvaluationServiceService;

/**
 * Class used for caching web service connections/instances/ports.
 * A similar class was not present in the provider-side prototype,
 * but there, a fixed tenant location was assumed and only one connection
 * was maintained the whole time. This implementation is cleaner.
 * 
 * @author maartend
 *
 */
public class PolicyEvaluationWebServiceCache {
	
	private static final Logger logger = Logger.getLogger(PolicyEvaluationWebServiceCache.class.getName());
	
	private static PolicyEvaluationWebServiceCache instance;
	
	public static PolicyEvaluationWebServiceCache getInstance() {
		if(instance == null) {
			instance = new PolicyEvaluationWebServiceCache();
		}
		return instance;
	}
	
	private PolicyEvaluationWebServiceCache() {
		
	}
	
	private HashMap<URL, PolicyEvaluationService> ports = new HashMap<URL, PolicyEvaluationService>();
	
	public PolicyEvaluationService getPort(URL endpointURL) {
		if(ports.containsKey(endpointURL)) {
			logger.fine("Found cached port");
			return ports.get(endpointURL);
		} else {
			PolicyEvaluationServiceService tenantService = new PolicyEvaluationServiceService(
					endpointURL);
			PolicyEvaluationService port = tenantService.getPolicyEvaluationServicePort();
			ports.put(endpointURL, port);
			return port;
		}
	}
	
	

}

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
package puma.central.pdp;

import java.io.IOException;
import java.util.logging.LogManager;

import puma.peputils.Action;
import puma.peputils.Environment;
import puma.peputils.PEP;
import puma.peputils.Subject;
import puma.peputils.attributes.ObjectAttributeValue;
import puma.peputils.attributes.SubjectAttributeValue;

public class OfflinePEPTest {

	public static void main(String[] args) {
		CentralPUMAPDP pdp;
		try {
			pdp = new CentralPUMAPDP(
					"/home/maartend/PhD/code/workspace-jee/puma-central-pdp/resources/policies",
					"edocs-provider-policy");
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("Quitting because of IOException");
			return;
		}
		
		System.out.println("Press enter to start");
		try {
			System.in.read();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		PEP pep = new CentralPUMAPEP(pdp);
		long startTime = System.nanoTime();
		int nbRuns = 1000000;
		for(int i = 0; i < nbRuns; i++) {
			testProviderConfidential(pep);
			if(i == 100) {
				// no logging anymore
				LogManager.getLogManager().reset();
			}
			if(i == (nbRuns / 10)) {
				System.out.println("10%");
			}
		}
		long endTime = System.nanoTime();
		long duration = endTime - startTime;
		System.out.println("time was " + (duration / 100000) + "ms for " + nbRuns + "runs");
		testProviderHelpdeskViewMetadata(pep);
		testProviderHelpdeskUpdateMetadata(pep);
		testProviderHelpdeskViewDocumentAssignedTenant(pep);
	}

	public static void testProviderConfidential(PEP pep) {
		// 0. The PDP is already initialized by the PDPInitializer

		// 1. First build your subject, object, action and environment, for
		// example
		// based on the current Session or some parameters in the request
		Subject subject = new Subject("s123");
		subject.addAttributeValue(new SubjectAttributeValue("tenant", "provider"));

		puma.peputils.Object object = new puma.peputils.Object("o123");
		object.addAttributeValue(new ObjectAttributeValue("type", "document"));
		object.addAttributeValue(new ObjectAttributeValue("confidential", true));

		Action action = new Action("view");

		Environment environment = new Environment();

		// 2. Then just ask the PEP for a decision
		boolean authorized = pep.isAuthorized(subject, object, action,
				environment);

		// 3. Assert the decision
		assert(authorized == false);
	}

	public static void testProviderHelpdeskViewMetadata(PEP pep) {
		// 0. The PDP is already initialized by the PDPInitializer

		// 1. First build your subject, object, action and environment, for
		// example
		// based on the current Session or some parameters in the request
		Subject subject = new Subject("maarten");
		subject.addAttributeValue(new SubjectAttributeValue("tenant", "provider"));
		subject.addAttributeValue(new SubjectAttributeValue("roles", "helpdesk"));

		puma.peputils.Object object = new puma.peputils.Object("123");
		object.addAttributeValue(new ObjectAttributeValue("type", "document_metadata"));

		Action action = new Action("view");

		Environment environment = new Environment();

		// 2. Then just ask the PEP for a decision
		boolean authorized = pep.isAuthorized(subject, object, action,
				environment);

		// 3. Assert the decision
		assert(authorized == true);
	}

	public static void testProviderHelpdeskUpdateMetadata(PEP pep) {
		// 0. The PDP is already initialized by the PDPInitializer

		// 1. First build your subject, object, action and environment, for
		// example
		// based on the current Session or some parameters in the request
		Subject subject = new Subject("maarten");
		subject.addAttributeValue(new SubjectAttributeValue("tenant", "provider"));
		subject.addAttributeValue(new SubjectAttributeValue("roles", "helpdesk"));

		puma.peputils.Object object = new puma.peputils.Object("123");
		object.addAttributeValue(new ObjectAttributeValue("type", "document_metadata"));

		Action action = new Action("update");

		Environment environment = new Environment();

		// 2. Then just ask the PEP for a decision
		boolean authorized = pep.isAuthorized(subject, object, action,
				environment);

		// 3. Assert the decision
		assert(authorized == false);
	}

	public static void testProviderHelpdeskViewDocumentAssignedTenant(PEP pep) {
		// 0. The PDP is already initialized by the PDPInitializer

		// 1. First build your subject, object, action and environment, for
		// example
		// based on the current Session or some parameters in the request
		Subject subject = new Subject("maarten");
		subject.addAttributeValue(new SubjectAttributeValue("tenant", "provider"));
		subject.addAttributeValue(new SubjectAttributeValue("roles", "helpdesk"));
		subject.addAttributeValue(new SubjectAttributeValue("assigned_tenants", "tenant1"));

		puma.peputils.Object object = new puma.peputils.Object("123");
		object.addAttributeValue(new ObjectAttributeValue("type", "document"));
		object.addAttributeValue(new ObjectAttributeValue("owning_tenant", "tenant1"));

		Action action = new Action("update");

		Environment environment = new Environment();

		// 2. Then just ask the PEP for a decision
		boolean authorized = pep.isAuthorized(subject, object, action,
				environment);

		// 3. Assert the decision
		assert(authorized == false);
	}

	public static void testProviderPolicy(PEP pep) {
		// 0. The PDP is already initialized by the PDPInitializer

		// 1. First build your subject, object, action and environment, for
		// example
		// based on the current Session or some parameters in the request
		Subject subject = new Subject("maarten");
		subject.addAttributeValue(new SubjectAttributeValue("tenant", "provider"));
		SubjectAttributeValue roles = new SubjectAttributeValue("roles");
		roles.addValue("phd");
		roles.addValue("imindsr");
		subject.addAttributeValue(roles);
		subject.addAttributeValue(new SubjectAttributeValue("departement",
				"computer-science"));
		subject.addAttributeValue(new SubjectAttributeValue("email",
				"maarten.decat@cs.kuleuven.be"));

		puma.peputils.Object object = new puma.peputils.Object("123");
		object.addAttributeValue(new ObjectAttributeValue("type", "document"));
		object.addAttributeValue(new ObjectAttributeValue("owning-tenant",
				"KBC"));
		object.addAttributeValue(new ObjectAttributeValue("sender", "jasper"));
		ObjectAttributeValue destinations = new ObjectAttributeValue(
				"destination", "bert@kbc-leasing.be");
		object.addAttributeValue(destinations);
		object.addAttributeValue(new ObjectAttributeValue("confidential", true));

		Action action = new Action("view");

		Environment environment = new Environment();

		// 2. Then just ask the PEP for a decision
		boolean authorized = pep.isAuthorized(subject, object, action,
				environment);

		// 3. Assert the decision
		assert(authorized == false);
	}

}

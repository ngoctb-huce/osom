/*-
 * ========================LICENSE_START=================================
 * org.etsi.osl.osom
 * %%
 * Copyright (C) 2019 openslice.io
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =========================LICENSE_END==================================
 */
package org.etsi.osl.osom;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.camel.CamelContext;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.etsi.osl.osom.partnerservices.PartnerOrganizationServicesManager;
import org.flowable.engine.ManagementService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.job.api.Job;
import org.flowable.spring.impl.test.FlowableSpringExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.etsi.osl.tmf.pm632.model.Organization;

@ExtendWith(FlowableSpringExtension.class)
@SpringBootTest(properties = { "CATALOG_GET_SERVICEORDER_BY_ID = direct:get_mocked_order",
		"CATALOG_GET_SERVICESPEC_BY_ID = direct:get_mocked_spec", "CATALOG_ADD_SERVICE = direct:get_mocked_add_service",
		"CATALOG_UPD_SERVICEORDER_BY_ID = direct:get_mocked_upd_order",
		"CATALOG_ADD_SERVICEORDER = direct:get_mocked_upd_order",
		"CATALOG_GET_SERVICE_BY_ID = direct:get_mocked_service_id",
		"CATALOG_GET_SERVICE_BY_ORDERID = direct:get_mocked_service_id",
		"CATALOG_SERVICE_QUEUE_ITEMS_GET: direct:get_mocked_service_id",
		"CATALOG_SERVICE_QUEUE_ITEM_UPD: direct:get_mocked_service_id",
		"CATALOG_SERVICE_QUEUE_ITEM_DELETE: direct:get_mocked_service_id",
		"CATALOG_UPD_SERVICE = direct:get_mocked_upd_service", "NFV_CATALOG_DEPLOY_NSD_REQ = direct:req_deploy_nsd",
		"NFV_CATALOG_GET_DEPLOYMENT_BY_ID = direct:req_deployment_id", 
		"NFV_CATALOG_UPD_DEPLOYMENT_BY_ID = direct:req_deployment_id", 
		 "CATALOG_GET_EXTERNAL_SERVICE_PARTNERS = direct:get_mocked_partners",
		 "CATALOG_UPD_EXTERNAL_SERVICESPEC = direct:upd_external_specs",
			"CATALOG_GET_SERVICETESTSPEC_BY_ID = direct:get_mocked_service_queueitems",	
			"CATALOG_ADD_SERVICETEST = direct:get_mocked_service_queueitems",	
			"CATALOG_UPD_SERVICETEST = direct:get_mocked_service_queueitems",	
			"CATALOG_GET_SERVICETEST_BY_ID = direct:get_mocked_service_queueitems",	
			
		 
		"uri.to   = mock:output" })
@ActiveProfiles("testing")
public class ProcessPartnerServicesIntegrationTest {
	private static final transient Log logger = LogFactory.getLog(ProcessPartnerServicesIntegrationTest.class.getName());

	@Autowired
	RepositoryService repositoryService;

	@Autowired
	private RuntimeService runtimeService;


	@Autowired
	private ManagementService managementService;
	
	@Autowired
	private TaskService taskService;

	@Autowired
	private CamelContext camelContext;

	@Autowired
	private PartnerOrganizationServicesManager partnerOrganizationServicesManager;

	SPMocked spmocked = new SPMocked();

	@Test
	// @Deployment(resources = { "processes/ServiceOrder.bpmn" })
	public void startProcess() throws Exception {


//		repositoryService.suspendProcessDefinitionByKey("OrderSchedulerProcess"); // this is to stop the timer
//		repositoryService.suspendProcessDefinitionByKey("fetchInRpogressOrdersProcess"); // this is to stop the timer
		//repositoryService.suspendProcessDefinitionByKey("fetchPartnerServicesProcess"); // this is to stop the timer
		
		/**
		 * configure here the mocked routes
		 */
		RoutesBuilder builder = new RouteBuilder() {
			@Override
			public void configure() {
				from("direct:get_mocked_partners").bean(spmocked, "getPartners");
				from("direct:upd_external_specs").bean(spmocked, "updateExternalSpecs");
				
			};
		};

		camelContext.addRoutes(builder);

		logger.info("waiting 1secs");
		Thread.sleep(1000); // wait

		List<Organization> orgz = partnerOrganizationServicesManager.retrievePartners();
		assertThat( orgz ).isInstanceOf( List.class);

		assertThat( orgz ).hasSize(1);
		assertThat( orgz.get(0).getPartyCharacteristic() ).hasSize(11);
		assertThat( orgz.get(0).findPartyCharacteristic("EXTERNAL_TMFAPI_BASEURL").getValue().getValue() ).isEqualTo( "http://portal.openslice.io" );
		
		
		//{"OAUTH2CLIENTSECRET":"secret","OAUTH2TOKENURI":"http://portal.openslice.io/osapi-oauth-server/oauth/token","OAUTH2SCOPES":["admin","read"],"PASSWORD":"openslice","BASEURL":"http://portal.openslice.io","USERNAME":"admin","CLIENTREGISTRATIONID":"authOpensliceProvider","OAUTH2CLIENTID":"osapiWebClientId"}
//		String strapiparams = orgz.get(0).getPartyCharacteristic().stream().findFirst().get().getValue().getValue();
//		
//		ObjectMapper mapper = new ObjectMapper();
//		mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
//		Map<String, Object> apiparams = mapper.readValue(strapiparams, Map.class);
//		
//		assertThat( apiparams ).hasSize( 8 );
//		assertThat( apiparams.get("CLIENTREGISTRATIONID")).isEqualTo("authOpensliceProvider");
//		assertThat(  apiparams.get("OAUTH2SCOPES")).isInstanceOf( ArrayList.class ); 
//		assertThat(  (ArrayList) apiparams.get("OAUTH2SCOPES")).hasSize( 2 );
		
		//Job timer = managementService.createTimerJobQuery().jobId("timerstarteventFetchPartnerServices").singleResult();
		//repositoryService.activateProcessDefinitionByKey( "fetchPartnerServicesProcess" );
		//runtimeService.startProcessInstanceById("fetchPartnerServicesProcess"  );
		
		
		
		List<Job> jobs = managementService.createTimerJobQuery().list();
		logger.info( "jobs.size() " +   jobs.size());
		for (Job timer : jobs) {
			logger.info( "Timer getExecutionId " +   timer.getExecutionId() );
			logger.info( "Timer getId " + timer.getId()  );
			logger.info( "Timer getJobHandlerConfiguration " + timer.getJobHandlerConfiguration() );
			logger.info( "Timer getElementName " + timer.getElementName()  );
			if ( timer.getJobHandlerConfiguration().contains( "timerstarteventFetchPartnerServices" ) ) {
				managementService.moveTimerToExecutableJob(timer.getId());
				//managementService.executeJob(timer.getId());			
				
			}
			//logger.info( "Timer details" + runtimeService.getActiveActivityIds( timer.getId() ));
		}
		
		
		
		logger.info("waiting 20secs");
		Thread.sleep( 20000 ); // wait

		/**
		 * this one needs to be online.. not fully mocked yet
		 */
		
		//assertThat( spmocked.getUpdatedSpecs() ).hasSize(25);
	}



}

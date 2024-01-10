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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.camel.CamelContext;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.etsi.osl.osom.lcm.LCMRulesExecutor;
import org.etsi.osl.osom.lcm.LCMRulesExecutorVariables;
import org.etsi.osl.osom.management.ServiceOrderManager;
import org.flowable.dmn.api.DmnRepositoryService;
import org.flowable.dmn.api.ExecuteDecisionBuilder;
import org.flowable.dmn.engine.DmnEngine;
import org.flowable.dmn.engine.test.DmnDeployment;
import org.flowable.dmn.model.DmnDefinition;
import org.flowable.dmn.xml.converter.DmnXMLConverter;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.spring.impl.test.FlowableSpringExtension;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.etsi.osl.tmf.common.model.Any;
import org.etsi.osl.tmf.common.model.EValueType;
import org.etsi.osl.tmf.common.model.service.Characteristic;
import org.etsi.osl.tmf.lcm.model.LCMRuleSpecification;
import org.etsi.osl.tmf.scm633.model.ServiceSpecCharacteristic;
import org.etsi.osl.tmf.scm633.model.ServiceSpecCharacteristicValue;
import org.etsi.osl.tmf.scm633.model.ServiceSpecification;
import org.etsi.osl.tmf.sim638.model.Service;
import org.etsi.osl.tmf.sim638.model.ServiceCreate;
import org.etsi.osl.tmf.so641.model.ServiceOrder;
import org.etsi.osl.tmf.so641.model.ServiceOrderItem;

@ExtendWith(FlowableSpringExtension.class)
@SpringBootTest(properties = { "CATALOG_GET_SERVICEORDER_BY_ID = direct:get_mocked_order",
		"CATALOG_GET_SERVICESPEC_BY_ID = direct:get_mocked_spec", "CATALOG_ADD_SERVICE = direct:get_mocked_add_service",
		"CATALOG_UPD_SERVICEORDER_BY_ID = direct:get_mocked_upd_order",
		"CATALOG_ADD_SERVICEORDER = direct:get_mocked_upd_order",
		"CATALOG_GET_SERVICE_BY_ID = direct:get_mocked_service_id",
		"CATALOG_SERVICE_QUEUE_ITEMS_GET: direct:get_mocked_service_queueitems",
		"CATALOG_SERVICE_QUEUE_ITEM_UPD: direct:get_mocked_service_id",
		"CATALOG_SERVICE_QUEUE_ITEM_DELETE: direct:get_mocked_service_id",
		"CATALOG_UPD_SERVICE = direct:get_mocked_upd_service", 
		"NFV_CATALOG_DEPLOY_NSD_REQ = direct:req_deploy_nsd",
		"CATALOG_GET_LCMRULE_BY_ID = direct:get_mocked_lcmrulebyid",		
		"CATALOG_GET_LCMRULES_BY_SPECID_PHASE = direct:get_mocked_lcmrulesbyspecid",				
		"CATALOG_SERVICES_OF_PARTNERS = direct:get_mocked_service_queueitems",			
		"CATALOG_SERVICES_TO_TERMINATE = direct:get_mocked_service_queueitems",	
		"CATALOG_GET_SERVICEORDER_IDS_BY_STATE = direct:get_mocked_service_queueitems",	
		"CATALOG_GET_SERVICETESTSPEC_BY_ID = direct:get_mocked_service_queueitems",		
		"CATALOG_ADD_SERVICETEST = direct:get_mocked_service_queueitems",		
		"CATALOG_UPD_SERVICETEST = direct:get_mocked_service_queueitems",	
		"CATALOG_GET_SERVICETEST_BY_ID = direct:get_mocked_service_queueitems",	
		"ALARMS_ADD_ALARM=mock:output",
		"ALARMS_UPDATE_ALARM=mock:output",
		"ALARMS_GET_ALARM=mock:output",
		"NFV_CATALOG_GET_DEPLOYMENT_BY_ID = direct:req_deployment_id", 
		"NFV_CATALOG_UPD_DEPLOYMENT_BY_ID = direct:req_deployment_id", 
		"NFV_CATALOG_GET_NSD_BY_ID = direct:req_nsd_id", 
		"uri.to   = mock:output" })
@ActiveProfiles("testing")
public class ProcessOrderIntegrationTest {
	private static final transient Log logger = LogFactory.getLog(ProcessOrderIntegrationTest.class.getName());

	@Autowired
	RepositoryService repositoryService;

	@Autowired
	private RuntimeService runtimeService;

	@Autowired
	private TaskService taskService;

	@Autowired
	private CamelContext camelContext;
	
	RoutesBuilder builder = new RouteBuilder() {
		@Override
		public void configure() {
			from("direct:get_mocked_order").bean(scmocked, "getOrderById");
			from("direct:get_mocked_spec").bean(scmocked, "getSpecById");
			from("direct:get_mocked_add_service").bean(scmocked, "getMockedAddService");
			from("direct:get_mocked_upd_service").bean(scmocked, "getMockedService");
			from("direct:get_mocked_upd_order").bean(scmocked, "updateServiceOrder");
			from("direct:get_mocked_service_id").bean(scmocked, "getServiceById");
			from("direct:get_mocked_service_queueitems").bean(scmocked, "getServiceQueueItems");
			from("direct:get_mocked_lcmrulebyid").bean(scmocked, "getLCMRulebyID");
			from("direct:get_mocked_lcmrulesbyspecid").bean(scmocked, "getLCMRulesbySpecIDPhase(${header.servicespecid}, ${header.phasename})");
			from("direct:req_deploy_nsd").bean(scmocked, "req_deploy_nsd");
			from("direct:req_deployment_id").bean(scmocked, "req_deployment_id");
			from("direct:req_nsd_id").bean(scmocked, "req_nsd_id");

		};
	};


//    @MockBean(name = "orchestrationService" )
//    @Autowired
//    private OrchestrationServiceMocked orchestrationServiceMocked;

	@Autowired
	private ServiceOrderManager serviceOrderManager;

	SCMocked scmocked = new SCMocked();

	@Test
	// @Deployment(resources = { "processes/ServiceOrder.bpmn" })
	public void startProcess() throws Exception {
		logger.debug("===============TEST START startProcess =============================");
		// doCallRealMethod().when( orchestrationServiceMocked).execute( Mockito.any() )
		// ;

		/**
		 * configure here the mocked routes
		 */

		camelContext.addRoutes(builder);

		logger.info("waiting 1secs");
		Thread.sleep(1000); // wait
		 scmocked.getRunningServices().clear();
		ServiceSpecification spec = serviceOrderManager.retrieveServiceSpec("f2b74f90-4140-4895-80d1-ef243398117b");
		ServiceSpecification specCirros = serviceOrderManager.retrieveServiceSpec("99176116-17cf-464f-96f7-86e685914666");
		ServiceOrder sorder = serviceOrderManager.retrieveServiceOrder("a842a6fd-a9df-4d0e-9e17-922954a100c6");
		assertThat(sorder).isInstanceOf(ServiceOrder.class);
		assertThat(spec).isInstanceOf(ServiceSpecification.class);

		assertThat(spec.getServiceSpecCharacteristic().size()  ).isEqualTo(11);
		assertThat(specCirros.getServiceSpecCharacteristic().size()  ).isEqualTo(10);
		assertThat(sorder.getOrderItem().stream().findFirst().get().getService().getServiceCharacteristic().size()  ).isEqualTo(2);

		assertThat(repositoryService.createProcessDefinitionQuery().count()).isEqualTo(15);
		assertThat(taskService.createTaskQuery().count()).isEqualTo(0);

		assertThat( scmocked.getRequeestedDescriptor() ).isNull();
		
		repositoryService.suspendProcessDefinitionByKey("OrderSchedulerProcess"); // this is to stop the timer

		Map<String, Object> variables = new HashMap<>();
		variables.put("orderid", "a842a6fd-a9df-4d0e-9e17-922954a100c6");
		runtimeService.startProcessInstanceByKey("StartOrderProcess", variables);
		logger.info("waiting 10sec");
		Thread.sleep(7000); // wait

		for (ProcessInstance pi : runtimeService.createProcessInstanceQuery().list()) {
			logger.info(" pi.id " + pi.toString());
		}

		for (Task task : taskService.createTaskQuery().list()) {
			logger.info(" task.name " + task.getName());
		}
		
		if (scmocked.getRunningServices().size() <2) {
			Thread.sleep(5000); // wait a little more :-)			
		}
		
		
		//check here that the running services contain equal characteristics to the original
		assertThat( scmocked.getRunningServices().size()  ).isEqualTo(2);
		Service aservice = null;
		Service aserviceCirros = null;
		for (String suuid : scmocked.getRunningServices().keySet()) {
			if ( scmocked.getRunningServices().get( suuid ).getName().equals("Cirros Test") ){
				aservice = scmocked.getRunningServices().get( suuid );
			} else if ( scmocked.getRunningServices().get( suuid ).getName().equals("cirros_2vnf_ns") ){
				aserviceCirros = scmocked.getRunningServices().get( suuid );
			}				
		}
		assertThat( aservice  ).isNotNull();
		assertThat( aservice.getServiceCharacteristic().size()  ).isEqualTo(11);
		assertThat( aserviceCirros  ).isNotNull();
		assertThat( aserviceCirros.getServiceCharacteristic().size()  ).isEqualTo(10);

		assertThat(  aservice.getServiceCharacteristicByName("Quality Class").getValue().getValue() ).isEqualTo( "1" );
		assertThat(  aservice.getServiceCharacteristicByName("cirros_2vnf_ns::OSM_CONFIG").getValue().getValue() ).contains( "eeeeeeee-8219-4580-9697-bf4a8f0a08f9" );
		assertThat(  aservice.getServiceCharacteristicByName("cirros_2vnf_ns::SSHKEY").getValue().getValue() ).isEqualTo( "MCKEYTESTINORDERExampleConcatSSHKEY_EnhancedByRule" );
		//check that the cirros_2vnf_ns::SSHKEY value from the service order has been passed properly to the related RFS service
		assertThat(  aserviceCirros.getServiceCharacteristicByName("OSM_CONFIG").getValue().getValue() ).contains( "eeeeeeee-8219-4580-9697-bf4a8f0a08f9" );
		assertThat(  aserviceCirros.getServiceCharacteristicByName("SSHKEY").getValue().getValue() ).isEqualTo( "MCKEYTESTINORDERExampleConcatSSHKEY_EnhancedByRule" );
		
		
		//we will further check LCM rules!
		
		

//		assertThat( scmocked.getRequeestedDescriptor() ).isNotNull();
//		assertThat( scmocked.getRequeestedDescriptor().getId() ).isEqualTo( 123456789 );
//		assertThat( scmocked.getRequeestedDescriptor().getConfigStatus() ).contains("cirros_ue_uplink=192.0");
//		assertThat( scmocked.getRequeestedDescriptor().getConfigStatus() ).contains("cirros_slice_uplink=1024.0");

		

		assertThat(taskService.createTaskQuery().count()).isEqualTo(0);

		logger.info("waiting 3secs");
		Thread.sleep(3000); // wait

		logger.debug("===============TEST END startProcess =============================");
	}

	@Autowired
	private DmnEngine dmnEngine;

//	@Autowired
//	private DmnRuleService ruleService;

//	@Autowired
//	@Rule
//	public FlowableDmnRule flowableSpringRule;
//
//	@Test
//	@DmnDeployment(resources = "dmn/genericdecisions.dmn")
//	public void uniqueHitPolicy() {
//		logger.debug("===============TEST START uniqueHitPolicy =============================");
////		DmnEngine admnEngine = flowableSpringRule.getDmnEngine();
//		DmnRuleService dmnRuleService = dmnEngine.getDmnRuleService();
//
//		ExecuteDecisionBuilder ex = ruleService.createExecuteDecisionBuilder().decisionKey("decisionKJ");
//
//		Map<String, Object> result = ex.variable("Uplink_throughput_per_UE__Guaranteed_uplink_throughput", 8).executeWithSingleResult();
//
//		assertEquals(64.0, result.get("cirros_ue_uplink"));
//		assertEquals(512.0, result.get("cirros_slice_uplink"));
//		logger.debug("===============TEST END uniqueHitPolicy =============================");
//	}

	@Test
	public void programmaticallyCreate() throws XMLStreamException, FileNotFoundException {

		logger.debug("===============TEST START programmaticallyCreate =============================");
		try {

			File initialFile = new File("src/test/resources/ondemand_decisions.dmn");
			InputStream targetStream = new FileInputStream(initialFile);

			XMLInputFactory inputFactory = XMLInputFactory.newInstance();
			XMLStreamReader xtr = inputFactory.createXMLStreamReader(targetStream);
			DmnDefinition dmnDefinition = new DmnXMLConverter().convertToDmnModel(xtr);

			DmnRepositoryService dmnRepositoryService = dmnEngine.getDmnRepositoryService();
			org.flowable.dmn.api.DmnDeployment dmnDeployment = dmnRepositoryService.createDeployment()
					.name("decision_ONDEMAND").tenantId("abcd").addDmnModel("ondemand_decisions.dmn", dmnDefinition).deploy();
			
//			DmnDecisionTable dmnt = dmnRepositoryService.getDecisionTable( "decision_ONDEMAND" );			
//			assertNotNull(dmnt);
			
//			ExecuteDecisionBuilder ex = ruleService.createExecuteDecisionBuilder().decisionKey("decision_ONDEMAND").tenantId("abcd");
//
//			Map<String, Object> variables = new HashMap<>();
//			variables.put("cameras", 3);
//			variables.put("video_definition", 3);
//			Map<String, Object> result = ex.variables(variables).executeWithSingleResult();
//			assertEquals("1024", result.get("uplink"));
//			assertEquals( 2048.0, result.get("slice_uplink"));
//			
//			variables = new HashMap<>();
//			variables.put("cameras", 3);
//			variables.put("video_definition", 2);
//			result = ex.variables(variables).executeWithSingleResult();
//			assertEquals("256", result.get("uplink"));
//			assertEquals( 1024.0, result.get("slice_uplink"));
			
			
		} finally {

		}
		

		logger.debug("===============TEST END programmaticallyCreate =============================");
	}
	
	
	@Test
	public void testNFVOProcessOrder() throws Exception {

		logger.debug("===============TEST START testNFVOProcessOrder =============================");

//		repositoryService.suspendProcessDefinitionByKey("OrderSchedulerProcess"); // this is to stop the timer
		repositoryService.suspendProcessDefinitionByKey("fetchInRpogressOrdersProcess"); // this is to stop the timer
		
		
//		/**
//		 * configure here the mocked routes
//		 */
//		RoutesBuilder builder = new RouteBuilder() {
//			@Override
//			public void configure() {
//				from("direct:get_mocked_order").bean(scmocked, "getOrderById");
//				from("direct:get_mocked_spec").bean(scmocked, "getSpecById");
//				from("direct:get_mocked_add_service").bean(scmocked, "getMockedService");
//				from("direct:get_mocked_upd_service").bean(scmocked, "getMockedService");
//				from("direct:get_mocked_upd_order").bean(scmocked, "updateServiceOrder");
//				from("direct:get_mocked_service_id").bean(scmocked, "getServiceById");
//				from("direct:req_deploy_nsd").bean(scmocked, "req_deploy_nsd");
//				from("direct:req_deployment_id").bean(scmocked, "req_deployment_id");
//
//			};
//		};
//
//		camelContext.addRoutes(builder);

		logger.info("waiting 1secs");
		Thread.sleep(1000); // wait

		assertThat(serviceOrderManager.retrieveServiceOrder("a842a6fd-a9df-4d0e-9e17-922954a100c6"))
				.isInstanceOf(ServiceOrder.class);
		
		Map<String, Object> variables = new HashMap<>();
		variables.put("orderid", "a842a6fd-a9df-4d0e-9e17-922954a100c6");
		runtimeService.startProcessInstanceByKey("StartOrderProcess", variables);
		logger.info("waiting 1sec");
		Thread.sleep(1000); // wait

		for (ProcessInstance pi : runtimeService.createProcessInstanceQuery().list()) {
			logger.info(" pi.id " + pi.toString());
		}

		for (Task task : taskService.createTaskQuery().list()) {
			logger.info(" task.name " + task.getName());
		}

		
		logger.info("waiting 10secs");
		Thread.sleep(10000); // wait

		logger.debug("===============TEST END testNFVOProcessOrder =============================");
	}
	
	
	

	
}

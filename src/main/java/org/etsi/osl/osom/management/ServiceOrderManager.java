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
package org.etsi.osl.osom.management;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.camel.ProducerTemplate;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.etsi.osl.osom.serviceactions.NSActionRequestPayload;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.etsi.osl.model.DeploymentDescriptor;
import org.etsi.osl.model.NetworkServiceDescriptor;
import org.etsi.osl.model.ScaleDescriptor;
import org.etsi.osl.tmf.pm632.model.Organization;
import org.etsi.osl.tmf.rcm634.model.ResourceSpecification;
import org.etsi.osl.tmf.ri639.model.LogicalResource;
import org.etsi.osl.tmf.ri639.model.PhysicalResource;
import org.etsi.osl.tmf.ri639.model.Resource;
import org.etsi.osl.tmf.ri639.model.ResourceCreate;
import org.etsi.osl.tmf.scm633.model.ServiceSpecification;
import org.etsi.osl.tmf.sim638.model.ServiceActionQueueItem;
import org.etsi.osl.tmf.sim638.model.ServiceCreate;
import org.etsi.osl.tmf.sim638.model.ServiceUpdate;
import org.etsi.osl.tmf.so641.model.ServiceOrder;
import org.etsi.osl.tmf.so641.model.ServiceOrderCreate;
import org.etsi.osl.tmf.so641.model.ServiceOrderStateType;
import org.etsi.osl.tmf.so641.model.ServiceOrderUpdate;
import org.etsi.osl.tmf.stm653.model.ServiceTest;
import org.etsi.osl.tmf.stm653.model.ServiceTestCreate;
import org.etsi.osl.tmf.stm653.model.ServiceTestSpecification;
import org.etsi.osl.tmf.stm653.model.ServiceTestUpdate;
import jakarta.validation.constraints.NotNull;

/**
 * @author ctranoris
 *
 */
@Service
public class ServiceOrderManager {

	private static final transient Log logger = LogFactory.getLog(ServiceOrderManager.class.getName());

	private static final String ORDER_ASSIGNEE = "admin";

	@Autowired
	private RuntimeService runtimeService;

	@Autowired
	private TaskService taskService;

    @Autowired
    private ProducerTemplate template;
    


	@Value("${CATALOG_GET_SERVICEORDER_BY_ID}")
	private String CATALOG_GET_SERVICEORDER_BY_ID = "";
	

	@Value("${CATALOG_GET_SERVICESPEC_BY_ID}")
	private String CATALOG_GET_SERVICESPEC_BY_ID = "";
	
//	@Value("${CATALOG_GET_INITIAL_SERVICEORDERS_IDS}")
//	private String CATALOG_GET_INITIAL_SERVICEORDERS_IDS = "";

	@Value("${CATALOG_GET_SERVICEORDER_IDS_BY_STATE}")
	private String CATALOG_GET_SERVICEORDER_IDS_BY_STATE = "";
	
	@Value("${CATALOG_UPD_SERVICEORDER_BY_ID}")
	private String CATALOG_UPD_SERVICEORDER_BY_ID = "";
	

	@Value("${CATALOG_ADD_SERVICEORDER}")
	private String CATALOG_ADD_SERVICEORDER = "";
	
	
	@Value("${CATALOG_ADD_SERVICE}")
	private String CATALOG_ADD_SERVICE = "";

	@Value("${CATALOG_UPD_SERVICE}")
	private String CATALOG_UPD_SERVICE = "";

	@Value("${CATALOG_GET_SERVICE_BY_ID}")
	private String CATALOG_GET_SERVICE_BY_ID = "";

	@Value("${CATALOG_GET_SERVICE_BY_ORDERID}")
	private String CATALOG_GET_SERVICE_BY_ORDERID = "";
	
	
	
	@Value("${CATALOG_SERVICE_QUEUE_ITEMS_GET}")
	private String CATALOG_SERVICE_QUEUE_ITEMS_GET = "";	

	@Value("${CATALOG_SERVICE_QUEUE_ITEM_UPD}")
	private String CATALOG_SERVICE_QUEUE_ITEM_UPD = "";
	
	@Value("${CATALOG_SERVICE_QUEUE_ITEM_DELETE}")
	private String CATALOG_SERVICE_QUEUE_ITEM_DELETE = "";
	

	@Value("${CATALOG_SERVICES_TO_TERMINATE}")
	private String CATALOG_SERVICES_TO_TERMINATE = "";

	@Value("${CATALOG_SERVICES_OF_PARTNERS}")
	private String CATALOG_SERVICES_OF_PARTNERS = "";
	
	@Value("${NFV_CATALOG_DEPLOY_NSD_REQ}")
	private String NFV_CATALOG_DEPLOY_NSD_REQ = "";
	

	@Value("${NFV_CATALOG_GET_DEPLOYMENT_BY_ID}")
	private String NFV_CATALOG_GET_DEPLOYMENT_BY_ID = "";
	

	@Value("${NFV_CATALOG_GET_NSD_BY_ID}")
	private String NFV_CATALOG_GET_NSD_BY_ID = "";
	

	@Value("${NFV_CATALOG_UPD_DEPLOYMENT_BY_ID}")
	private String NFV_CATALOG_UPD_DEPLOYMENT_BY_ID = "";

	@Value("${CATALOG_GET_PARTNER_ORGANIZATON_BY_ID}")
	private String CATALOG_GET_PARTNER_ORGANIZATON_BY_ID = "";

	@Value("${NFV_CATALOG_NS_DAY2_ACTION}")
	private String NFV_CATALOG_NS_DAY2_ACTION = "";

	@Value("${NFV_CATALOG_NSACTIONS_SCALE}")
	private String NFV_CATALOG_NSACTIONS_SCALE = "";


	@Value("${CATALOG_GET_SERVICETESTSPEC_BY_ID}")
	private String CATALOG_GET_SERVICETESTSPEC_BY_ID = "";
	
	@Value("${CATALOG_ADD_SERVICETEST}")
	private String CATALOG_ADD_SERVICETEST = "";
	
	@Value("${CATALOG_UPD_SERVICETEST}")
	private String CATALOG_UPD_SERVICETEST = "";
	
	@Value("${CATALOG_GET_SERVICETEST_BY_ID}")
	private String CATALOG_GET_SERVICETEST_BY_ID = "";	
	
	

    @Value("${CRD_DEPLOY_CR_REQ}")
    private String CRD_DEPLOY_CR_REQ = "";
    
    @Value("${CRD_PATCH_CR_REQ}")
    private String CRD_PATCH_CR_REQ = "";
    
    @Value("${CRD_DELETE_CR_REQ}")
    private String CRD_DELETE_CR_REQ = "";

    @Value("${CATALOG_ADD_RESOURCE}")
    private String CATALOG_ADD_RESOURCE = "";

    @Value("${CATALOG_GET_RESOURCE_BY_ID}")
    private String CATALOG_GET_RESOURCE_BY_ID = "";
    
    
	
	@Transactional
	public void processOrder(ServiceOrder serviceOrder) {

		logger.info("Received order to process serviceOrder id : " + serviceOrder.getId());
		Map<String, Object> variables = new HashMap<>();
		variables.put("orderid", serviceOrder.getId());

		runtimeService.startProcessInstanceByKey("InitialServiceOrderAckProcess", variables);
	}

	@Transactional
	public List<String> getTasks(String assignee) {
		/**
		 * we ignore for now the assignee
		 */
		String assign = ORDER_ASSIGNEE;
		logger.info("Received order to getTasks, assignee : " + assign);
		List<Task> tasks = taskService.createTaskQuery().taskCandidateGroup(assign).list();

		List<String> orders = tasks.stream().map(task -> {
			Map<String, Object> variables = taskService.getVariables(task.getId());
			return (String) variables.get("orderid");
		}).collect(Collectors.toList());

		logger.info("orderid(s) : " + orders.toString());
		return orders;
	}



	@Transactional
	public void humanComplete(String id) {
		logger.info("Received Order manual complete for orderid=" +id );
		/**
		 * we ignore for now the assignee
		 */
		String assignee = ORDER_ASSIGNEE;

		List<Task> tasks = taskService.createTaskQuery()
				.taskDefinitionKey("usertaskManualCompleteOrder")
				// .taskCandidateGroup( assignee )
				.list();
		String taskId = null;

		for (Task t : tasks) {
			logger.info("PENDING humanComplete t.id=" + t.getId() + "" + "orderid=" + taskService.getVariables(t.getId()).get("orderid") );
			String orderid=  (String) taskService.getVariables(t.getId()).get("orderid");
			if ( orderid.contains( id )) {
				taskId = t.getId();
				if (taskId != null) {

					logger.info("will complete orderid=" +id );
					taskService.complete(taskId);
				} else {

					logger.error("Task ID cannot be found for received OrderApproval for orderid=" + id);
				}
			}
			

		}

	}

//	/**
//	 * Request orders to be processed
//	 * @return a string list of Order IDs
//	 */
//	public List<String> retrieveOrdersToBeProcessed() {
//		logger.info("will retrieve Service Orders to be processed from catalog "   );
//		try {
//			Map<String, Object> map = new HashMap<>();
//			map.put("orderstate", ServiceOrderStateType.ACKNOWLEDGED.toString() );
//			
//			Object response = template.
//					requestBodyAndHeaders( CATALOG_GET_SERVICEORDER_IDS_BY_STATE, "", map );
//
//			logger.info("will retrieve Service OrdersACKNOWLEDGED from catalog response: " + response.getClass()  );
//			if ( !(response instanceof String)) {
//				logger.error("List  object is wrong.");
//				return null;
//			}
//			//String[] sor = toJsonObj( (String)response, String[].class );
//
//			ArrayList<String> sor = toJsonObj( (String)response, ArrayList.class ); 
//			logger.debug("retrieveOrdersToBeProcessed response is: " + response);
//			
////			return asList(sor);
//			return sor;
//			
//		}catch (Exception e) {
//			logger.error("Cannot retrieve Listof Service Orders ACKNOWLEDGED from catalog. " + e.toString());
//		}
//		return null;
//	}
	
	
	/**
	 * @return
	 */
	public List<String> retrieveOrdersByState(ServiceOrderStateType orderState) {
		logger.info("will retrieve Service Orders " + orderState.toString() + " from catalog "   );
		try {
			Map<String, Object> map = new HashMap<>();
			map.put("orderstate", orderState.toString() );
			Object response = template.
					requestBodyAndHeaders( CATALOG_GET_SERVICEORDER_IDS_BY_STATE, "", map );

			logger.debug("will retrieve Service Orders " + orderState.toString() + " from catalog response: " + response.getClass()  );
			if ( !(response instanceof String)) {
				logger.error("List  object is wrong.");
				return null;
			}
			//String[] sor = toJsonObj( (String)response, String[].class );

			ArrayList<String> sor = toJsonObj( (String)response, ArrayList.class ); 
			logger.debug("retrieveOrdersByState response is: " + response);
			
//			return asList(sor);
			return sor;
			
		}catch (Exception e) {
			logger.error("Cannot retrieve Listof Service Orders "+ orderState.toString() + " from catalog. " + e.toString());
		}
		return null;
	}
	/**
	 * get  service order by id from model via bus
	 * @param id
	 * @return
	 * @throws IOException
	 */
	public ServiceOrder retrieveServiceOrder( String orderid) {
		logger.info("will retrieve Service Order from catalog orderid=" + orderid   );
		try {
			Object response = template.
					requestBody( CATALOG_GET_SERVICEORDER_BY_ID, orderid);
			
			if ( !(response instanceof String)) {
				logger.error("Service Order object is wrong.");
				return null;
			}
			logger.debug("retrieveServiceOrder response is: " + response);
			ServiceOrder sor = toJsonObj( (String)response, ServiceOrder.class); 
			
			return sor;
			
		}catch (Exception e) {
			logger.error("Cannot retrieve Service Order details from catalog. " + e.toString());
		}
		return null;
	}


	
	public void updateServiceOrderOrder(String orderid, ServiceOrderUpdate serviceOrder) {
		logger.info("will set Service Order in progress orderid=" + orderid   );
		try {

			template.sendBodyAndHeader( CATALOG_UPD_SERVICEORDER_BY_ID, toJsonString(serviceOrder), "orderid", orderid);

			
		}catch (Exception e) {
			logger.error("Cannot set Service Order status from catalog. " + e.toString());
		}
		
	}
	
	public ServiceOrder createServiceOrder(ServiceOrderCreate serviceOrderCreate) {
		logger.info("will create Service Order "   );
		try {

			
			try {
				Object response = template.
						requestBody( CATALOG_ADD_SERVICEORDER, toJsonString(serviceOrderCreate));
				
				if ( !(response instanceof String)) {
					logger.error("Service Order object is wrong.");
					return null;
				}
				logger.debug("createServiceOrder response is: " + response);
				ServiceOrder sor = toJsonObj( (String)response, ServiceOrder.class); 
				
				return sor;
				
			}catch (Exception e) {
				logger.error("Cannot createServiceOrder details to catalog. " + e.toString());
			}
			return null;
			
			

			
		}catch (Exception e) {
			logger.error("Cannot createServiceOrder to catalog. " + e.toString());
		}
		return null;
		
	}
	

	/**
	 * get  service spec by id from model via bus
	 * @param id
	 * @return
	 * @throws IOException
	 */
	public ServiceSpecification retrieveServiceSpec(String specid) {
		logger.info("will retrieve Service Specification from catalog orderid=" + specid   );
		
		try {
			Object response = template.
					requestBody( CATALOG_GET_SERVICESPEC_BY_ID, specid);

			if ( !(response instanceof String)) {
				logger.error("Service Specification object is wrong.");
				return null;
			}
			ServiceSpecification sor = toJsonObj( (String)response, ServiceSpecification.class); 
			//logger.debug("retrieveSpec response is: " + response);
			return sor;
			
		}catch (Exception e) {
			logger.error("Cannot retrieve Service Specification details from catalog. " + e.toString());
		}
		return null;
	}
	
	

	public org.etsi.osl.tmf.sim638.model.Service createService( ServiceCreate s, ServiceOrder sor, ServiceSpecification spec) {
		logger.info("will create Service for spec: " + spec.getId() );
		try {
			Map<String, Object> map = new HashMap<>();
			map.put("orderid", sor.getId() );
			map.put("serviceSpecid", spec.getId() );
			Object response = template.requestBodyAndHeaders( CATALOG_ADD_SERVICE, toJsonString(s), map);

			if ( !(response instanceof String)) {
				logger.error("Service Instance object is wrong.");
			}

			org.etsi.osl.tmf.sim638.model.Service serviceInstance = toJsonObj( (String)response, org.etsi.osl.tmf.sim638.model.Service.class); 
			//logger.debug("createService response is: " + response);
			return serviceInstance;
			
			
		}catch (Exception e) {
			logger.error("Cannot create Service for spec " + spec.getId()+ ": " + e.toString());
		}
		return null;
		
	}
	
	/**
	 * @param serviceId
	 * @param s
	 * @param propagateToSO is a cryptic thing. However it is used as follows: if FALSE, to just update the service status in catalog without further taking any action.
	 * if TRUE then the ServiceUpdate will trigger a ServiceActionQueue to further process the update. So this is needed to avoid these kinds of deadlocks
	 * @return
	 */
	public org.etsi.osl.tmf.sim638.model.Service updateService(String serviceId, ServiceUpdate s, boolean propagateToSO) {
		logger.info("will update Service : " + serviceId );
		try {
			Map<String, Object> map = new HashMap<>();
			map.put("serviceid", serviceId );
			map.put("propagateToSO", propagateToSO );
			
			Object response = template.requestBodyAndHeaders( CATALOG_UPD_SERVICE, toJsonString(s), map);

			if ( !(response instanceof String)) {
				logger.error("Service Instance object is wrong.");
			}

			org.etsi.osl.tmf.sim638.model.Service serviceInstance = toJsonObj( (String)response, org.etsi.osl.tmf.sim638.model.Service.class); 
			//logger.debug("createService response is: " + response);
			return serviceInstance;
			
			
		}catch (Exception e) {
			logger.error("Cannot update Service: " + serviceId + ": " + e.toString());
		}
		return null;
		
	}
	

	
	
	/**
	 * Ger service instance via bus
	 * @param serviceID
	 * @return
	 */
	public org.etsi.osl.tmf.sim638.model.Service retrieveService(String serviceID) {
		logger.info("will retrieve Service instance from catalog serviceID=" + serviceID   );
		try {
			Object response = template.
					requestBody( CATALOG_GET_SERVICE_BY_ID, serviceID);

			if ( !(response instanceof String)) {
				logger.error("Service object is wrong.");
				return null;
			}
			org.etsi.osl.tmf.sim638.model.Service serviceInstance = toJsonObj( (String)response, org.etsi.osl.tmf.sim638.model.Service.class); 
			//logger.debug("retrieveService response is: " + response);
			return serviceInstance;
			
		}catch (Exception e) {
			logger.error("Cannot retrieve Service details from catalog. " + e.toString());
		}
		return null;
	}

	
	
	
	
	static <T> T toJsonObj(String content, Class<T> valueType)  throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        return mapper.readValue( content, valueType);
    }

	 static String toJsonString(Object object) throws IOException {
	        ObjectMapper mapper = new ObjectMapper();
	        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
	        return mapper.writeValueAsString(object);
	    }

	 
	public DeploymentDescriptor nfvoDeploymentRequestByNSDid( DeploymentDescriptor ddreq ) {
		
		if (ddreq == null) {
			logger.fatal("nfvoDeploymentRequestByNSDid ddreq is NULL!"  );
			return null;
		}
		
		if (ddreq.getExperiment() == null) {
			logger.fatal("nfvoDeploymentRequestByNSDid ddreq.getExperiment() is NULL!"  );
			return null;
		}
		
		logger.info("Will request by NFV Catalog to deploy NSD id= " + ddreq.getExperiment().getId()   );
		
		try {

			String body = toJsonString(ddreq);
			Object response = template.requestBodyAndHeader( NFV_CATALOG_DEPLOY_NSD_REQ, body , "id", ddreq.getExperiment().getId());

			if ( !(response instanceof String)) {
				logger.error("DeploymentDescriptor object is wrong.");
				return null;
			}
			DeploymentDescriptor dd = toJsonObj( (String)response, DeploymentDescriptor.class); 
			logger.debug("nfvoDeploymentRequestByNSDid response is: " + response);
			return dd;
			
		}catch (Exception e) {
			logger.error("Cannot retrieve DeploymentDescriptor details from NFV catalog. " + e.toString());
			e.printStackTrace();
		}
		return null;
	}

	public DeploymentDescriptor retrieveNFVODeploymentRequestById(long deploymentId) {

		logger.info("Will request by NFV Catalog detaile of DeploymentRequest= " + deploymentId );
		
		try {

			Object response = template.requestBody( NFV_CATALOG_GET_DEPLOYMENT_BY_ID, deploymentId );

			if ( !(response instanceof String)) {
				logger.error("DeploymentDescriptor object is wrong.");
				return null;
			}
			DeploymentDescriptor dd = toJsonObj( (String)response, DeploymentDescriptor.class); 
			logger.debug("retriveNFVODeploymentRequestById response is: " + response);
			return dd;
			
		}catch (Exception e) {
			logger.error("Cannot retrieve DeploymentDescriptor details from NFV catalog. " + e.toString());
			e.printStackTrace();
		}
		return null;
	}
	
	public DeploymentDescriptor nfvoDeploymentRequestUpdate( DeploymentDescriptor ddreq ) {
		
		
		logger.info("Will update nfvoDeploymentRequestUpdate = " + ddreq.getId()   );
		
		try {

			String body = toJsonString(ddreq);
			Object response = template.requestBodyAndHeader( NFV_CATALOG_UPD_DEPLOYMENT_BY_ID, body , "id", ddreq.getId());

			if ( !(response instanceof String)) {
				logger.error("DeploymentDescriptor object is wrong.");
				return null;
			}
			DeploymentDescriptor dd = toJsonObj( (String)response, DeploymentDescriptor.class); 
			logger.debug("nfvoDeploymentRequestUpdate response is: " + response);
			return dd;
			
		}catch (Exception e) {
			logger.error("Cannot retrieve DeploymentDescriptor details from NFV catalog. " + e.toString());
			e.printStackTrace();
		}
		return null;
	}
	
	
	/**
	 * 
	 * execute Day2 action 
	 * @param nsp is a JSON string from class NSActionRequestPayload 
	 * @return
	 */
	public String nfvoDay2Action(NSActionRequestPayload nsp) {
		 
		logger.info("Will act NFV_CATALOG_NS_DAY2_ACTION = " + nsp   );
		
		try {

			String body = toJsonString(nsp);
			Object response = template.requestBody( NFV_CATALOG_NS_DAY2_ACTION, body);

			if ( !(response instanceof String)) {
				logger.error("nfvoDay2Action result  is wrong.");
				return null;
			}

			logger.debug("nfvoDay2Action response is: " + response);
			return (String) response;
			
		}catch (Exception e) {
			logger.error("Cannot perform nfvoDay2Action. " + e.toString());
			e.printStackTrace();
		}
		return null;
	 }
	
	/**
	 * get  service order by id from model via bus
	 * @param id
	 * @return
	 * @throws IOException
	 */
	public NetworkServiceDescriptor retrieveNSD( String nsdID) {
		logger.info("will retrieve NetworkServiceDescriptor from NSD/VNF catalog nsdID=" + nsdID   );
		try {
			Object response = template.
					requestBody( NFV_CATALOG_GET_NSD_BY_ID, nsdID);

			if ( !(response instanceof String)) {
				logger.error("NetworkServiceDescriptor object is wrong.");
				return null;
			}
			NetworkServiceDescriptor sor = toJsonObj( (String)response, NetworkServiceDescriptor.class); 
			//logger.debug("retrieveServiceOrder response is: " + response);
			return sor;
			
		}catch (Exception e) {
			logger.error("Cannot retrieve NetworkServiceDescriptor details from catalog. " + e.toString());
		}
		return null;
	}

	public Organization getExternalPartnerOrganization(String partnerId) {
		
		logger.info("will retrieve External Partner from catalog partnerId=" + partnerId   );
		
		try {
			Object response = template.
					requestBody( CATALOG_GET_PARTNER_ORGANIZATON_BY_ID, partnerId);

			if ( !(response instanceof String)) {
				logger.error("External Partner  object is wrong.");
				return null;
			}
			Organization orgz = toJsonObj( (String)response, Organization.class); 
			//logger.debug("retrieveSpec response is: " + response);
			return orgz;
			
		}catch (Exception e) {
			logger.error("Cannot retrieve External Partner details from catalog. " + e.toString());
		}
		return null;
	}

	
	//CATALOG_SERVICE_QUEUE_ITEMS_GET
	public List<ServiceActionQueueItem> retrieveServiceQueueItems() {
		logger.debug("will retrieve Service QueueItems from repository"   );
		try {
			
			Object response = template.
					requestBody( CATALOG_SERVICE_QUEUE_ITEMS_GET, "" );

			if ( !(response instanceof String)) {
				logger.error("List  object is wrong.");
				return null;
			}
			//String[] sor = toJsonObj( (String)response, String[].class );

			ServiceActionQueueItem[] sor = toJsonObj( (String)response, ServiceActionQueueItem[].class ); 
			logger.debug("retrieveServiceQueueItems response is: " + response);
			
//			return asList(sor);
			return Arrays.asList(sor);
			
		}catch (Exception e) {
			logger.error("Cannot retrieve Listof Service QueueItems . " + e.toString());
		}
		return null;
	}
	
	public void deleteServiceActionQueueItem(ServiceActionQueueItem item) {
	
		//
		logger.info("will delete Service QueueItems from repository itemid : " + item.getUuid() );
		try {

			String body = toJsonString(item);
			
			Map<String, Object> map = new HashMap<>();
			map.put("itemid", item.getUuid() );
			Object response = template.requestBodyAndHeaders( CATALOG_SERVICE_QUEUE_ITEM_DELETE, body, map);

			
			
		}catch (Exception e) {
			logger.error("Cannot update itemid: " + item.getUuid() + ": " + e.toString());
		}
		
	}

	public List<String> retrieveActiveServiceToTerminate() {
		logger.info("will retrieve ActiveServiceToTerminate"   );
		try {
			
			Object response = template.
					requestBody( CATALOG_SERVICES_TO_TERMINATE, "" );

			logger.debug("will retrieve ActiveServiceToTerminate response: " + response.getClass()  );
			if ( !(response instanceof String)) {
				logger.error("List  object is wrong.");
				return null;
			}
			//String[] sor = toJsonObj( (String)response, String[].class );

			String[] sor = toJsonObj( (String)response, String[].class ); 
			logger.debug("retrieveActiveServiceToTerminate response is: " + response);
			
//			return asList(sor);
			return Arrays.asList(sor);
			
		}catch (Exception e) {
			logger.error("Cannot retrieve Listof ActiveServiceToTerminate . " + e.toString());
		}
		return null;
	}

	public String nfvoScaleDescriptorAction(ScaleDescriptor aScaleDescriptor) {

		
		try {

			String body = toJsonString(aScaleDescriptor);
			Object response = template.requestBodyAndHeader( NFV_CATALOG_NSACTIONS_SCALE, body , "id", aScaleDescriptor.getDeploymentRequestID() );

			if ( !(response instanceof String)) {
				logger.error(" nfvoScaleDescriptorAction response object is wrong.");
				return null;
			}
			//String dd = toJsonObj( (String)response, DeploymentDescriptor.class); 
			logger.debug("nfvoScaleDescriptorAction response is: " + response);
			return (String) response;
			
		}catch (Exception e) {
			logger.error("Cannot retrieve nfvoScaleDescriptorAction details from NFV catalog. " + e.toString());
			e.printStackTrace();
		}
		return null;
	}

	public List<String> retrieveActiveServiceOfExternalPartners() {
			logger.info("will retrieve ActiveServiceOfExternalPartners"   );
			try {
				
				Object response = template.
						requestBody( CATALOG_SERVICES_OF_PARTNERS, "" );

				logger.debug("will retrieve ActiveServiceOfExternalPartners response: " + response.getClass()  );
				if ( !(response instanceof String)) {
					logger.error("List  object is wrong.");
					return null;
				}

				String[] sor = toJsonObj( (String)response, String[].class ); 
				logger.debug("ActiveServiceOfExternalPartners response is: " + response);
				
//				return asList(sor);
				return Arrays.asList(sor);
				
			}catch (Exception e) {
				logger.error("Cannot retrieve Listof ActiveServiceOfExternalPartners . " + e.toString());
			}
			return null;
		}

	
	/**
	 * Ger service instance IDs via bus CATALOG_GET_SERVICE_BY_ORDERID
	 * @param serviceIDorderID
	 * @return List<String>
	 */
	public List<String>  retrieveServicesOfOrder(String orderID) {
		logger.info("will retrieve ActiveServiceOfExternalPartners"   );
		try {
			
			Object response = template.
					requestBody( CATALOG_GET_SERVICE_BY_ORDERID, orderID );

			logger.debug("will retrieve ServicesOfOrder response: " + response.getClass()  );
			if ( !(response instanceof String)) {
				logger.error("List  object is wrong.");
				return null;
			}

			String[] sor = toJsonObj( (String)response, String[].class ); 
			logger.debug("ServicesOfOrder response is: " + response);
			
			return Arrays.asList(sor);
			
		}catch (Exception e) {
			logger.error("Cannot retrieve Listof ServicesOfOrder . " + e.toString());
		}
		return null;
	}
	
	
	/**
	 * get  service Test spec by id from model via bus
	 * @param id
	 * @return
	 * @throws IOException
	 */
	public ServiceTestSpecification retrieveServiceTestSpec(String specid) {
		logger.info("will retrieve Service Test Specification from catalog orderid=" + specid   );
		
		try {
			Object response = template.
					requestBody( CATALOG_GET_SERVICETESTSPEC_BY_ID, specid);

			if ( !(response instanceof String)) {
				logger.error("Service Test Specification object is wrong.");
				return null;
			}
			ServiceTestSpecification sor = toJsonObj( (String)response, ServiceTestSpecification.class); 
			//logger.debug("retrieveSpec response is: " + response);
			return sor;
			
		}catch (Exception e) {
			logger.error("Cannot retrieve Service Test Specification details from catalog. " + e.toString());
		}
		return null;
	}

	public ServiceTest createServiceTest( ServiceTestCreate s, ServiceOrder sor, ServiceTestSpecification spec) {
		logger.info("will create Service Test for spec: " + spec.getId() );
		try {
			Map<String, Object> map = new HashMap<>();
			map.put("orderid", sor.getId() );
			map.put("serviceTestSpecid", spec.getId() );
			Object response = template.requestBodyAndHeaders( CATALOG_ADD_SERVICETEST, toJsonString(s), map);

			if ( !(response instanceof String)) {
				logger.error("Service Instance object is wrong.");
			}

			ServiceTest serviceInstance = toJsonObj( (String)response, ServiceTest.class); 
			//logger.debug("createService response is: " + response);
			return serviceInstance;
			
			
		}catch (Exception e) {
			logger.error("Cannot create Service for spec " + spec.getId()+ ": " + e.toString());
		}
		return null;
		
	}
	
	/**
	 * @param serviceId
	 * @param s
	 * @return
	 */
	public ServiceTest updateServiceTest(String serviceId, ServiceTestUpdate s) {
		logger.info("will update Service : " + serviceId );
		try {
			Map<String, Object> map = new HashMap<>();
			map.put("serviceid", serviceId );
			
			Object response = template.requestBodyAndHeaders( CATALOG_UPD_SERVICETEST, toJsonString(s), map);

			if ( !(response instanceof String)) {
				logger.error("ServiceTest Instance object is wrong.");
			}

			ServiceTest serviceInstance = toJsonObj( (String)response, ServiceTest.class); 

			return serviceInstance;
			
			
		}catch (Exception e) {
			logger.error("Cannot update ServiceTest: " + serviceId + ": " + e.toString());
		}
		return null;
		
	}
	

	/**
	 * Ger ServiceTest instance via bus
	 * @param serviceID
	 * @return
	 */
	public ServiceTest retrieveServiceTest(String serviceID) {
		logger.info("will retrieve ServiceTest instance from catalog serviceID=" + serviceID   );
		try {
			Object response = template.
					requestBody( CATALOG_GET_SERVICETEST_BY_ID, serviceID);

			if ( !(response instanceof String)) {
				logger.error("Service object is wrong.");
				return null;
			}
			ServiceTest serviceInstance = toJsonObj( (String)response, ServiceTest.class); 
			//logger.debug("retrieveService response is: " + response);
			return serviceInstance;
			
		}catch (Exception e) {
			logger.error("Cannot retrieve ServiceTest details from catalog. " + e.toString());
		}
		return null;
	}
	

  public Resource createResource(ResourceCreate s, ServiceOrder sor, String resourceSpecid) {

    
    logger.info("will create Resource for spec: " + resourceSpecid );
    try {
        Map<String, Object> map = new HashMap<>();
        map.put("orderid", sor.getId() );
        map.put("resourceSpecid", resourceSpecid );
        Object response = template.requestBodyAndHeaders( CATALOG_ADD_RESOURCE, toJsonString(s), map);

        if ( !(response instanceof String)) {
            logger.error("Resource Instance object is wrong.");
        }

        //logger.debug("createResource response is: " + response);
        try {
          LogicalResource resourceInstance = toJsonObj( (String)response, LogicalResource.class); 
          return resourceInstance;          
        }catch (Exception e) {
          PhysicalResource resourceInstance = toJsonObj( (String)response, PhysicalResource.class); 
          return resourceInstance;      
        }
        
        
    }catch (Exception e) {
        logger.error("Cannot create Resource for spec " + resourceSpecid + ": " + e.toString());
    }
    return null;
  }

  public Resource retrieveResource(@NotNull String resourceID) {
    logger.info("will retrieve Resource instance from catalog resourceID=" + resourceID   );
    try {
        Object response = template.
                requestBody( CATALOG_GET_RESOURCE_BY_ID, resourceID);

        if ( !(response instanceof String)) {
            logger.error("resource object is wrong.");
            return null;
        }
        LogicalResource rInstance = toJsonObj( (String)response, LogicalResource.class);
        
        return rInstance;
        
    }catch (Exception e) {
        logger.error("Cannot retrieve LogicalResource details from catalog. " + e.toString());
    }
    return null;
    
  }
  
  /**
   * @param rFS_CRSPEC 
   * @param serviceId 
   * 
   */
  public String cridgeDeploymentRequest(Map<String, Object> map, String CR_SPEC) {
      

      try {
        
        

        //Object response = template.requestBodyAndHeaders( CRD_DEPLOY_CR_REQ, CR_SPEC , map );
        //see OSOMRouteBuilder.class
        Object response = template.requestBodyAndHeaders( "direct:retriesCRD_DEPLOY_CR_REQ", CR_SPEC , map );
          

          if ( !(response instanceof String)) {
              logger.error("cridgeDeploymentRequest response object is wrong.");
              return null;
          }
          logger.debug("cridgeDeploymentRequest response is: " + response);
          return (String) response;
          
      }catch (Exception e) {
          logger.error("Cannot retrieve cridgeDeploymentRequestresponse. " + e.toString());
          e.printStackTrace();
      }
      return null;
      
  }

  

  /**
   * @param map
   * @param crspec
   */
  public String cridgeDeletionRequest(Map<String, Object> map, String CR_SPEC) {

    try {
        
        Object response = template.requestBodyAndHeaders( CRD_DELETE_CR_REQ, CR_SPEC , map );

        if ( !(response instanceof String)) {
            logger.error("cridgeDeletionRequest response object is wrong.");
            return null;
        }
        logger.debug("cridgeDeletionRequest response is: " + response);
        return (String) response;
        
    }catch (Exception e) {
        logger.error("Cannot retrieve cridgeDeletionRequest. " + e.toString());
        e.printStackTrace();
    }
    return null;
    
    
  }



}

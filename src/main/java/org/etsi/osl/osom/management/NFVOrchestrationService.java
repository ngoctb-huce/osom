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

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.Map;
import java.util.regex.Pattern;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.etsi.osl.model.nfv.DeploymentDescriptor;
import org.etsi.osl.model.nfv.DeploymentDescriptorStatus;
import org.etsi.osl.model.nfv.ExperimentMetadata;
import org.etsi.osl.model.nfv.ExperimentOnBoardDescriptor;
import org.etsi.osl.model.nfv.NetworkServiceDescriptor;
import org.etsi.osl.tmf.common.model.Any;
import org.etsi.osl.tmf.common.model.service.Characteristic;
import org.etsi.osl.tmf.common.model.service.Note;
import org.etsi.osl.tmf.common.model.service.ServiceStateType;
import org.etsi.osl.tmf.scm633.model.ServiceSpecCharacteristic;
import org.etsi.osl.tmf.scm633.model.ServiceSpecification;
import org.etsi.osl.tmf.sim638.model.Service;
import org.etsi.osl.tmf.sim638.model.ServiceUpdate;
import org.etsi.osl.tmf.so641.model.ServiceOrder;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


@Component(value = "nfvOrchestrationService") //bean name
public class NFVOrchestrationService implements JavaDelegate {

	private static final transient Log logger = LogFactory.getLog(NFVOrchestrationService.class.getName());


	@Value("${spring.application.name}")
	private String compname;
	
	
	@Autowired
	private ServiceOrderManager serviceOrderManager;

	@Autowired
	private DependencyRulesSolver aDependencyRulesSolver;
	
	public void execute(DelegateExecution execution) {

		logger.info( "NFVOrchestrationService" );
		logger.info( "VariableNames:" + execution.getVariableNames().toString() );
		logger.info("orderid:" + execution.getVariable("orderid").toString() );
		logger.info("contextServiceId:" + execution.getVariable("contextServiceId").toString() );

		ServiceUpdate su = new ServiceUpdate();//the object to update the service
		Note noteItem = new Note();
		noteItem.setText("");
		
		if (execution.getVariable("contextServiceId") instanceof String) {
			
			
			ServiceOrder sorder = serviceOrderManager.retrieveServiceOrder( execution.getVariable("orderid").toString() );
			Service aService = serviceOrderManager.retrieveService( (String) execution.getVariable("contextServiceId") );
			logger.info("Service name:" + aService.getName() );
			logger.info("Service state:" + aService.getState()  );			
			logger.info("Request to NFVO for Service: " + aService.getId() );
			
			//we need to retrieve here the Service Spec of this service that we send to the NFVO
			
			ServiceSpecification spec = serviceOrderManager.retrieveServiceSpec( aService.getServiceSpecificationRef().getId() );
			
			if ( spec!=null ) {			

				ServiceSpecCharacteristic c = spec.getServiceSpecCharacteristicByName( "NSDID" );						

				String NSDID = c.getDefaultValue();
				
				
				
				if ( NSDID != null) {
					/**
					 * it is registered in our NFV catalog. Let's request an instantiation of it
					 */
					

					ServiceSpecCharacteristic cOSM_NSDCATALOGID = spec.getServiceSpecCharacteristicByName( "OSM_NSDCATALOGID" );		
					ServiceSpecCharacteristic cOnBoardDescriptorID = spec.getServiceSpecCharacteristicByName( "OnBoardDescriptorID" );		
					ServiceSpecCharacteristic cOnBoardDescriptorUUID = spec.getServiceSpecCharacteristicByName( "OnBoardDescriptorUUID" );	
					ServiceSpecCharacteristic cMANOproviderName = spec.getServiceSpecCharacteristicByName( "MANOproviderName" );


					String OSM_NSDCATALOGID = "";
					String OnBoardDescriptorID = "";
					String OnBoardDescriptorUUID = "";
					String MANOproviderName = "";
					
					try {
						if ( cOSM_NSDCATALOGID!=null && OnBoardDescriptorID!=null && OnBoardDescriptorUUID!=null && MANOproviderName!=null ) {
							OSM_NSDCATALOGID = cOSM_NSDCATALOGID.getDefaultValue();
							OnBoardDescriptorID = cOnBoardDescriptorID.getDefaultValue();
							OnBoardDescriptorUUID = cOnBoardDescriptorUUID.getDefaultValue();
							MANOproviderName = cMANOproviderName.getDefaultValue();						
						} else {
							throw new Exception( "Found NULL mandatory values. Instnatiation cannot proceed"  );
						}
						
					} catch (Exception e) {
						logger.error("Cannot create DeploymentDescriptor request");	
						e.printStackTrace();					
					}
						

					try {
						NetworkServiceDescriptor refnsd = serviceOrderManager.retrieveNSD( NSDID );
						
						
						if ( refnsd == null ) {
							logger.error("NetworkServiceDescriptor cannot be retrieved, NSDID: " + NSDID );
							execution.setVariable("deploymentId", null);
							noteItem.setText("Request to NFVO FAILED. NetworkServiceDescriptor cannot be retrieved, NSDID: " + NSDID);
							throw new Exception( "NetworkServiceDescriptor cannot be retrieved, NSDID: " + NSDID );
						}
						
						Map<String, Object> configParams = aDependencyRulesSolver.get( sorder, spec );
						
						DeploymentDescriptor dd = createNewDeploymentRequest( aService, refnsd, 
								sorder.getStartDate(), 
								sorder.getExpectedCompletionDate(), 
								sorder.getId(),
								configParams, OSM_NSDCATALOGID, OnBoardDescriptorID, OnBoardDescriptorUUID);
						
						su.setState(ServiceStateType.RESERVED );
						Note successNoteItem = new Note();
						successNoteItem.setText(String.format("Request to NFVO %s with Deployment Request id:%s",
								MANOproviderName,
								dd.getId()));
						successNoteItem.setDate( OffsetDateTime.now(ZoneOffset.UTC).toString() );
						successNoteItem.setAuthor( compname );
						su.addNoteItem( successNoteItem );
						
						Characteristic serviceCharacteristicItem = new Characteristic();
						serviceCharacteristicItem.setName( "DeploymentRequestID" );
						serviceCharacteristicItem.setValue( new Any( dd.getId() + "" ));
						su.addServiceCharacteristicItem(serviceCharacteristicItem);
						
						serviceCharacteristicItem = new Characteristic();
						serviceCharacteristicItem.setName( "Status" );
						serviceCharacteristicItem.setValue( new Any( dd.getStatus() + "" ));
						su.addServiceCharacteristicItem(serviceCharacteristicItem);

						serviceCharacteristicItem = new Characteristic();
						serviceCharacteristicItem.setName( "OperationalStatus" );
						serviceCharacteristicItem.setValue( new Any( dd.getOperationalStatus() + "" ));
						su.addServiceCharacteristicItem(serviceCharacteristicItem);

						serviceCharacteristicItem = new Characteristic();
						serviceCharacteristicItem.setName( "ConstituentVnfrIps" );
						serviceCharacteristicItem.setValue( new Any( dd.getConstituentVnfrIps()  + "" ));
						su.addServiceCharacteristicItem(serviceCharacteristicItem);
						

						serviceCharacteristicItem = new Characteristic();
						serviceCharacteristicItem.setName( "ConfigStatus" );
						serviceCharacteristicItem.setValue( new Any( dd.getConfigStatus() + "" ));
						su.addServiceCharacteristicItem(serviceCharacteristicItem);
						
						serviceCharacteristicItem = new Characteristic();
						serviceCharacteristicItem.setName( "APPLY_CONFIG" );
						serviceCharacteristicItem.setValue( new Any( dd.getInstantiationconfig()  + "" ));
						su.addServiceCharacteristicItem(serviceCharacteristicItem);
						
						serviceCharacteristicItem = new Characteristic();
						serviceCharacteristicItem.setName( "InstanceId" );
						serviceCharacteristicItem.setValue( new Any( dd.getInstanceId() + "" ));
						su.addServiceCharacteristicItem(serviceCharacteristicItem);


						serviceCharacteristicItem = new Characteristic();
						serviceCharacteristicItem.setName( "NSR" );
						serviceCharacteristicItem.setValue( new Any( dd.getNsr()  + "" ));
						su.addServiceCharacteristicItem(serviceCharacteristicItem);

						serviceCharacteristicItem = new Characteristic();
						serviceCharacteristicItem.setName( "NSLCM" );
						serviceCharacteristicItem.setValue( new Any( dd.getNs_nslcm_details()   + "" ));
						su.addServiceCharacteristicItem(serviceCharacteristicItem);
												
						Service supd = serviceOrderManager.updateService(  aService.getId(), su, false);
						logger.info("Request to NFVO for NSDID:" + NSDID + " done! Service: " + supd.getId() );
						
						execution.setVariable("deploymentId", dd.getId());
						
						
						return;					
					} catch (Exception e) {
						logger.error("Cannot create DeploymentDescriptor request");	
						e.printStackTrace();					
					}
					
					finally {
						
					}
					
				} else {

					logger.error( "Cannot retrieve NSDID from ServiceSpecification for service :" + spec.getId() );
				}
				
			} else {

				logger.error( "Cannot retrieve ServiceSpecification for service :" + (String) execution.getVariable("contextServiceId") );
			}
		} else {
			logger.error( "Cannot retrieve variable contextServiceId"  );
		}

		//if we get here somethign is wrong so we need to terminate the service.
		
		noteItem.setText("Request to NFVO FAILED." + noteItem.getText()  );
		noteItem.setAuthor( compname );
		noteItem.setDate( OffsetDateTime.now(ZoneOffset.UTC).toString() );
		su.addNoteItem( noteItem );
		su.setState(ServiceStateType.TERMINATED   );
		serviceOrderManager.updateService(  execution.getVariable("contextServiceId").toString(), su, false);
		
	}



	private DeploymentDescriptor createNewDeploymentRequest(Service aService, 
			NetworkServiceDescriptor refnsd, OffsetDateTime startDate, OffsetDateTime endDate, String orderid,
			Map<String, Object> configParams, String OSM_NSDCATALOGID, String OnBoardDescriptorID, String OnBoardDescriptorUUID) {
		DeploymentDescriptor ddreq = new DeploymentDescriptor();
		ExperimentMetadata expReq = refnsd;
		
		
		ddreq.setName("Service Order " + orderid);
		ddreq.setDescription("Created automatically by OSOM for Service Order " + orderid);
		ddreq.setExperiment( expReq  );
		ddreq.setStartReqDate(  new Date(startDate.toInstant().toEpochMilli()) );
		ddreq.setStartDate( new Date(startDate.toInstant().toEpochMilli()) );
		ddreq.setEndReqDate( new Date(endDate.toInstant().toEpochMilli()) );
		ddreq.setEndDate( new Date(endDate.toInstant().toEpochMilli()) );
		ddreq.setStatus( DeploymentDescriptorStatus.SCHEDULED );
		
		ExperimentOnBoardDescriptor obddescriptor = new ExperimentOnBoardDescriptor();
		obddescriptor.setId( Long.parseLong(OnBoardDescriptorID) );
		obddescriptor.setUuid(OnBoardDescriptorUUID);
		obddescriptor.setDeployId(OSM_NSDCATALOGID);		
		ddreq.setObddescriptor_uuid( obddescriptor  );

		String instantiationconfig = "{}";
		Characteristic configCharacteristic = aService.getServiceCharacteristicByName( "OSM_CONFIG" );
		if ( (configCharacteristic!=null) &&
				(configCharacteristic.getValue()  != null) &&
				(configCharacteristic.getValue().getValue() != null)
				&&
				(!configCharacteristic.getValue().getValue().equals("") )) {
			try {
				instantiationconfig = configCharacteristic.getValue().getValue();
			}catch (Exception e) {
				logger.error("cannot extract OSM_CONFIG");
				e.printStackTrace();
			}
		} else {
			configCharacteristic = null;
		}
		
//		/**
//		 * we will pass all characteristics if there is NO additionalParamsForVnf already added in confi param
//		 * {  additionalParamsForVnf: [ {member-vnf-index: "1", additionalParams: {touch_filename: your-value,  touch_filename2: your-value2} }]   }'
//		 */
//		if ( ( !instantiationconfig.contains("additionalParamsForVnf") ) &&
//				(ddreq.getExperiment() !=null ) &&
//				(ddreq.getExperiment().getConstituentVxF() !=null )){
//			
//			
//			String serviceParams="";
//			for (Characteristic chars : aService.getServiceCharacteristic()  ) {
//				if ( ( chars.getValue()!= null ) && ( !chars.getName().equals("OSM_CONFIG") )) {
//					if (!chars.getName().contains( "Primitive::") ) {
//						serviceParams = serviceParams + "\"" + chars.getName() + "\" : \"" + chars.getValue().getValue() + "\",";						
//					}					
//				}				
//			}
//			serviceParams = serviceParams + " \"_lastParam\": \"_last\"";
//			
//
//			serviceParams = "\"additionalParams\": {" + serviceParams + "}";
//			
//			StringBuilder additionalParamsForVnf = new StringBuilder();
//
//			additionalParamsForVnf.append(" \"additionalParamsForVnf\": [ ");
//			for (ConstituentVxF cvxf : ddreq.getExperiment().getConstituentVxF()) {
//				additionalParamsForVnf.append("{ \"member-vnf-index\": \"" + cvxf.getMembervnfIndex()  + "\", " + serviceParams + "}") ;
//				additionalParamsForVnf.append(",");
//			}
//			
//			int k = additionalParamsForVnf.lastIndexOf(",");
//			if ( k>=0 ) { 
//				additionalParamsForVnf.delete( k, k+1 );
//			}
//			additionalParamsForVnf.append(" ] ");
//
//			String acomma="";
//			if ( configCharacteristic!=null ) {
//				acomma = ",";
//			}
//			instantiationconfig = instantiationconfig.replaceFirst( Pattern.quote("{") , "{" + additionalParamsForVnf.toString() +  acomma );	
//		}
		
		Characteristic sshk= aService.getServiceCharacteristicByName( "SSHKEY" );
		if ( (sshk!=null) && 
				( sshk.getValue()!=null ) && 
				( sshk.getValue().getValue() !=null ) ) {
			try {
				String sshval = sshk.getValue().getValue();
				if ( sshval!=null ) {
					instantiationconfig = instantiationconfig.replaceFirst( Pattern.quote("{") , "{ \"ssh_keys\": [\"" + sshval + "\"],");					
				}
			}catch (Exception e) {
				logger.error("cannot extract SSHKEY");
				e.printStackTrace();
			}
		}

		
		instantiationconfig = instantiationconfig.replaceFirst( Pattern.quote("{") , "{\"nsName\": \"" + "Service_Order_" + orderid + "\",");	

		/**
		 * for now only if OSM_CONFIG is not empty we will pass all parameters!. WE still Need nsdId and probably vimId
		 */
		if ( configCharacteristic!=null) { 
			ddreq.setInstantiationconfig(instantiationconfig);
			logger.debug( "instantiationconfig: " + instantiationconfig );
		}
		
		
		
		DeploymentDescriptor dd =serviceOrderManager.nfvoDeploymentRequestByNSDid( ddreq );
		
		if ( dd == null ) {
			logger.error("DeploymentDescriptor is NULL");
			
		}
		return dd;
	}


	
	
}

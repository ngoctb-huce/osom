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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.etsi.osl.model.DeploymentDescriptor;
import org.etsi.osl.model.DeploymentDescriptorStatus;
import org.etsi.osl.model.DeploymentDescriptorVxFInstanceInfo;
import org.etsi.osl.tmf.common.model.Any;
import org.etsi.osl.tmf.common.model.service.Characteristic;
import org.etsi.osl.tmf.common.model.service.ServiceStateType;
import org.etsi.osl.tmf.sim638.model.Service;
import org.etsi.osl.tmf.sim638.model.ServiceUpdate;


@Component(value = "nfvOrchestrationCheckDeploymentService") //bean name
public class NFVOrchestrationCheckDeploymentService implements JavaDelegate {

	private static final transient Log logger = LogFactory.getLog(NFVOrchestrationCheckDeploymentService.class.getName());


	@Autowired
	private ServiceOrderManager serviceOrderManager;
	
	public void execute(DelegateExecution execution) {

		logger.info( "NFVOrchestrationCheckDeploymentService" );
		logger.info( execution.getVariableNames().toString() );
		Long deploymentId = (Long) execution.getVariable("deploymentId") ;
		if ( deploymentId == null) {

			logger.error( "Variable deploymentId is NULL!" );
			execution.setVariable("serviceDeploymentFinished", Boolean.TRUE );
			return;
		}
		if ( execution.getVariable("contextServiceId") == null) {

			logger.error( "Variable contextServiceId is NULL!" );
			execution.setVariable("serviceDeploymentFinished", Boolean.TRUE );
			return;
		}
		Service aService = serviceOrderManager.retrieveService( (String) execution.getVariable("contextServiceId") );

		if ( aService == null ) {
			logger.info( "aService is null for contextServiceId = " +(String) execution.getVariable("contextServiceId") );			
			execution.setVariable("serviceDeploymentFinished", Boolean.TRUE );
			return;
		}
		//retrieve Status from NFVO (OSM?) scheduler
		logger.info("Checking Deployment Status of deployment Request id: " + deploymentId );

		DeploymentDescriptor dd =serviceOrderManager.retrieveNFVODeploymentRequestById( deploymentId );

		if ( dd == null) {

			logger.error( "DeploymentDescriptor dd is NULL!" );
			execution.setVariable("serviceDeploymentFinished", Boolean.TRUE );
			return;
		}

		execution.setVariable("serviceDeploymentFinished", Boolean.FALSE );
		
		logger.info("Operational Status of deployment Request id: " + dd.getOperationalStatus() );
		logger.info("Status of deployment Request id: " + dd.getStatus() );
		ServiceUpdate supd = new ServiceUpdate();
		boolean aVNFINDEXREFadded = false;
		
		boolean propagateToSO = false;
		
		if ( aService.getServiceCharacteristic() != null ) {
			for (Characteristic c : aService.getServiceCharacteristic()) {
				if ( c.getName().equals("Status")) {
					c.setValue( new Any( dd.getStatus() + "" ));
				} else if ( c.getName().equals("OperationalStatus")) {
					c.setValue( new Any( dd.getOperationalStatus() + "" ));
				} else if ( c.getName().equals("ConstituentVnfrIps")) {
					c.setValue( new Any( dd.getConstituentVnfrIps() + "" ));
				} else if ( c.getName().equals("ConfigStatus")) {
					c.setValue( new Any( dd.getConfigStatus() + "" ));
				}  else if ( c.getName().equals("InstanceId")) {
					c.setValue( new Any( dd.getInstanceId() + "" ));
				} else if ( c.getName().equals("NSR")) {
					c.setValue( new Any( dd.getNsr() + "" ));
					propagateToSO = true;
				} else if ( c.getName().equals("NSLCM")) {
					c.setValue( new Any( dd.getNs_nslcm_details() + "" ));
					propagateToSO = true;
				}				
				if ( dd.getDeploymentDescriptorVxFInstanceInfo() !=null ) {
					for ( DeploymentDescriptorVxFInstanceInfo vnfinfo : dd.getDeploymentDescriptorVxFInstanceInfo() ) {
						if ( c.getName().equals(  "VNFINDEXREF_INFO_" + vnfinfo.getMemberVnfIndexRef() )) {
							c.setValue( new Any( vnfinfo.getVxfInstanceInfo()  + "" ));
							aVNFINDEXREFadded = true;
							propagateToSO = true;
						} 
						
					}
				}
				
				supd.addServiceCharacteristicItem( c );					
			}
			
			if (!aVNFINDEXREFadded) {
				if ( dd.getDeploymentDescriptorVxFInstanceInfo() !=null ) {
					for (DeploymentDescriptorVxFInstanceInfo vnfinfo : dd.getDeploymentDescriptorVxFInstanceInfo()) {
						if ( vnfinfo.getMemberVnfIndexRef()!=null ){
							Characteristic serviceCharacteristicItem = new Characteristic();
							serviceCharacteristicItem.setName( "VNFINDEXREF_INFO_" + vnfinfo.getMemberVnfIndexRef() );
							serviceCharacteristicItem.setValue( new Any( vnfinfo.getVxfInstanceInfo()  ));
							supd.addServiceCharacteristicItem(serviceCharacteristicItem);
						}								
					}							
				}
			}
			
			
		} else {
			logger.error("Service has no characteristics!" );
			
		}
		
		if ( dd.getStatus().equals( DeploymentDescriptorStatus.RUNNING) ) {
			supd.setState( ServiceStateType.ACTIVE);
		} else if ( dd.getStatus().equals( DeploymentDescriptorStatus.FAILED) ) {
			supd.setState( ServiceStateType.INACTIVE );
		} else if ( dd.getStatus().equals( DeploymentDescriptorStatus.REJECTED) 
				|| dd.getStatus().equals( DeploymentDescriptorStatus.FAILED_OSM_REMOVED)
				|| dd.getStatus().equals( DeploymentDescriptorStatus.COMPLETED)
				|| dd.getStatus().equals( DeploymentDescriptorStatus.TERMINATED) 
				|| dd.getStatus().equals( DeploymentDescriptorStatus.TERMINATION_FAILED) ) {
			supd.setState( ServiceStateType.TERMINATED );
		}
		
		Service serviceResult = serviceOrderManager.updateService( aService.getId(), supd, propagateToSO );
		
		if ( serviceResult!= null ) {
			if ( serviceResult.getState().equals(ServiceStateType.ACTIVE)
					||serviceResult.getState().equals(ServiceStateType.INACTIVE)
					|| serviceResult.getState().equals(ServiceStateType.TERMINATED)) {

				logger.info("Deployment Status OK. Service state = " + serviceResult.getState() );
				execution.setVariable("serviceDeploymentFinished", Boolean.TRUE);
				return;
			}			
		}
		logger.info("Wait For Deployment Status. ");
		
		

		
		
	}


	
}

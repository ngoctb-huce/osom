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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import org.etsi.osl.tmf.common.model.service.Note;
import org.etsi.osl.tmf.common.model.service.ResourceRef;
import org.etsi.osl.tmf.common.model.service.ServiceRef;
import org.etsi.osl.tmf.common.model.service.ServiceStateType;
import org.etsi.osl.tmf.sim638.model.Service;
import org.etsi.osl.tmf.so641.model.ServiceOrder;
import org.etsi.osl.tmf.so641.model.ServiceOrderItem;
import org.etsi.osl.tmf.so641.model.ServiceOrderStateType;
import org.etsi.osl.tmf.so641.model.ServiceOrderUpdate;
import jakarta.validation.Valid;


@Component(value = "orderCompleteService") //bean name
public class OrderCompleteService implements JavaDelegate {

	private static final transient Log logger = LogFactory.getLog(OrderCompleteService.class.getName());

	@Value("${spring.application.name}")
	private String compname;

    @Autowired
    private ServiceOrderManager serviceOrderManager;
    

    @Autowired
    NFVOrchestrationCheckDeploymentService nfvOrchestrationCheckDeploymentService;
    
	public void execute(DelegateExecution execution) {

		logger.info("OrderCompleteService:" + execution.getVariableNames().toString() );

		if (execution.getVariable("orderId")!=null) {
			logger.info("Will check status of services of orderid:" + execution.getVariable("orderId") );
			
			ServiceOrder sOrder = serviceOrderManager.retrieveServiceOrder((String) execution.getVariable("orderId") );
			
			if (sOrder == null) {
				logger.error("Cannot retrieve Service Order details from catalog.");
				return;
			}

			for (ServiceOrderItem soi : sOrder.getOrderItem()) {
				for (ServiceRef sref : soi.getService().getSupportingService()) {
					Service aService = serviceOrderManager.retrieveService(sref.getId());

					if ((aService != null)
							&& (aService.getServiceCharacteristicByName("externalPartnerServiceId") != null)) {
						// service belongs to a partner. Here we might query it

					} else if ((aService != null)
							&& (aService.getServiceCharacteristicByName("DeploymentRequestID") != null)) {

						if (nfvOrchestrationCheckDeploymentService != null) {
							String deploymentRequestID = aService.getServiceCharacteristicByName("DeploymentRequestID")
									.getValue().getValue();

							if ( deploymentRequestID.length()>0 ) {
								execution.setVariable("deploymentId", Long.parseLong(deploymentRequestID));
								execution.setVariable("serviceId", aService.getUuid());
								execution.setVariable("contextServiceId", aService.getUuid());

								nfvOrchestrationCheckDeploymentService.execute(execution);								
							}

						}

					}
				}

			}


			@Valid
			ServiceOrderStateType currentState = sOrder.getState();
				
			boolean allCompletedItemsInOrder= true;
			boolean allTerminatedItemsInOrder= true;
			boolean existsInactiveInORder= false;
			boolean existsTerminatedInORder= false;
			//boolean updateServiceOrder= false;
			
			logger.info("ServiceOrder id:" + sOrder.getId());
			for (ServiceOrderItem soi : sOrder.getOrderItem()) {
				boolean existsReserved=false;
				boolean existsInactive=false;
				boolean existsActive=false;
				boolean existsTerminated=false;
				boolean allTerminated= ( soi.getService().getSupportingService() != null && soi.getService().getSupportingService().size()>0) 
						|| ( soi.getService().getSupportingResource() != null && soi.getService().getSupportingResource().size()>0 );
				boolean existsDesigned=false;
				boolean allActive= ( soi.getService().getSupportingService() != null && soi.getService().getSupportingService().size()>0) 
						|| ( soi.getService().getSupportingResource() != null && soi.getService().getSupportingResource().size()>0 );
				
				
				if ( soi.getService().getSupportingService() != null) {
					for (ServiceRef sr : soi.getService().getSupportingService()) {
						Service srv = serviceOrderManager.retrieveService( sr.getId() );
						existsReserved = existsReserved || srv.getState().equals(ServiceStateType.RESERVED );
						existsInactive = existsInactive || srv.getState().equals(ServiceStateType.INACTIVE );
						existsDesigned = existsDesigned || srv.getState().equals(ServiceStateType.DESIGNED );
						existsActive  = existsActive || srv.getState().equals(ServiceStateType.ACTIVE );
						existsTerminated  = existsTerminated || srv.getState().equals(ServiceStateType.TERMINATED );
						allTerminated = allTerminated && srv.getState().equals(ServiceStateType.TERMINATED );
						allActive = allActive && srv.getState().equals(ServiceStateType.ACTIVE );
					}						
				}
				
				
				if ( soi.getService().getSupportingResource() != null) {
					for (ResourceRef rr : soi.getService().getSupportingResource()) {
						Service srv = serviceOrderManager.retrieveService( rr.getId() );
						existsReserved = existsReserved || srv.getState().equals(ServiceStateType.RESERVED );
						existsInactive = existsInactive || srv.getState().equals(ServiceStateType.INACTIVE );
						existsDesigned = existsDesigned || srv.getState().equals(ServiceStateType.DESIGNED );
						existsActive  = existsActive || srv.getState().equals(ServiceStateType.ACTIVE );
						existsTerminated  = existsTerminated || srv.getState().equals(ServiceStateType.TERMINATED );
						allTerminated = allTerminated && srv.getState().equals(ServiceStateType.TERMINATED );
						allActive = allActive && srv.getState().equals(ServiceStateType.ACTIVE );
					}					
				}
				
				@Valid
				ServiceStateType sserviceState = soi.getService().getState();
				if (allActive) {
					sserviceState = ServiceStateType.ACTIVE;
					soi.setState( ServiceOrderStateType.COMPLETED );		
				} else if (allTerminated) {
					sserviceState = ServiceStateType.TERMINATED;	
					soi.setState( ServiceOrderStateType.COMPLETED );	
					existsTerminatedInORder = true;				
				} else if (existsInactive) {
					sserviceState = ServiceStateType.INACTIVE;		
					soi.setState( ServiceOrderStateType.FAILED );			
					existsInactiveInORder = true;
				} else if (existsDesigned) {
					sserviceState = ServiceStateType.DESIGNED;	
					soi.setState( ServiceOrderStateType.INPROGRESS );					
				} else if (existsReserved) {
					sserviceState = ServiceStateType.RESERVED;	
					soi.setState( ServiceOrderStateType.INPROGRESS );						
				}
				
				
				
				
//				if ( soi.getService().getState() != sserviceState ) {
//					updateServiceOrder = true;
//				}
				soi.getService().setState(sserviceState);	

//				allCompletedItemsInOrder = allCompletedItemsInOrder && soi.getService().getState().equals( ServiceStateType.ACTIVE  );
//				allTerminatedItemsInOrder = allTerminatedItemsInOrder && soi.getService().getState().equals( ServiceStateType.TERMINATED  );
				
				allCompletedItemsInOrder = allCompletedItemsInOrder && soi.getState().equals( ServiceOrderStateType.COMPLETED );
				allTerminatedItemsInOrder = allTerminatedItemsInOrder && soi.getState().equals( ServiceOrderStateType.COMPLETED );

				logger.info("ServiceOrderItem state:" + sserviceState.toString() );
			}
			
			   
			if (allCompletedItemsInOrder || allTerminatedItemsInOrder) {
				//updateServiceOrder = true;
				sOrder.setState( ServiceOrderStateType.COMPLETED );				
			} else if ( existsInactiveInORder ) {
				sOrder.setState( ServiceOrderStateType.FAILED );		
			}  else if ( existsTerminatedInORder ) {
				sOrder.setState( ServiceOrderStateType.FAILED );		
			} 
			
			if ( currentState != sOrder.getState() ) {
				logger.info("Will update ServiceOrder with state:" +  sOrder.getState() );
				ServiceOrderUpdate serviceOrderUpd = new ServiceOrderUpdate();
				serviceOrderUpd.setState( sOrder.getState() );
				
				for (ServiceOrderItem orderItemItem : sOrder.getOrderItem()) {
					serviceOrderUpd.addOrderItemItem(orderItemItem);
				}
				
				Note noteItem = new Note();
				noteItem.setText( String.format( "Service Order State is: %s " ,  serviceOrderUpd.getState()) );
				noteItem.setDate( OffsetDateTime.now(ZoneOffset.UTC).toString() );
				noteItem.setAuthor( compname );
				serviceOrderUpd.addNoteItem( noteItem );
				
				serviceOrderManager.updateServiceOrderOrder( sOrder.getId() , serviceOrderUpd);

			}
			
		}
		
	}

}

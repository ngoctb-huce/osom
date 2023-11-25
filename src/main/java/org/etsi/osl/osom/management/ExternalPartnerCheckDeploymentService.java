/*-
 * ========================LICENSE_START=================================
 * org.etsi.osl.osom
 * %%
 * Copyright (C) 2019 - 2020 openslice.io
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
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.etsi.osl.osom.partnerservices.PartnerOrganizationServicesManager;
import org.etsi.osl.tmf.common.model.Any;
import org.etsi.osl.tmf.common.model.UserPartRoleType;
import org.etsi.osl.tmf.common.model.service.Characteristic;
import org.etsi.osl.tmf.common.model.service.Note;
import org.etsi.osl.tmf.common.model.service.ServiceRef;
import org.etsi.osl.tmf.common.model.service.ServiceSpecificationRef;
import org.etsi.osl.tmf.common.model.service.ServiceStateType;
import org.etsi.osl.tmf.pm632.model.Organization;
import org.etsi.osl.tmf.prm669.model.RelatedParty;
import org.etsi.osl.tmf.scm633.model.ServiceSpecification;
import org.etsi.osl.tmf.sim638.model.Service;
import org.etsi.osl.tmf.sim638.model.ServiceCreate;
import org.etsi.osl.tmf.sim638.model.ServiceOrderRef;
import org.etsi.osl.tmf.sim638.model.ServiceUpdate;
import org.etsi.osl.tmf.so641.model.ServiceOrder;
import org.etsi.osl.tmf.so641.model.ServiceOrderItem;
import org.etsi.osl.tmf.so641.model.ServiceOrderStateType;
import org.etsi.osl.tmf.so641.model.ServiceOrderUpdate;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component(value = "externalPartnerCheckDeploymentService") //bean name
public class ExternalPartnerCheckDeploymentService  implements JavaDelegate {
	
	private static final transient Log logger = LogFactory.getLog( ExternalPartnerCheckDeploymentService.class.getName());


	@Value("${spring.application.name}")
	private String compname;
	
	@Autowired
	private ServiceOrderManager serviceOrderManager;
	
	@Autowired
	private PartnerOrganizationServicesManager partnerOrganizationServicesManager;
	
	
	public void execute(DelegateExecution execution) {

		logger.info( "ExternalPartnerCheckDeploymentService" );
		logger.debug( execution.getVariableNames().toString() );
		String externalServiceOrderId = (String) execution.getVariable("externalServiceOrderId") ;
		execution.setVariable("serviceDeploymentFinished",   false );

		ServiceOrder sorder = serviceOrderManager.retrieveServiceOrder( execution.getVariable("orderid").toString() );
		Service aLocalWrapperProxyService = serviceOrderManager.retrieveService( (String) execution.getVariable("contextServiceId") );
		logger.debug("Check external partner for Service name:" + aLocalWrapperProxyService.getName() );
		logger.debug("Check external partner for  Service state:" + aLocalWrapperProxyService.getState()  );			
		logger.debug("Request to External Service Partner for Service: " + aLocalWrapperProxyService.getId() );

		logger.debug("Checking Order Status of Order Request id: " + externalServiceOrderId );
		ServiceSpecification spec = serviceOrderManager.retrieveServiceSpec( aLocalWrapperProxyService.getServiceSpecificationRef().getId() );
		RelatedParty rpOrg = null;
		if ( spec.getRelatedParty() != null ) {
			for (RelatedParty rp : spec.getRelatedParty()) {
				if ( rp.getRole().equals( UserPartRoleType.ORGANIZATION.getValue() )) {
					rpOrg =rp;
					break;
				}				
			}			
		}
		Organization orgz = serviceOrderManager.getExternalPartnerOrganization( rpOrg.getId() );
		
		if ( orgz == null) {
			logger.error("Cannot retrieve partner organization"  );	
			
			return;
		}
		
		logger.debug("External partner organization:" + orgz.getName()  );	
		
		ServiceOrder externalSOrder = partnerOrganizationServicesManager.retrieveServiceOrder( orgz, externalServiceOrderId );
		ServiceUpdate serviceProxyUpdate = new ServiceUpdate();
		
		
		
		if (externalSOrder != null ) {
			logger.info("External partner organization order state:" + externalSOrder.getState()  );	
			if ( externalSOrder.getState().equals( ServiceOrderStateType.COMPLETED )){
				serviceProxyUpdate.setState( ServiceStateType.ACTIVE);
			} else if ( externalSOrder.getState().equals( ServiceOrderStateType.ACKNOWLEDGED ) ||
					externalSOrder.getState().equals( ServiceOrderStateType.INPROGRESS )){
				serviceProxyUpdate.setState( ServiceStateType.RESERVED );
			} else if ( externalSOrder.getState().equals( ServiceOrderStateType.CANCELLED ) ||
					externalSOrder.getState().equals( ServiceOrderStateType.FAILED ) ||
					externalSOrder.getState().equals( ServiceOrderStateType.REJECTED )){
				serviceProxyUpdate.setState( ServiceStateType.TERMINATED );
			}	else if ( externalSOrder.getState().equals( ServiceOrderStateType.INITIAL ) ||
					externalSOrder.getState().equals( ServiceOrderStateType.PENDING )){
				serviceProxyUpdate.setState( ServiceStateType.RESERVED );
			}		else if ( externalSOrder.getState().equals( ServiceOrderStateType.PARTIAL )){
				serviceProxyUpdate.setState( ServiceStateType.INACTIVE );
			}
			
			
			/**
			 * update now service characteristics from the remote Service Inventory
			 */
			
				for (ServiceOrderItem ext_soi : externalSOrder.getOrderItem()) {
					for (ServiceRef serviceRef : ext_soi.getService().getSupportingService()) {
						Service remotePartnerService = partnerOrganizationServicesManager.retrieveServiceFromInventory( orgz, serviceRef.getId() );
						//we need to create here on our partner, Services in our ServiceInventory that reflect the remote Services in the partnerService Inventory!
						if (remotePartnerService!=null) {							
							
							boolean foundInInventory = false;
							
							List<String> serviceids = serviceOrderManager.retrieveServicesOfOrder( sorder.getId()  );
							for (String sid : serviceids) {
								Service lservice = serviceOrderManager.retrieveService(sid);
								Characteristic charexternalPartnerServiceId = lservice.getServiceCharacteristicByName("externalPartnerServiceId");
								if ((  charexternalPartnerServiceId!= null ) && (  charexternalPartnerServiceId.getValue()!= null )) {
									if ( charexternalPartnerServiceId.getValue().getValue().equals( remotePartnerService.getId() ) ) {
										foundInInventory = true;
										//we can update also the current service with the one from remote service inventory
										ServiceUpdate supd = new ServiceUpdate();
										for (Characteristic c : remotePartnerService.getServiceCharacteristic()) {
											c.setUuid(null);
											if ( !c.getName().equals("EXEC_ACTION")) {
												supd.addServiceCharacteristicItem(c);							
											}
										}
										supd.setState( remotePartnerService.getState() );
										serviceOrderManager.updateService( lservice.getId(), supd , false);
									}
									
								}
							}
							
							if ( !foundInInventory ) {
								Service addedPartnerService = addServiceFromPartnerOrg( 
										sorder,
										externalSOrder,
										aLocalWrapperProxyService, 
										spec,
										orgz, 
										remotePartnerService, 
										externalServiceOrderId);
								

									ServiceRef supportingServiceRef = new ServiceRef();
									supportingServiceRef.setId( addedPartnerService.getId() );
									supportingServiceRef.setReferredType( addedPartnerService.getName() );
									supportingServiceRef.setName( addedPartnerService.getName()  );
									serviceProxyUpdate.addSupportingServiceItem(supportingServiceRef);
								
									if ( remotePartnerService.getServiceCharacteristic() != null ) {
										for (Characteristic c : remotePartnerService.getServiceCharacteristic()) {
											c.setUuid( null );
											c.setName( orgz.getName()  
													+ "::" 
													+ remotePartnerService.getName() 
													+ "::" 
													+ c.getName());// we prefix here with the Service Name of external partner.
											serviceProxyUpdate.addServiceCharacteristicItem( c );	
										}
									}
								
							}
							
							
						} else {
							logger.error("ExternalPartnerCheckDeploymentService cannot retrieve remotePartnerService!"); 
						}
						
						
					}
				}
			
			
		}
		
		if ( aLocalWrapperProxyService.getState() != serviceProxyUpdate.getState()) {

			String partnerNotes = "";
			if ( externalSOrder.getNote()!=null) {
				for (Note note : externalSOrder.getNote()) {
					partnerNotes += note.getText() + ".";
				}
				partnerNotes = " Notes from external partner order: " + partnerNotes;
			}			
			
			Note noteItem = new Note();
			noteItem.setText("Update Service Order State to: " + serviceProxyUpdate.getState() + ". "+  partnerNotes);
			noteItem.setDate( OffsetDateTime.now(ZoneOffset.UTC).toString() );
			noteItem.setAuthor( compname );
			serviceProxyUpdate.addNoteItem( noteItem );
			Service serviceResult = serviceOrderManager.updateService( aLocalWrapperProxyService.getId(), serviceProxyUpdate, false );
			if ( serviceResult!=null ) {
				if ( serviceResult.getState().equals(ServiceStateType.ACTIVE)
						|| serviceResult.getState().equals(ServiceStateType.TERMINATED)) {

					logger.info("Request Deployment Status OK. Service state = " + serviceResult.getState() );
					execution.setVariable("serviceDeploymentFinished", true);
					return;
				}				
			} else {
				logger.error("Request Deployment Status ERROR from External Parnter with null serviceResult " );
			}
		}
		logger.info("Wait For  External Service Partner Status. ");
		
		
	}


	private Service addServiceFromPartnerOrg(ServiceOrder localSOrder, 
			ServiceOrder externalSOrder, 
			Service aLocalWrapperService, 
			ServiceSpecification aLocalServiceSpec,
			Organization orgz,
			Service remotePartnerService, 
			String externalServiceOrderId) {

		ServiceCreate serviceToCreate = new ServiceCreate();//the object to update the service
		serviceToCreate.setName( orgz.getName()  + "::"+ remotePartnerService.getName() );
		serviceToCreate.setState( remotePartnerService.getState() );
		serviceToCreate.setCategory( remotePartnerService.getCategory() );
		serviceToCreate.setType( remotePartnerService.getType());
		serviceToCreate.setServiceDate(remotePartnerService.getServiceDate() );
		serviceToCreate.setStartDate( remotePartnerService.getStartDate() );
		serviceToCreate.setEndDate( remotePartnerService.getEndDate()  );
		serviceToCreate.hasStarted( remotePartnerService.isHasStarted() );
		serviceToCreate.setIsServiceEnabled( remotePartnerService.isIsServiceEnabled() );
		serviceToCreate.setStartMode( remotePartnerService.getStartMode() );
		
		Note noteItem = new Note();
		noteItem.setText("Service Created by ExternalPartnerCheckDeploymentService as a reference to the external Service Inventory of Partner " + orgz.getName() + 
				". External ServiceID = " + remotePartnerService.getId());
		noteItem.setAuthor( compname );
		serviceToCreate.addNoteItem(noteItem);
		
		ServiceOrderRef serviceOrderref = new ServiceOrderRef();
		serviceOrderref.setId( localSOrder.getId() );
		serviceOrderref.setServiceOrderItemId( localSOrder.getId() );
		serviceToCreate.addServiceOrderItem(serviceOrderref );
		
		ServiceSpecificationRef serviceSpecificationRef = new ServiceSpecificationRef();
		serviceSpecificationRef.setId( remotePartnerService.getServiceSpecificationRef().getId() );
		serviceSpecificationRef.setName( remotePartnerService.getServiceSpecificationRef().getName());
		serviceToCreate.setServiceSpecificationRef(serviceSpecificationRef );
		
		if (aLocalServiceSpec.getRelatedParty()!=null) {
			for (RelatedParty rp : aLocalServiceSpec.getRelatedParty()) {
				rp.setUuid(null); 
				rp.setExtendedInfo( remotePartnerService.getId() );
				serviceToCreate.addRelatedPartyItem(rp);
			}			
		}
		
		//copy all characteristics
		for (Characteristic iterableChar : remotePartnerService.getServiceCharacteristic() ) {
			Characteristic serviceCharacteristicItem =  new Characteristic();
			serviceCharacteristicItem.setName( iterableChar.getName() );
			serviceCharacteristicItem.setValueType( iterableChar.getValueType() );
						
			Any val = new Any();
			val.setValue( iterableChar.getValue().getValue() );
			val.setAlias( iterableChar.getValue().getAlias() );
			
			serviceCharacteristicItem.setValue( val );
			serviceToCreate.addServiceCharacteristicItem( serviceCharacteristicItem );
		}
		
		//add as extra characteristics:	
		Characteristic serviceCharacteristicItem = new Characteristic();
		serviceCharacteristicItem.setName( "externalServiceOrderId" );		
		String vals = externalSOrder.getId() + "";
		Any any = new Any( vals );
		serviceCharacteristicItem.setValue( any );
		serviceToCreate.addServiceCharacteristicItem(serviceCharacteristicItem);
		

		serviceCharacteristicItem = new Characteristic();
		serviceCharacteristicItem.setName( "externalPartnerServiceId" );		
		vals = remotePartnerService.getId() + "";
		any = new Any( vals );
		serviceCharacteristicItem.setValue( any );
		serviceToCreate.addServiceCharacteristicItem(serviceCharacteristicItem);
		
		
		Service createdService = serviceOrderManager.createService(  serviceToCreate, localSOrder, aLocalServiceSpec);
		
		//we need to add also this service as supporting ServiceOrderItem to the LocalServiceOrder

		ServiceOrderItem orderItemItem = new ServiceOrderItem();

		for (ServiceOrderItem soi : localSOrder.getOrderItem()) {
			if (soi.getService().getServiceSpecification().getId().equals( aLocalServiceSpec.getUuid())) {
				ServiceRef supportingServiceRef = new ServiceRef();
				supportingServiceRef.setId( createdService.getId() );
				supportingServiceRef.setReferredType( createdService.getName() );
				supportingServiceRef.setName( createdService.getName()  );
				soi.getService().addSupportingServiceItem(supportingServiceRef );	
				orderItemItem = soi;
				break;
				
			} else {
				
				
				
				if ( soi.getService().getSupportingService() != null ) {
					for (ServiceRef soiServiceRef : soi.getService().getSupportingService() ) {
						if ( soiServiceRef.getId().equals( aLocalWrapperService.getUuid())) {
							ServiceRef supportingServiceRef = new ServiceRef();
							supportingServiceRef.setId( createdService.getId() );
							supportingServiceRef.setReferredType( createdService.getName() );
							supportingServiceRef.setName( createdService.getName()  );
							soi.getService().addSupportingServiceItem(supportingServiceRef );	
							orderItemItem = soi;
							break;
						}			
						
					}	
					
				}
			}
			
			
		}
		
		
		
		ServiceOrderUpdate serviceOrderUpd = new ServiceOrderUpdate();				
		serviceOrderUpd.addOrderItemItem(orderItemItem);
		serviceOrderManager.updateServiceOrderOrder( localSOrder.getId(), serviceOrderUpd );
		
		return createdService;
	}
		

}

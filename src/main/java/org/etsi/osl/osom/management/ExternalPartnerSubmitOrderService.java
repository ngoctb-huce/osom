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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.etsi.osl.osom.partnerservices.PartnerOrganizationServicesManager;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.etsi.osl.tmf.common.model.Any;
import org.etsi.osl.tmf.common.model.UserPartRoleType;
import org.etsi.osl.tmf.common.model.service.Characteristic;
import org.etsi.osl.tmf.common.model.service.Note;
import org.etsi.osl.tmf.common.model.service.ServiceSpecificationRef;
import org.etsi.osl.tmf.common.model.service.ServiceStateType;
import org.etsi.osl.tmf.pm632.model.Organization;
import org.etsi.osl.tmf.prm669.model.RelatedParty;
import org.etsi.osl.tmf.scm633.model.ServiceSpecification;
import org.etsi.osl.tmf.sim638.model.Service;
import org.etsi.osl.tmf.sim638.model.ServiceUpdate;
import org.etsi.osl.tmf.so641.model.ServiceOrder;
import org.etsi.osl.tmf.so641.model.ServiceOrderCreate;
import org.etsi.osl.tmf.so641.model.ServiceOrderItem;
import org.etsi.osl.tmf.so641.model.ServiceOrderStateType;
import org.etsi.osl.tmf.so641.model.ServiceRestriction;

@Component(value = "externalPartnerSubmitOrderService") //bean name
public class ExternalPartnerSubmitOrderService  implements JavaDelegate {


	private static final transient Log logger = LogFactory.getLog( ExternalPartnerSubmitOrderService.class.getName());

	@Value("${spring.application.name}")
	private String compname;

	@Autowired
	private ServiceOrderManager serviceOrderManager;
	
	@Autowired
	private PartnerOrganizationServicesManager partnerOrganizationServicesManager;

	@Value("${THIS_PARTNER_NAME}")
	private String THIS_PARTNER_NAME = "";
	
	@Override
	public void execute(DelegateExecution execution) {

		logger.info( "ExternalPartnerSubmitOrderService" );
		logger.info( "VariableNames:" + execution.getVariableNames().toString() );
		logger.info("orderid:" + execution.getVariable("orderid").toString() );
		logger.info("contextServiceId:" + execution.getVariable("contextServiceId").toString() );
				

		ServiceUpdate su = new ServiceUpdate();//the object to update the service
		if (execution.getVariable("contextServiceId") instanceof String) {

			ServiceOrder sorder = serviceOrderManager.retrieveServiceOrder( execution.getVariable("orderid").toString() );
			Service aService = serviceOrderManager.retrieveService( (String) execution.getVariable("contextServiceId") );
			logger.info("Service name:" + aService.getName() );
			logger.info("Service state:" + aService.getState()  );			
			logger.info("Request to External Service Partner for Service: " + aService.getId() );

			ServiceSpecification spec = serviceOrderManager.retrieveServiceSpec( aService.getServiceSpecificationRef().getId() );
			
			if ( spec!=null ) {
				logger.info("Service spec:" + spec.getName()  );						
				RelatedParty rpOrg = null;
				if ( spec.getRelatedParty() != null ) {
					for (RelatedParty rp : spec.getRelatedParty()) {
						if ( rp.getRole().equals( UserPartRoleType.ORGANIZATION.getValue() )) {
							rpOrg =rp;
							break;
						}				
					}			
				}
				
				String remoteServiceSpecID = rpOrg.getExtendedInfo();				
				Organization orgz = serviceOrderManager.getExternalPartnerOrganization( rpOrg.getId() );
				
				if ( orgz!=null ) {
					logger.info("External partner organization:" + orgz.getName() + ". Preparing Service Order." );					
					
					ServiceOrderCreate servOrder = new ServiceOrderCreate();
					servOrder.setCategory("Automated order");
					servOrder.setDescription("Automatically created by partner " + THIS_PARTNER_NAME);
					servOrder.setRequestedStartDate( sorder.getStartDate() );
					servOrder.setRequestedCompletionDate( sorder.getExpectedCompletionDate()  );					
					
					Note noteItemOrder = new Note();
					noteItemOrder.text("Automatically created by partner " + THIS_PARTNER_NAME);
					noteItemOrder.setAuthor(THIS_PARTNER_NAME);
					servOrder.addNoteItem( noteItemOrder );

					ServiceOrderItem soi = new ServiceOrderItem();
					servOrder.getOrderItem().add(soi);
					soi.setState(ServiceOrderStateType.ACKNOWLEDGED);

					ServiceRestriction serviceRestriction = new ServiceRestriction();
					ServiceSpecificationRef aServiceSpecificationRef = new ServiceSpecificationRef();
					aServiceSpecificationRef.setId( remoteServiceSpecID );
					aServiceSpecificationRef.setName( spec.getName() );
					aServiceSpecificationRef.setVersion(spec.getVersion());

					serviceRestriction.setServiceSpecification(aServiceSpecificationRef);
					
					for (Characteristic servChar : aService.getServiceCharacteristic()) {
						servChar.setUuid(null);
						serviceRestriction.addServiceCharacteristicItem(servChar);
					}
					
					soi.setService(serviceRestriction);
					
					
					ServiceOrder externalSOrder = partnerOrganizationServicesManager.makeExternalServiceOrder( servOrder, orgz, remoteServiceSpecID );
					
					if ( externalSOrder != null ) {
						execution.setVariable("externalServiceOrderId", externalSOrder.getId());

						su.setState(ServiceStateType.FEASIBILITYCHECKED );
						Note noteItem = new Note();
						noteItem.setText( "Request to partner " + orgz.getName() + " for spec:" + spec.getName()  + " done!  ServiceOrder id: " + externalSOrder.getId());
						noteItem.setDate( OffsetDateTime.now(ZoneOffset.UTC).toString() );
						noteItem.setAuthor( compname );
						su.addNoteItem( noteItem );
						Characteristic serviceCharacteristicItem = new Characteristic();
						serviceCharacteristicItem.setName( "externalServiceOrderId" );
						
						String vals = externalSOrder.getId() + "";
						Any any = new Any( vals );
						serviceCharacteristicItem.setValue( any );
						su.addServiceCharacteristicItem(serviceCharacteristicItem);
						
						Service supd = serviceOrderManager.updateService(  aService.getId(), su, false);
						logger.info("Request to partner " + orgz.getName() + " for spec:" + spec.getName()  + " done! Service: " + supd.getId() );						
						return;						
					}
				}
				
				
			} else {
				logger.error( "Cannot retrieve ServiceSpecification for service :" + (String) execution.getVariable("serviceId") );
			}
		} else {
			logger.error( "Cannot retrieve variable serviceId"  );
		}

		//if we get here somethign is wrong so we need to terminate the service.
		Note noteItem = new Note();
		noteItem.setText("Order Request Service to External Partner FAILED");
		noteItem.setAuthor( compname );
		noteItem.setDate( OffsetDateTime.now(ZoneOffset.UTC).toString() );
		su.addNoteItem( noteItem );
		su.setState(ServiceStateType.TERMINATED   );
		serviceOrderManager.updateService(  execution.getVariable("serviceId").toString(), su, false);
		
	}

	


	
}

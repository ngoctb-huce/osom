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
import org.etsi.osl.tmf.common.model.service.Note;
import org.etsi.osl.tmf.common.model.service.ServiceRef;
import org.etsi.osl.tmf.common.model.service.ServiceStateType;
import org.etsi.osl.tmf.scm633.model.ServiceSpecCharacteristic;
import org.etsi.osl.tmf.scm633.model.ServiceSpecification;
import org.etsi.osl.tmf.sim638.model.Service;
import org.etsi.osl.tmf.sim638.model.ServiceUpdate;
import org.etsi.osl.tmf.so641.model.ServiceOrder;


@Component(value = "localSoCheckDeployment") //bean name
public class LocalSOCheckDeployment  implements JavaDelegate {

	private static final transient Log logger = LogFactory.getLog( LocalSOCheckDeployment.class.getName());


	@Value("${spring.application.name}")
	private String compname;
	@Autowired
	private ServiceOrderManager serviceOrderManager;
	
	@Autowired
	private PartnerOrganizationServicesManager partnerOrganizationServicesManager;
	
	
	public void execute(DelegateExecution execution) {

		logger.info( "LocalSOCheckDeployment" );
		logger.debug( execution.getVariableNames().toString() );

		execution.setVariable("lsoServiceDeploymentFinished",   false );

		ServiceOrder sorder = serviceOrderManager.retrieveServiceOrder( execution.getVariable("orderid").toString() );
		Service aService = serviceOrderManager.retrieveService( (String) execution.getVariable("contextServiceId") );
		logger.debug("Check LocalSOCheckDeploymentfor Service name:" + aService.getName() );
		logger.debug("Check LocalSOCheckDeployment  Service state:" + aService.getState()  );			
		logger.debug("Request for Service id: " + aService.getId() );

		ServiceSpecification spec = serviceOrderManager.retrieveServiceSpec( aService.getServiceSpecificationRef().getId() );
		
		
		//decide if the service will be active only if it's supported services are also active

		boolean allacctive=true;
		boolean existsTerminated=false;
		for (ServiceRef sref : aService.getSupportingService() ) {
			Service supportedService = serviceOrderManager.retrieveService( sref.getId() );
			if ( ! supportedService.getState().equals( ServiceStateType.ACTIVE ) ) {
				allacctive = false;
			}
			
			if ( supportedService.getState().equals( ServiceStateType.TERMINATED ) || supportedService.getState().equals( ServiceStateType.INACTIVE ) ) {
				existsTerminated = true;
			}
			
		}
		
		ServiceUpdate supd = new ServiceUpdate();

		if ( allacctive ) {
			supd.setState( ServiceStateType.ACTIVE);			
		} else if ( existsTerminated ) {
			supd.setState( ServiceStateType.TERMINATED);			
		} else {
			supd.setState( ServiceStateType.RESERVED);			
		}
		
		
		if  (spec.getName().contains("DUMMYSERVICE") ) {
			logger.info("DUMMYSERVICE status" );
			ServiceSpecCharacteristic charc = spec.findSpecCharacteristicByName( "FINALSTATUS" );
			if ( (charc!=null ) && ( charc.getServiceSpecCharacteristicValue() !=null ) ) {
				try {
					String val = charc.getServiceSpecCharacteristicValue().stream().findFirst().get().getValue().getValue();
					if (val.equals("TERMINATED") ) {
						supd.setState( ServiceStateType.TERMINATED );						
					} else if (val.equals("INACTIVE") ) {
						supd.setState( ServiceStateType.INACTIVE );						
					} else if (val.equals("RESERVED") ) {
						supd.setState( ServiceStateType.RESERVED );						
					} else if (val.equals("ACTIVE") ) {
						supd.setState( ServiceStateType.ACTIVE );						
					}
				}catch (Exception e) {

				}				
			}
		}
		
		if ( aService.getState() != supd.getState()) {			
			Note noteItem = new Note();
			noteItem.setText("Update Service State to: " + supd.getState() + ". ");
			noteItem.setDate( OffsetDateTime.now(ZoneOffset.UTC).toString() );
			noteItem.setAuthor( compname );
			supd.addNoteItem( noteItem );
			Service serviceResult = serviceOrderManager.updateService( aService.getId(), supd, false );
			if ( serviceResult.getState().equals(ServiceStateType.ACTIVE)
					|| serviceResult.getState().equals(ServiceStateType.TERMINATED)) {

				logger.info("Deployment Status OK. Service state = " + serviceResult.getState() );
				execution.setVariable("lsoServiceDeploymentFinished", true);
				return;
			}
		}
		logger.info("Wait For Local SO Service Status. ");
		
		if ( aService.getState().equals(ServiceStateType.ACTIVE)
				|| aService.getState().equals(ServiceStateType.TERMINATED)) {

			logger.info("Deployment Status OK. Service state EQUAL = " + aService.getState() );
			execution.setVariable("lsoServiceDeploymentFinished", true);
			return;
		}
		
	}
}

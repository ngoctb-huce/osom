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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.etsi.osl.tmf.sim638.model.Service;
import org.etsi.osl.tmf.sim638.model.ServiceUpdate;
import org.etsi.osl.tmf.so641.model.ServiceOrder;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component(value = "localSoInitialize") //bean name
public class LocalSOInitialize implements JavaDelegate {


	private static final transient Log logger = LogFactory.getLog( LocalSOInitialize.class.getName());
	
	@Autowired
	private ServiceOrderManager serviceOrderManager;

	@Value("${spring.application.name}")
	private String compname;
	
	@Override
	public void execute(DelegateExecution execution) {

		logger.info( "LocalSOInitialize" );
		logger.info( "VariableNames:" + execution.getVariableNames().toString() );
		logger.info("orderid:" + execution.getVariable("orderid").toString() );
		logger.info("contextServiceId:" + execution.getVariable("contextServiceId").toString() );
				

		ServiceUpdate su = new ServiceUpdate();//the object to update the service
		if (execution.getVariableLocal("contextServiceId") instanceof String) {

			ServiceOrder sorder = serviceOrderManager.retrieveServiceOrder( execution.getVariable("orderid").toString() );
			Service aService = serviceOrderManager.retrieveService( (String) execution.getVariable("contextServiceId") );
			logger.info("Service name:" + aService.getName() );
			logger.info("Service state:" + aService.getState()  );			
			logger.info("Request to External Service Partner for Service: " + aService.getId() );

//			ServiceSpecification spec = serviceOrderManager.retrieveServiceSpec( aService.getServiceSpecificationRef().getId() );
//			
//			if ( spec!=null ) {
//				logger.info("Service spec:" + spec.getName()  );						
//				
//				
//				su.setState(ServiceStateType.FEASIBILITYCHECKED );
//				Note noteItem = new Note();
//				noteItem.setText( "Local Service Orchestration initialized for spec:" + spec.getName()  + " done!");
//				noteItem.setDate( OffsetDateTime.now(ZoneOffset.UTC).toString() );
//				noteItem.setAuthor( compname );
//				su.addNoteItem( noteItem );
//				
//				
//				Service supd = serviceOrderManager.updateService(  aService.getId(), su, false);
//				logger.info("Service updated: " + supd.getId() );						
//				return;						
//				
//				
//				
//			} else {
//				logger.error( "Cannot retrieve ServiceSpecification for service :" + (String) execution.getVariableLocal("contextServiceId") );
//			}
		} else {
			logger.error( "Cannot retrieve variable contextServiceId"  );
		}

//		//if we get here somethign is wrong so we need to terminate the service.
//		Note noteItem = new Note();
//		noteItem.setText("Order Request Service for Local Service Orchestration FAILED");
//		noteItem.setAuthor( compname );
//		noteItem.setDate( OffsetDateTime.now(ZoneOffset.UTC).toString() );
//		su.addNoteItem( noteItem );
//		su.setState(ServiceStateType.TERMINATED   );
//		serviceOrderManager.updateService(  execution.getVariableLocal("contextServiceId").toString(), su, false);
		
	}
	
	
}

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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import org.etsi.osl.tmf.common.model.service.ServiceStateType;
import org.etsi.osl.tmf.sim638.model.Service;


@Component(value = "userOrderCompleteService") //bean name
public class UserOrderCompleteService implements JavaDelegate {

	private static final transient Log logger = LogFactory.getLog(UserOrderCompleteService.class.getName());

	@Value("${spring.application.name}")
	private String compname;

	@Autowired
	private ServiceOrderManager serviceOrderManager;
	
	public void execute(DelegateExecution execution) {
		logger.info("UserOrderCompleteService:" + execution.getVariableNames().toString());

		logger.info( execution.getVariableNames().toString() );
		logger.info("contextServiceId:" + execution.getVariable("contextServiceId").toString() );
		logger.info("orderid:" + execution.getVariable("orderid").toString() );

		execution.setVariable("serviceHandledManually", Boolean.TRUE ); 
		
		if (execution.getVariable("contextServiceId") instanceof String) {
			Service s = serviceOrderManager.retrieveService( (String) execution.getVariable("contextServiceId") );
			logger.info("Service name:" + s.getName() );
			logger.info("Service state:" + s.getState()  );	

			
			
			if ( !s.getState().equals(ServiceStateType.RESERVED) ) {
				execution.setVariable("serviceHandledManually", Boolean.TRUE ); 
			} else {
				execution.setVariable("serviceHandledManually", Boolean.FALSE ); 
			}
			
			
//			ServiceUpdate supd = new ServiceUpdate();
//
//			Note noteItem = new Note();
//			noteItem.setText("Service will be handled by " + execution.getVariable("brokeActivity" ));
//			
//			noteItem.setAuthor( compname );
//			
//			supd.addNoteItem(noteItem);
//			serviceOrderManager.updateService( (String)execution.getVariable("contextServiceId") , supd, false);
			
		}		

		
	}

}

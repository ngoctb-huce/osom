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
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.etsi.osl.tmf.common.model.service.Note;
import org.etsi.osl.tmf.common.model.service.ResourceRef;
import org.etsi.osl.tmf.common.model.service.ServiceStateType;
import org.etsi.osl.tmf.ri639.model.Resource;
import org.etsi.osl.tmf.sim638.model.Service;
import org.etsi.osl.tmf.sim638.model.ServiceUpdate;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import jakarta.validation.Valid;


@Component(value = "gcOrchestrationCheckDeploymentService") //bean name
public class GCOrchestrationCheckDeploymentService implements JavaDelegate {

	private static final transient Log logger = LogFactory.getLog(GCOrchestrationCheckDeploymentService.class.getName());



    @Value("${spring.application.name}")
    private String compname;

	@Autowired
	private ServiceOrderManager serviceOrderManager;
	
	public void execute(DelegateExecution execution) {

		logger.info( "CROrchestrationCheckDeploymentService" );
		logger.info( execution.getVariableNames().toString() );


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

		execution.setVariable("serviceDeploymentFinished", Boolean.FALSE );

		ServiceUpdate supd = new ServiceUpdate();
		boolean propagateToSO = false;

		//retrieve the related supporting resource by id and check its status
		//ResourceRef supresourceRef = aService.getSupportingResource().stream().findFirst().get();//we assume for now we have only one related resource

		List<Resource> rlist = new ArrayList<Resource>();
        for (ResourceRef rref : aService.getSupportingResource()) {
          Resource res = serviceOrderManager.retrieveResource(rref.getId());
          
          if (  res == null ) {
            supd.setState( ServiceStateType.TERMINATED);
            execution.setVariable("serviceDeploymentFinished", Boolean.TRUE);
            Service serviceResult = serviceOrderManager.updateService( aService.getId(), supd, propagateToSO );
            return;
          }
          rlist.add(res);
          
        }
        @Valid
        ServiceStateType currentState = aService.getState();        
        
	    ServiceStateType nextState = aService.findNextStateBasedOnResourceList(rlist);
	    
	    if (!currentState.equals(nextState)) {
	        supd.setState( nextState );     
	        Note n = new Note();
	        n.setText("Service Status Changed to: " +  nextState);
	        n.setAuthor(compname);
	        n.setDate(OffsetDateTime.now(ZoneOffset.UTC).toString());
	        supd.addNoteItem(n);	        
	        aService = serviceOrderManager.updateService( aService.getId(), supd, propagateToSO );	      
	    }
		
		if ( aService!= null ) {
			if ( aService.getState().equals(ServiceStateType.ACTIVE)
					|| aService.getState().equals(ServiceStateType.TERMINATED)) {

				logger.info("Deployment Status OK. Service state = " + aService.getState() );
				execution.setVariable("serviceDeploymentFinished", Boolean.TRUE);
				return;
			}			
		}
		logger.info("Wait For Deployment Status. ");
		
		

		
		
	}


	
}

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
import org.etsi.osl.osom.lcm.LCMRulesController;
import org.etsi.osl.tmf.common.model.UserPartRoleType;
import org.etsi.osl.tmf.prm669.model.RelatedParty;
import org.etsi.osl.tmf.scm633.model.ServiceSpecification;
import org.etsi.osl.tmf.sim638.model.EServiceStartMode;
import org.etsi.osl.tmf.sim638.model.Service;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @author ctranoris
 * this one will process one ServiceOrderItem of a specific serviceorder
 */
@Component(value = "automationCheck") // bean name
public class AutomationCheck implements JavaDelegate {

	private static final transient Log logger = LogFactory.getLog(AutomationCheck.class.getName());

	@Autowired
	private ServiceOrderManager serviceOrderManager;


	@Autowired
	private LCMRulesController lcmRulesController;
	
	
	@Value("${spring.application.name}")
	private String compname;
	
	public void execute(DelegateExecution execution) {

		logger.info("AutomationCheck:" + execution.getVariableNames().toString());
		execution.setVariable("brokeActivity", "MANUALLY" ); 
		
		ServiceSpecification spec = null;
		String contextServiceSpecId = (String) execution.getVariable("contextServiceSpecId");
		if ( contextServiceSpecId != null ) {
			spec = serviceOrderManager.retrieveServiceSpec(contextServiceSpecId);
		} else {
			return;
		}
		
		Service contextService = null;
		String contextServiceId = (String) execution.getVariable("contextServiceId");
		if ( contextServiceId != null ) {
			contextService = serviceOrderManager.retrieveService(contextServiceId);
		}else {
			return;
		}
		

		execution.setVariable("brokeActivity", "MANUALLY" ); 
		
		if ( contextService.getStartMode().equals( EServiceStartMode.AUTOMATICALLY_MANAGED.getValue() ) ) {
			
			execution.setVariable("brokeActivity", "AUTO" ); 	//the default action
			
			 if (fromPartnerOrganization(spec) != null ) {
				execution.setVariable("brokeActivity", "PARTNER" );	
			} else if (  ( spec.findSpecCharacteristicByName( "testSpecRef" ) != null ) ) {
				execution.setVariable("brokeActivity", "TESTSPEC" );
			} else if ( spec.getType().equals("ResourceFacingServiceSpecification") &&  ( spec.findSpecCharacteristicByName( "OSM_NSDCATALOGID" ) != null ) ) {
				execution.setVariable("brokeActivity", "RFS_OSM" ); 						
			}  else if ( spec.getType().equals("ResourceFacingServiceSpecification") &&  ( spec.findSpecCharacteristicByName( "_CR_SPEC" ) != null ) ) {
				execution.setVariable("brokeActivity", "RFS_CRSPEC" );
			}  else if ( spec.getType().equals("ResourceFacingServiceSpecification") &&  ( spec.findSpecCharacteristicByName( "_MT_QUERY" ) != null ) ) {
				 execution.setVariable("brokeActivity", "RFS_MTSPEC" );
			} else if ( spec.getType().equals("ResourceFacingServiceSpecification") ) {
              execution.setVariable("brokeActivity", "GRSPEC" );                      
          }		
		}


//		ServiceUpdate supd = new ServiceUpdate();
//
//		Note noteItem = new Note();
//		noteItem.setText("Service will be handled by " + execution.getVariable("brokeActivity" ));
//		
//		noteItem.setAuthor( compname );
//		
//		supd.addNoteItem(noteItem);
//		serviceOrderManager.updateService( contextService.getId() , supd, false);

		
		
	}
	
	
	private RelatedParty fromPartnerOrganization(ServiceSpecification specrel) {
		if ( specrel.getRelatedParty() != null ) {
			for (RelatedParty rp : specrel.getRelatedParty()) {
				if ( rp.getRole().equals( UserPartRoleType.ORGANIZATION.getValue() )) {
					return rp;					
				}				
			}			
		}
		return null;
	}
	
	
}

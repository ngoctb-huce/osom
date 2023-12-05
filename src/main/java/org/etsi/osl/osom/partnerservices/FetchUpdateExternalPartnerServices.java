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
package org.etsi.osl.osom.partnerservices;

import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.etsi.osl.tmf.common.model.Any;
import org.etsi.osl.tmf.pm632.model.Characteristic;
import org.etsi.osl.tmf.pm632.model.Organization;
import org.etsi.osl.tmf.scm633.model.ServiceSpecification;

@Component(value = "fetchUpdateExternalPartnerServices") // bean name
public class FetchUpdateExternalPartnerServices  implements JavaDelegate {


	@Autowired
	PartnerOrganizationServicesManager partnerOrganizationServicesManager;
	
	@Override
	public void execute(DelegateExecution execution) {
		if ( (execution.getVariable("partnerOrganization")!=null) && (execution.getVariable("partnerOrganization") instanceof String ))  {
			
			ObjectMapper mapper = new ObjectMapper();
			
			Organization org;
			try {
				org = mapper.readValue( execution.getVariable("partnerOrganization").toString(), Organization.class);
			} catch (JsonMappingException e) {
				e.printStackTrace();
				return;
			} catch (JsonProcessingException e) {
				e.printStackTrace();
				return;
			}
			
			/**
			 * Fetch partner service specs. For now there is no criteria and we fetch all in one json...
			 */
			if ( 
					( org.findPartyCharacteristic("EXTERNAL_TMFAPI_CLIENTREGISTRATIONID")!=null ) &&
					( org.findPartyCharacteristic("EXTERNAL_TMFAPI_OAUTH2TOKENURI")!=null ) &&
					( org.findPartyCharacteristic("EXTERNAL_TMFAPI_USERNAME")!=null ) &&
					( org.findPartyCharacteristic("EXTERNAL_TMFAPI_BASEURL") != null) &&
					( org.findPartyCharacteristic("EXTERNAL_TMFAPI_CLIENTREGISTRATIONID").getValue() != null) &&
					( org.findPartyCharacteristic("EXTERNAL_TMFAPI_OAUTH2TOKENURI").getValue() != null) &&
					( org.findPartyCharacteristic("EXTERNAL_TMFAPI_USERNAME").getValue() != null) &&
					( org.findPartyCharacteristic("EXTERNAL_TMFAPI_BASEURL").getValue() != null) &&
					(!org.findPartyCharacteristic("EXTERNAL_TMFAPI_CLIENTREGISTRATIONID").getValue().getValue().equals("") ) &&
					(!org.findPartyCharacteristic("EXTERNAL_TMFAPI_OAUTH2TOKENURI").getValue().getValue().equals("") ) &&
					(!org.findPartyCharacteristic("EXTERNAL_TMFAPI_USERNAME").getValue().getValue().equals("") ) &&
					(!org.findPartyCharacteristic("EXTERNAL_TMFAPI_BASEURL").getValue().getValue().equals("") )
					) {
				
				
				
				List<ServiceSpecification> specs = partnerOrganizationServicesManager.fetchServiceSpecs( org );
				
				for (ServiceSpecification serviceSpecification : specs) {
					/**
					 * add to the spec, the organization as related party
					 */

					serviceSpecification.getRelatedParty().clear();//clear all related parties if any
					serviceSpecification.getAttachment().clear();
					if ( serviceSpecification.getDescription() == null ) {
						serviceSpecification.setDescription( "Service from Organization: " + org.getName() + ", id: " + org.getId() );					
					} else {
						serviceSpecification.setDescription( "Service from Organization: " + org.getName() + ", id: " + org.getId() + ". " + serviceSpecification.getDescription());			
						
					}
					
					
					
					partnerOrganizationServicesManager.updateSpecInLocalCatalog(org.getId(),  serviceSpecification );				
				}
				
				
			} else {
				Characteristic partyCharacteristicItem = org.findPartyCharacteristic("EXTERNAL_TMFAPI_STATUS");
				if ( partyCharacteristicItem == null ) {
					partyCharacteristicItem = new Characteristic();
					partyCharacteristicItem.setName( "EXTERNAL_TMFAPI_STATUS" );
					partyCharacteristicItem.setValueType( "TEXT" );
					partyCharacteristicItem.setValue( new Any(""));
					org.addPartyCharacteristicItem(partyCharacteristicItem);
					
				}

				partyCharacteristicItem.setValue( new Any("NOT_CONFIGURED"));
				partnerOrganizationServicesManager.updateOrganizationInCatalog(org.getId(), org);
			}
			

			
			
		}

		
	}
}

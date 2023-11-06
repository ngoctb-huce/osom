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
package org.etsi.osl.osom;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.etsi.osl.model.DeploymentDescriptor;
import org.etsi.osl.model.DeploymentDescriptorStatus;
import org.etsi.osl.tmf.common.model.Any;
import org.etsi.osl.tmf.pm632.model.Characteristic;
import org.etsi.osl.tmf.pm632.model.ContactMedium;
import org.etsi.osl.tmf.pm632.model.Organization;
import org.etsi.osl.tmf.scm633.model.ServiceSpecification;
import org.etsi.osl.tmf.so641.model.ServiceOrder;

public class SPMocked {

	private static final transient Log logger = LogFactory.getLog(SPMocked.class.getName());


	private List<ServiceSpecification> updatedSpecs = new ArrayList<>();
	/**
	 * get mocked service order by id from model via bus
	 * 
	 * @param id
	 * @return
	 * @throws IOException
	 */
	public String getPartners() throws IOException {
		logger.info("getPartners id ");

		List<Organization> alist= new ArrayList<>();
		Organization oc = new Organization();
		oc.setUuid(UUID.randomUUID().toString());
		oc.setName("TESTA");
		
//		Characteristic partyCharacteristicItem =  new Characteristic();
//		partyCharacteristicItem.setName( "EXTERNAL_TMFAPI" );
//		
//		Any value = new Any();
//		
//		Map<String, Object> apiparams = new HashMap<>();
//		String[] scopes = {"admin" , "read"};
//		
//
//		apiparams.put( "CLIENTREGISTRATIONID", "authOpensliceProvider");
//		apiparams.put( "OAUTH2CLIENTID", "osapiWebClientId");
//		apiparams.put( "OAUTH2CLIENTSECRET", "secret");
//		apiparams.put( "OAUTH2SCOPES", scopes);
//		apiparams.put( "OAUTH2TOKENURI", "http://portal.openslice.io/osapi-oauth-server/oauth/token");
//		apiparams.put( "USERNAME", "admin");
//		apiparams.put( "PASSWORD", "openslice");
//		apiparams.put( "BASEURL", "http://portal.openslice.io");
//
//		ObjectMapper mapper = new ObjectMapper();
//		mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
//		String strinparams = mapper.writeValueAsString( apiparams );
//		value.setValue( strinparams );
//		
//		partyCharacteristicItem.setValue( value );
//		o.addPartyCharacteristicItem(partyCharacteristicItem );
		
		Characteristic partyCharacteristicItem = new Characteristic();
		partyCharacteristicItem.setName("EXTERNAL_TMFAPI_BASEURL");
		partyCharacteristicItem.value( new Any(  "http://portal.openslice.io" ));
		oc.addPartyCharacteristicItem(partyCharacteristicItem );
		
		partyCharacteristicItem = new Characteristic();
		partyCharacteristicItem.setName("EXTERNAL_TMFAPI_CLIENTREGISTRATIONID");
		partyCharacteristicItem.value( new Any(  "authOpensliceProvider" ));
		oc.addPartyCharacteristicItem(partyCharacteristicItem );
				
		partyCharacteristicItem = new Characteristic();
		partyCharacteristicItem.setName("EXTERNAL_TMFAPI_OAUTH2CLIENTID");
		partyCharacteristicItem.value( new Any(  "osapiWebClientId" ));
		oc.addPartyCharacteristicItem(partyCharacteristicItem );
				
		partyCharacteristicItem = new Characteristic();
		partyCharacteristicItem.setName("EXTERNAL_TMFAPI_OAUTH2CLIENTSECRET");
		partyCharacteristicItem.value( new Any(  "secret" ));
		oc.addPartyCharacteristicItem(partyCharacteristicItem );

		partyCharacteristicItem = new Characteristic();
		partyCharacteristicItem.setName("EXTERNAL_TMFAPI_OAUTH2SCOPES");
		partyCharacteristicItem.value( new Any(  "admin;read" ));
		oc.addPartyCharacteristicItem(partyCharacteristicItem );
		
		partyCharacteristicItem = new Characteristic();
		partyCharacteristicItem.setName("EXTERNAL_TMFAPI_OAUTH2TOKENURI");
		partyCharacteristicItem.value( new Any(  "http://portal.openslice.io/osapi-oauth-server/oauth/token" ));
		oc.addPartyCharacteristicItem(partyCharacteristicItem );

		partyCharacteristicItem = new Characteristic();
		partyCharacteristicItem.setName("EXTERNAL_TMFAPI_USERNAME");
		partyCharacteristicItem.value( new Any(  "admin" ));
		oc.addPartyCharacteristicItem(partyCharacteristicItem );

		partyCharacteristicItem = new Characteristic();
		partyCharacteristicItem.setName("EXTERNAL_TMFAPI_PASSWORD");
		partyCharacteristicItem.value( new Any(  "openslice" ));
		oc.addPartyCharacteristicItem(partyCharacteristicItem );

		partyCharacteristicItem = new Characteristic();
		partyCharacteristicItem.setName("EXTERNAL_TMFAPI_SERVICE_CATALOG_URLS");
		partyCharacteristicItem.value( new Any(  "" ));
		oc.addPartyCharacteristicItem(partyCharacteristicItem );

		partyCharacteristicItem = new Characteristic();
		partyCharacteristicItem.setName("EXTERNAL_TMFAPI_SERVICE_CATEGORY_URLS");
		partyCharacteristicItem.value( new Any(  "" ));
		oc.addPartyCharacteristicItem(partyCharacteristicItem );
				
		
		partyCharacteristicItem = new Characteristic();
		partyCharacteristicItem.setName("EXTERNAL_TMFAPI_SERVICE_ORDER_URLS");
		partyCharacteristicItem.value( new Any(  "" ));
		oc.addPartyCharacteristicItem(partyCharacteristicItem );
		
		
		alist.add( oc );				
		return toJsonString(alist);
	}
	
	public String updateExternalSpecs( String s) throws IOException {
		ServiceSpecification spec = toJsonObj(s, ServiceSpecification.class );
		logger.info("updateExternalSpecs spec id= " + spec.getId() );
		updatedSpecs.add( spec );
		return toJsonString( spec );
		
	}

	static String toJsonString(Object object) throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
		return mapper.writeValueAsString(object);
	}

	static <T> T toJsonObj(String content, Class<T> valueType) throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
		return mapper.readValue(content, valueType);
	}

	public List<ServiceSpecification> getUpdatedSpecs() {
		return updatedSpecs;
	}

	public void setUpdatedSpecs(List<ServiceSpecification> updatedSpecs) {
		this.updatedSpecs = updatedSpecs;
	}


}

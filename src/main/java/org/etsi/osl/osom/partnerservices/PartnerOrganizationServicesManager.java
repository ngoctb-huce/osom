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

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.net.ssl.SSLException;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import org.apache.camel.ProducerTemplate;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.etsi.osl.tmf.common.model.Any;
import org.etsi.osl.tmf.pm632.model.Characteristic;
import org.etsi.osl.tmf.pm632.model.Organization;
import org.etsi.osl.tmf.scm633.model.ServiceCandidate;
import org.etsi.osl.tmf.scm633.model.ServiceCategory;
import org.etsi.osl.tmf.scm633.model.ServiceSpecCharacteristic;
import org.etsi.osl.tmf.scm633.model.ServiceSpecification;
import org.etsi.osl.tmf.sim638.model.ServiceUpdate;
import org.etsi.osl.tmf.so641.model.ServiceOrder;
import org.etsi.osl.tmf.so641.model.ServiceOrderCreate;
import org.etsi.osl.tmf.so641.model.ServiceOrderStateType;
import org.etsi.osl.tmf.so641.model.ServiceOrderUpdate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import jakarta.validation.constraints.NotNull;
import reactor.core.publisher.Mono;

/**
 * @author ctranoris
 *
 */
@Service
public class PartnerOrganizationServicesManager {

	private static final transient Log logger = LogFactory.getLog(PartnerOrganizationServicesManager.class.getName());

	@Autowired
	private ProducerTemplate template;

	@Value("${CATALOG_GET_EXTERNAL_SERVICE_PARTNERS}")
	private String CATALOG_GET_EXTERNAL_SERVICE_PARTNERS = "";


	@Value("${CATALOG_UPD_EXTERNAL_SERVICESPEC}")
	private String CATALOG_UPD_EXTERNAL_SERVICESPEC = "";
	

	@Value("${CATALOG_UPDATE_PARTNER_ORGANIZATION}")
	private String CATALOG_UPDATE_PARTNER_ORGANIZATION = "";
	
	Map<String, WebClient> webclients = new HashMap<>();

	public List<Organization> retrievePartners() {
		logger.info("will retrieve Service Providers  from catalog ");
		try {
			Map<String, Object> map = new HashMap<>();
			Object response = template.requestBodyAndHeaders(CATALOG_GET_EXTERNAL_SERVICE_PARTNERS, "", map);

			if (!(response instanceof String)) {
				logger.error("List  object is wrong.");
				return null;
			}

			Class<List<Organization>> clazz = (Class) List.class;
			List<Organization> organizations = mapJsonToObjectList(new Organization(), (String) response,
					Organization.class);
			logger.info("retrieveSPs response is: " + response);
			return organizations;

		} catch (Exception e) {
			logger.error("Cannot retrieve Listof Service Providers from catalog. " + e.toString());
		}
		return null;
	}

	protected static <T> List<T> mapJsonToObjectList(T typeDef, String json, Class clazz) throws Exception {
		List<T> list;
		ObjectMapper mapper = new ObjectMapper();
		System.out.println(json);
		TypeFactory t = TypeFactory.defaultInstance();
		list = mapper.readValue(json, t.constructCollectionType(ArrayList.class, clazz));

//	      System.out.println(list);
//	      System.out.println(list.get(0).getClass());
		return list;
	}

	public List<ServiceSpecification> fetchServiceSpecs(Organization org) {
		
		Characteristic ctype = org.findPartyCharacteristic("EXTERNAL_TMFAPI_CLIENTREGISTRATIONID");
		if ( ctype !=null ) {			
			if (ctype.getValue().getValue().contains("flowone")) {
				return fetchServiceSpecsFlowOne(org); //break here
			}			
		}
		
		
		
		logger.info("Will fetchServiceSpecs of organization: " + org.getName() + ", id: " + org.getId());

		WebClient webclient = this.getOrganizationWebClient(org);

		List<ServiceSpecification> totalSpecsList = new ArrayList<>();		
		
		/**
		 * First fetch any from catalog URLs, only if there is a URL
		 */
		try
		{			
			String url = "/tmf-api/serviceCatalogManagement/v4/serviceSpecification";			
			if ( ( org.findPartyCharacteristic("EXTERNAL_TMFAPI_SERVICE_CATALOG_URLS") != null) &&
					(org.findPartyCharacteristic("EXTERNAL_TMFAPI_SERVICE_CATALOG_URLS").getValue() != null) &&
					(!org.findPartyCharacteristic("EXTERNAL_TMFAPI_SERVICE_CATALOG_URLS").getValue().getValue().equals("") )) {
				
				url = org.findPartyCharacteristic("EXTERNAL_TMFAPI_SERVICE_CATALOG_URLS").getValue().getValue();
				logger.info("Will fetchServiceSpecs of organization: " + org.getName() + " from: " + url );		
				
				if ( webclient!=null ) {					
					List<ServiceSpecification> specsList = webclient.get()
							.uri( url )
								//.attributes( ServletOAuth2AuthorizedClientExchangeFilterFunction.clientRegistrationId("authOpensliceProvider"))
								.retrieve()
								.onStatus(HttpStatusCode::is4xxClientError, response -> {
									logger.error("4xx error");
									 webclients.remove( org.getId(), webclient );
									this.updateOrgzStatus(org, "WEBCLIENT 4xx ERROR");
							        return Mono.error(new RuntimeException("4xx"));
							      })
							      .onStatus(HttpStatusCode::is5xxServerError, response -> {
							    	  logger.error("5xx error");
									webclients.remove( org.getId(), webclient );
									this.updateOrgzStatus(org, "WEBCLIENT 5xx ERROR");
							        return Mono.error(new RuntimeException("5xx"));
							      })
							  .bodyToMono( new ParameterizedTypeReference<List<ServiceSpecification>>() {})
							  .block();
					
					totalSpecsList.addAll(specsList);
					
				} else  {
					logger.error("WebClient is null. Cannot be created.");
					this.updateOrgzStatus(org, "WEBCLIENT is null");
				}
			}
		}catch (Exception e) {
			logger.error("fetchServiceSpecs error on web client request");
			this.updateOrgzStatus(org, "WEBCLIENT fetchServiceSpecs error ");
			this.invalidateOrganizationWebClient(org);
			return new ArrayList<>();
			
		}
		
		
		
		//EXTERNAL_TMFAPI_SERVICE_CATEGORY_URLS
		/**
		 *  fetch any from category URLs
		 */
		try
		{
			
			String urls = "/tmf-api/serviceCatalogManagement/v4/serviceCategory";
			
			if ( ( org.findPartyCharacteristic("EXTERNAL_TMFAPI_SERVICE_CATEGORY_URLS") != null) &&
					(org.findPartyCharacteristic("EXTERNAL_TMFAPI_SERVICE_CATEGORY_URLS").getValue() != null) &&
					(!org.findPartyCharacteristic("EXTERNAL_TMFAPI_SERVICE_CATEGORY_URLS").getValue().getValue().equals("") )) {
				urls = org.findPartyCharacteristic("EXTERNAL_TMFAPI_SERVICE_CATEGORY_URLS").getValue().getValue();
			}
			
			String[] urlToFetch = urls.split(",");
			
						
		
			if ( webclient!=null ) {
				for (String url : urlToFetch) {
					logger.info("Will fetchServiceCandidates of organization: " + org.getName() + " from: " + url );
					
					
					ServiceCategory sc = webclient.get()
							.uri( url )
								//.attributes( ServletOAuth2AuthorizedClientExchangeFilterFunction.clientRegistrationId("authOpensliceProvider"))
								.retrieve()
								.onStatus(HttpStatusCode::is4xxClientError, response -> {
									logger.error("4xx error");
									 webclients.remove( org.getId(), webclient );
									this.updateOrgzStatus(org, "WEBCLIENT 4xx ERROR");
							        return Mono.error(new RuntimeException("4xx"));
							      })
							      .onStatus(HttpStatusCode::is5xxServerError, response -> {
							    	  logger.error("5xx error");
									webclients.remove( org.getId(), webclient );
									this.updateOrgzStatus(org, "WEBCLIENT 5xx ERROR");
							        return Mono.error(new RuntimeException("5xx"));
							      })
							  .bodyToMono( new ParameterizedTypeReference< ServiceCategory>() {})
							  .block();


					//prepei me neo fetch sot service candidate na parw to spec
					
						for (ServiceCandidate serviceCandidateRef : sc.getServiceCandidateObj()   ) {
							url = url.split("serviceCategory")[0];
							String urlCandidate = url + "serviceCandidate/" + serviceCandidateRef.getId();
							ServiceCandidate scand = webclient.get()
									.uri( urlCandidate )
										//.attributes( ServletOAuth2AuthorizedClientExchangeFilterFunction.clientRegistrationId("authOpensliceProvider"))
										.retrieve()
										.onStatus(HttpStatusCode::is4xxClientError, response -> {
											logger.error("4xx error");
											 webclients.remove( org.getId(), webclient );
											this.updateOrgzStatus(org, "WEBCLIENT 4xx ERROR");
									        return Mono.error(new RuntimeException("4xx"));
									      })
									      .onStatus(HttpStatusCode::is5xxServerError, response -> {
									    	  logger.error("5xx error");
											webclients.remove( org.getId(), webclient );
											this.updateOrgzStatus(org, "WEBCLIENT 5xx ERROR");
									        return Mono.error(new RuntimeException("5xx"));
									      })
									  .bodyToMono( new ParameterizedTypeReference< ServiceCandidate>() {})
									  .block();
							
							
							//we only care for the id, so we add this in this fake spec							
							
							if ( (scand!=null) && ( scand.getServiceSpecificationObj() != null) ) {
								totalSpecsList.add( scand.getServiceSpecificationObj() );
							}
							
						}	
					
						
					
				}
				
				
			} else  {
				logger.error("WebClient is null. Cannot be created.");
				this.updateOrgzStatus(org, "WEBCLIENT is null");
			}

		}catch (Exception e) {
			logger.error("fetchServiceSpecs error on web client request");
			this.updateOrgzStatus(org, "WEBCLIENT fetchServiceSpecs error ");
			this.invalidateOrganizationWebClient(org);
			e.printStackTrace();
			return new ArrayList<>();
		}
		
		
		/**
		 * will  fetch each spec details from API
		 */
		

		List<ServiceSpecification> fullSpecsResult = new ArrayList<>();		
		String urlfullspec = "/tmf-api/serviceCatalogManagement/v4/serviceSpecification";
		
		if ( ( org.findPartyCharacteristic("EXTERNAL_TMFAPI_SERVICE_SPEC") != null) &&
				(org.findPartyCharacteristic("EXTERNAL_TMFAPI_SERVICE_SPEC").getValue() != null) &&
				(!org.findPartyCharacteristic("EXTERNAL_TMFAPI_SERVICE_SPEC").getValue().getValue().equals(""))) {
			urlfullspec = org.findPartyCharacteristic("EXTERNAL_TMFAPI_SERVICE_SPEC").getValue().getValue();
		}
		
		for (ServiceSpecification specsrc : totalSpecsList) {
			if ( specsrc.getId()!=null ) {
				ServiceSpecification fullspec = webclient.get()
						.uri( urlfullspec + "/" + specsrc.getId() )
						.retrieve()
						.onStatus(HttpStatusCode::is4xxClientError, response -> {
							logger.error("4xx error");
							webclients.remove( org.getId(), webclient );
							this.updateOrgzStatus(org, "WEBCLIENT ServiceSpecification 4xx ERROR " + specsrc.getId() );
					        return Mono.error(new RuntimeException("4xx"));
					      })
					      .onStatus(HttpStatusCode::is5xxServerError, response -> {
					    	  logger.error("5xx error");

								webclients.remove( org.getId(), webclient );
								this.updateOrgzStatus(org, "WEBCLIENT ServiceSpecification 5xx ERROR "  + specsrc.getId() );
					        return Mono.error(new RuntimeException("5xx"));
					      })
					  .bodyToMono( new ParameterizedTypeReference< ServiceSpecification>() {})
					  .block();
				logger.info("Will add from " + org.getName() + " serviceSpecification name: " + fullspec.getName() + ", id: " + fullspec.getId());
				fullSpecsResult.add( fullspec );				
			}
		}
		

		this.updateOrgzStatus(org, "FULLSPECS SUCCESS");

		return fullSpecsResult;
	}


	private List<ServiceSpecification> fetchServiceSpecsFlowOne(Organization org) {
		logger.info("Will fetchServiceSpecsFlowOne of organization: " + org.getName() + ", id: " + org.getId());

		WebClient webclient = this.getOrganizationWebClient(org);
		List<ServiceSpecification> specs = new ArrayList<>();		
	
		try
		{		
		
			if ( webclient!=null ) {
				
				/**
				 * first fetch only id since it is a Long
				 */
				List<SimpleIDSpec> aspecsIDs = webclient.get()
						.uri( org.findPartyCharacteristic("EXTERNAL_TMFAPI_SERVICE_CATALOG_URLS").getValue().getValue()  )
							//.attributes( ServletOAuth2AuthorizedClientExchangeFilterFunction.clientRegistrationId("authOpensliceProvider"))
							.retrieve()
							.onStatus(HttpStatusCode::is4xxClientError, response -> {
								logger.error("4xx eror");
								this.updateOrgzStatus(org, "WEBCLIENT 4xx ERROR");
						        return Mono.error(new RuntimeException("4xx"));
						      })
						      .onStatus(HttpStatusCode::is5xxServerError, response -> {
						    	  logger.error("5xx eror");
									this.updateOrgzStatus(org, "WEBCLIENT 5xx ERROR");
						        return Mono.error(new RuntimeException("5xx"));
						      })
						  .bodyToMono( new ParameterizedTypeReference< List<SimpleIDSpec>>() {})
						  .block();
				
				
				/**
				 * then fetch the sp
				 */
				for (SimpleIDSpec aspecID : aspecsIDs) {
					//SimpleIDSpec aspecId = toJsonObj(aspecsID, SimpleIDSpec.class);
					String s = toJsonString( aspecID );
					ServiceSpecification aspec = aspecID;// toJsonObj(s, ServiceSpecification.class);
					aspec.setUuid( ""+ aspecID.getIntAsString() );
					
					
					for (ServiceSpecCharacteristic characts : aspec.getServiceSpecCharacteristic()) {
						characts.setConfigurable(true); //this is a hack for FlowOne
						characts.setValueType("TEXT");
//						for (ServiceSpecCharacteristicValue cval : characts.getServiceSpecCharacteristicValue()) {
//							cval.value
//						}
					}

					logger.info("Will add FlowOne serviceSpecification name: " + aspec.getName() + ", id: " + aspec.getId());
					specs.add(aspec);
				}
				
				
				
				
			} else  {
				this.updateOrgzStatus(org, "fetchServiceSpecsFlowone WebClient is null. Cannot be created");
				logger.error("WebClient is null. Cannot be created.");
			}

		}catch (Exception e) {
			logger.error("fetchServiceSpecsFlowone error on web client request");
			webclients.put(org.getId(), null);//to reset the webclient to retrieve a new one when there is a new try\
			this.updateOrgzStatus(org, "fetchServiceSpecsFlowone error on web client request");
			e.printStackTrace();
		}
		
		/**
		 * will create or fetch existing web client for this organization
		 */

		return specs;
	}
	

	private void invalidateOrganizationWebClient(Organization org) {
		if (webclients.get(org.getId()) != null) {
			webclients.remove( org.getId() );
		}
		
	}

	private WebClient getOrganizationWebClient(Organization org) {
		
//		if (webclients.get(org.getId()) != null) {
//			return webclients.get( org.getId() );
//		} else {
			

			try {
				String[] scopes = new String[0];
				String clientRegId = "";
				String aOAUTH2CLIENTID = "";
				String aOAUTHSECRET="";
				String aTOKEURI="";
				String aUSERNAME="";
				String aPASSWORD="";
				String aBASEURL="";
				
				if ( org.findPartyCharacteristic("EXTERNAL_TMFAPI_OAUTH2SCOPES")!=null ) {
					scopes = org.findPartyCharacteristic("EXTERNAL_TMFAPI_OAUTH2SCOPES").getValue().getValue().split(";");
				}
				if ( org.findPartyCharacteristic("EXTERNAL_TMFAPI_CLIENTREGISTRATIONID") !=null ) {
					clientRegId = org.findPartyCharacteristic("EXTERNAL_TMFAPI_CLIENTREGISTRATIONID").getValue().getValue();
				}
				if ( org.findPartyCharacteristic("EXTERNAL_TMFAPI_OAUTH2CLIENTID") !=null ) {
					aOAUTH2CLIENTID = org.findPartyCharacteristic("EXTERNAL_TMFAPI_OAUTH2CLIENTID").getValue().getValue();
				}
				if ( org.findPartyCharacteristic("EXTERNAL_TMFAPI_OAUTH2CLIENTSECRET") !=null ) {
					aOAUTHSECRET = org.findPartyCharacteristic("EXTERNAL_TMFAPI_OAUTH2CLIENTSECRET").getValue().getValue();
				}


				if ( org.findPartyCharacteristic("EXTERNAL_TMFAPI_OAUTH2TOKENURI") !=null ) {
					aTOKEURI = org.findPartyCharacteristic("EXTERNAL_TMFAPI_OAUTH2TOKENURI").getValue().getValue();
				}
				if ( org.findPartyCharacteristic("EXTERNAL_TMFAPI_USERNAME") !=null ) {
					aUSERNAME = org.findPartyCharacteristic("EXTERNAL_TMFAPI_USERNAME").getValue().getValue();
				}
				if ( org.findPartyCharacteristic("EXTERNAL_TMFAPI_PASSWORD") !=null ) {
					aPASSWORD = org.findPartyCharacteristic("EXTERNAL_TMFAPI_PASSWORD").getValue().getValue();
				}
				if ( org.findPartyCharacteristic("EXTERNAL_TMFAPI_BASEURL") !=null ) {
					aBASEURL = org.findPartyCharacteristic("EXTERNAL_TMFAPI_BASEURL").getValue().getValue();
				}
			
			
			
			GenericClient oac = new GenericClient(
					
					clientRegId, 
					aOAUTH2CLIENTID, 
					aOAUTHSECRET, 
					scopes, 
					aTOKEURI, 
					aUSERNAME, 
					aPASSWORD, 
					aBASEURL );
			
			WebClient webClient;
				webClient = oac.createWebClient();
				webclients.put( org.getId() , webClient);
				return webClient;
			} catch (SSLException e) {				
				
				e.printStackTrace();
				this.updateOrgzStatus(org, "WEBCLIENT ERROR SSLException");
			}
			
//		}
		return null;
	}

	private void updateOrgzStatus(Organization org, String status) {
		Characteristic partyCharacteristicItem = org.findPartyCharacteristic("EXTERNAL_TMFAPI_STATUS");
		if ( partyCharacteristicItem == null ) {
			partyCharacteristicItem = new Characteristic();
			partyCharacteristicItem.setName( "EXTERNAL_TMFAPI_STATUS" );
			partyCharacteristicItem.setValueType( "TEXT" );
			partyCharacteristicItem.setValue( new Any(""));
			org.addPartyCharacteristicItem(partyCharacteristicItem);
		}

		partyCharacteristicItem.setValue( new Any( OffsetDateTime.now(ZoneOffset.UTC) + ": " + status ));
		this.updateOrganizationInCatalog(org.getId(), org);
		
	}

	public ServiceSpecification updateSpecInLocalCatalog(String orgid, ServiceSpecification serviceSpecification) {
		logger.info("Will UpdateSpecInLocalCatalog serviceSpecification name: " + serviceSpecification.getName() + ", id: " + serviceSpecification.getId());

		try {
			Map<String, Object> map = new HashMap<>();
			map.put("servicespecid", serviceSpecification.getId() );
			map.put("orgid", orgid );
			Object response = template.requestBodyAndHeaders( CATALOG_UPD_EXTERNAL_SERVICESPEC, toJsonString( serviceSpecification ), map);

			if ( !(response instanceof String)) {
				logger.error("Service Spec object is wrong.");
			}

			ServiceSpecification serviceSpecResp = toJsonObj( (String)response, ServiceSpecification.class); 
			//logger.debug("createService response is: " + response);
			return serviceSpecResp;
			
			
		}catch (Exception e) {
			logger.error("Cannot update Service Spec : " + serviceSpecification.getId() + ": " + e.toString());
		}
		return null;

	}
	
	
	public Organization updateOrganizationInCatalog(String orgid, Organization orgz) {
		logger.info("Will updateOrganizationCatalog orgz name: " + orgz.getName() + ", id: " + orgid);

		try {
			Map<String, Object> map = new HashMap<>();
			map.put("orgid", orgid );
			Object response = template.requestBodyAndHeaders( CATALOG_UPDATE_PARTNER_ORGANIZATION, toJsonString( orgz ), map);

			if ( !(response instanceof String)) {
				logger.error("Organization object is wrong.");
			}

			Organization orgzresp = toJsonObj( (String)response, Organization.class); 
			//logger.debug("createService response is: " + response);
			return orgzresp;
			
			
		}catch (Exception e) {
			logger.error("Cannot update Organization : " + orgid + ": " + e.toString());
		}
		return null;

	}
	

	static <T> T toJsonObj(String content, Class<T> valueType) throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
		return mapper.readValue(content, valueType);
	}

	static String toJsonString(Object object) throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
		return mapper.writeValueAsString(object);
	}

	public ServiceOrder makeExternalServiceOrder(ServiceOrderCreate servOrder, Organization org, String remoteServiceSpecID) {
				
		Characteristic ctype = org.findPartyCharacteristic("EXTERNAL_TMFAPI_CLIENTREGISTRATIONID");
		if ( ctype !=null ) {			
			if (ctype.getValue().getValue().contains("flowone")) {
				return makeExternalServiceOrderFlowOne(servOrder, org, remoteServiceSpecID); //break here
			}			
		}
		
		logger.info("Will makeExternalServiceOrder to organization: " + org.getName() + ", id: " + org.getId());

		/**
		 * will create or fetch existing web client for this organization
		 */
		WebClient webclient = this.getOrganizationWebClient(org);


		//EXTERNAL_TMFAPI_SERVICE_ORDER_URLS
		String url = "/tmf-api/serviceOrdering/v4/serviceOrder";
		
		if ( ( org.findPartyCharacteristic("EXTERNAL_TMFAPI_SERVICE_ORDER_URLS") != null) &&
				(org.findPartyCharacteristic("EXTERNAL_TMFAPI_SERVICE_ORDER_URLS").getValue() != null) &&
				(!org.findPartyCharacteristic("EXTERNAL_TMFAPI_SERVICE_ORDER_URLS").getValue().getValue().equals("") )) {
			url = org.findPartyCharacteristic("EXTERNAL_TMFAPI_SERVICE_ORDER_URLS").getValue().getValue();
		}
		
		ServiceOrder sorder = new ServiceOrder();
		
		if ( webclient!=null ) {
			
			try {
			sorder = webclient.post()
					.uri(url)
				      //.header("Authorization", "Basic " + encodedClientData)
				      .bodyValue( servOrder ) 
						//.attributes( ServletOAuth2AuthorizedClientExchangeFilterFunction.clientRegistrationId("authOpensliceProvider"))
						.retrieve()
						.onStatus(HttpStatusCode::is4xxClientError, response -> {
							logger.error("4xx eror");
					        return Mono.error(new RuntimeException("4xx"));
					      })
					      .onStatus(HttpStatusCode::is5xxServerError, response -> {
					    	  logger.error("5xx eror");
					        return Mono.error(new RuntimeException("5xx"));
					      })
					  .bodyToMono( new ParameterizedTypeReference<ServiceOrder>() {})
					  .block();
		
			}catch (Exception e) {
				logger.error(" error on web client request");
				this.invalidateOrganizationWebClient(org);
				e.printStackTrace();
			}
			
			 
			 
		} else  {
			logger.error("WebClient is null. Cannot be created.");
		}

		

		return sorder;
	}

	private ServiceOrder makeExternalServiceOrderFlowOne( ServiceOrderCreate servOrderCreate, Organization org, String remoteServiceSpecID ) {
		logger.info("Will makeExternalServiceOrderFlowOne to organization: " + org.getName() + ", id: " + org.getId());

		/**
		 * will create or fetch existing web client for this organization
		 */
		WebClient webclient = this.getOrganizationWebClient(org);
		FlowOneServiceOrderCreate servOrder = new FlowOneServiceOrderCreate( servOrderCreate );
		
		
		
		String abody = "";
		try {
			abody = toJsonString( servOrder );
			logger.debug( "ServiceOrderCreate = " + abody );
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		
		ServiceOrder sorder = new ServiceOrder();
		
		
		if ( webclient!=null ) {
			
			try {
			sorder = webclient.post()
					.uri( org.findPartyCharacteristic("EXTERNAL_TMFAPI_SERVICE_ORDER_URLS").getValue().getValue()  )
				      //.header("Authorization", "Basic " + encodedClientData)
				      .bodyValue( abody ) 
						//.attributes( ServletOAuth2AuthorizedClientExchangeFilterFunction.clientRegistrationId("authOpensliceProvider"))
						.retrieve()
						.onStatus(HttpStatusCode::is4xxClientError, response -> {
							logger.error("4xx eror");
					        return Mono.error(new RuntimeException("4xx"));
					      })
					      .onStatus(HttpStatusCode::is5xxServerError, response -> {
					    	  logger.error("5xx eror");
					        return Mono.error(new RuntimeException("5xx"));
					      })
					  .bodyToMono( new ParameterizedTypeReference<ServiceOrder>() {})
					  .block();
		
			}catch (Exception e) {
				logger.error(" error on web client request");
				this.invalidateOrganizationWebClient(org);
				e.printStackTrace();
			}
			 
			 
		} else  {
			logger.error("WebClient is null. Cannot be created.");
		}

		

		return sorder;
	}

	public ServiceOrder retrieveServiceOrder(Organization org, String externalServiceOrderId) {

		Characteristic ctype = org.findPartyCharacteristic("EXTERNAL_TMFAPI_CLIENTREGISTRATIONID");
		if ( ctype !=null ) {			
			if (ctype.getValue().getValue().contains("flowone")) {
				return retrieveServiceOrderFlowOne( org, externalServiceOrderId); //break here
			}			
		}
		
		
		logger.info("Will retrieveServiceOrder from organization: " + org.getName() + ", id: " + org.getId());

		/**
		 * will create or fetch existing web client for this organization
		 */
		WebClient webclient = this.getOrganizationWebClient(org);


		ServiceOrder sorder = new ServiceOrder();
		if ( webclient!=null ) {
			try {
			
			sorder = webclient.get()
					.uri("/tmf-api/serviceOrdering/v4/serviceOrder/{id}", externalServiceOrderId)
				      //.header("Authorization", "Basic " + encodedClientData)
						//.attributes( ServletOAuth2AuthorizedClientExchangeFilterFunction.clientRegistrationId("authOpensliceProvider"))
						.retrieve()
						.onStatus(HttpStatusCode::is4xxClientError, response -> {
							logger.error("4xx eror");
					        return Mono.error(new RuntimeException("4xx"));
					      })
					      .onStatus(HttpStatusCode::is5xxServerError, response -> {
					    	  logger.error("5xx eror");
					        return Mono.error(new RuntimeException("5xx"));
					      })
					  .bodyToMono( new ParameterizedTypeReference<ServiceOrder>() {})
					  .block();
		
			}catch (Exception e) {
				logger.error(" error on web client request");
				this.invalidateOrganizationWebClient(org);
				e.printStackTrace();
			}
			 
			 
			
		} else  {
			logger.error("WebClient is null. Cannot be created.");

			this.invalidateOrganizationWebClient(org);
		}

		

		return sorder;
	}

	/**
	 * @param org
	 * @param externalServiceOrderId
	 * @return
	 */
	private ServiceOrder retrieveServiceOrderFlowOne(Organization org, String externalServiceOrderId) {
		logger.info("Will retrieveServiceOrderFlowOne from organization: " + org.getName() + ", id: " + org.getId());

		/**
		 * will create or fetch existing web client for this organization
		 */
		WebClient webclient = this.getOrganizationWebClient(org);


		ServiceOrder sorder = new ServiceOrder();
		if ( webclient!=null ) {
			
			try {
			String sorderStr = webclient.get()
					.uri(org.findPartyCharacteristic("EXTERNAL_TMFAPI_SERVICE_ORDER_URLS").getValue().getValue() + "/" + externalServiceOrderId)
				      //.header("Authorization", "Basic " + encodedClientData)
						//.attributes( ServletOAuth2AuthorizedClientExchangeFilterFunction.clientRegistrationId("authOpensliceProvider"))
						.retrieve()
						.onStatus(HttpStatusCode::is4xxClientError, response -> {
							logger.error("4xx eror");
					        return Mono.error(new RuntimeException("4xx"));
					      })
					      .onStatus(HttpStatusCode::is5xxServerError, response -> {
					    	  logger.error("5xx eror");
					        return Mono.error(new RuntimeException("5xx"));
					      })
					  .bodyToMono( new ParameterizedTypeReference<String>() {})
					  .block();
			

			
				//sorderStr = sorderStr.replace("\"note\":{", "\"note\":{\"id\":\"1\",");
				FlowOneServiceOrder flowsorder = toJsonObj(sorderStr, FlowOneServiceOrder.class);
				sorder.setUuid( flowsorder.getId() );
				if ( flowsorder.getState() != null) {
					sorder.setState( ServiceOrderStateType.fromValue( flowsorder.getState().toUpperCase() ));					
				} else {
					sorder.setState( ServiceOrderStateType.INPROGRESS );
					logger.error("FlowOneServiceOrder state is NULL");
				}
				sorder.addOrderItemItem(flowsorder.getOrderItem());
			}catch (Exception e) {
				logger.error(" error on web client request");
				this.invalidateOrganizationWebClient(org);
				e.printStackTrace();
			}
			 
			
		} else  {
			logger.error("WebClient is null. Cannot be created.");
		}

		

		return sorder;
	}

	
	public ServiceOrder updateExternalServiceOrder(String serviceId, ServiceOrderUpdate servOrder, Organization org) {
		
		Characteristic ctype = org.findPartyCharacteristic("EXTERNAL_TMFAPI_CLIENTREGISTRATIONID");
		if ( ctype !=null ) {			
			if (ctype.getValue().getValue().contains("flowone")) {
				return updateExternalServiceOrderFlowOne(serviceId, servOrder, org); //break here
			}			
		}
		
		logger.info("Will updateExternalServiceOrder to organization: " + org.getName() + ", id: " + org.getId());

		/**
		 * will create or fetch existing web client for this organization
		 */
		WebClient webclient = this.getOrganizationWebClient(org);


		//EXTERNAL_TMFAPI_SERVICE_ORDER_URLS
		String url = "/tmf-api/serviceOrdering/v4/serviceOrder/" + serviceId;
		
		if ( ( org.findPartyCharacteristic("EXTERNAL_TMFAPI_SERVICE_ORDER_URLS") != null) &&
				(org.findPartyCharacteristic("EXTERNAL_TMFAPI_SERVICE_ORDER_URLS").getValue() != null) &&
				(!org.findPartyCharacteristic("EXTERNAL_TMFAPI_SERVICE_ORDER_URLS").getValue().getValue().equals("") )) {
			url = org.findPartyCharacteristic("EXTERNAL_TMFAPI_SERVICE_ORDER_URLS").getValue().getValue();
		}
		
		ServiceOrder sorder = new ServiceOrder();
		
		if ( webclient!=null ) {
			
			try {
			sorder = webclient.patch()
					.uri(url)
				      //.header("Authorization", "Basic " + encodedClientData)
				      .bodyValue( servOrder ) 
						//.attributes( ServletOAuth2AuthorizedClientExchangeFilterFunction.clientRegistrationId("authOpensliceProvider"))
						.retrieve()
						.onStatus(HttpStatusCode::is4xxClientError, response -> {
							logger.error("4xx eror");
					        return Mono.error(new RuntimeException("4xx"));
					      })
					      .onStatus(HttpStatusCode::is5xxServerError, response -> {
					    	  logger.error("5xx eror");
					        return Mono.error(new RuntimeException("5xx"));
					      })
					  .bodyToMono( new ParameterizedTypeReference<ServiceOrder>() {})
					  .block();
			}catch (Exception e) {
				logger.error(" error on web client request");
				this.invalidateOrganizationWebClient(org);
				e.printStackTrace();
			}

			 
			 
		} else  {
			logger.error("WebClient is null. Cannot be created.");
		}

		

		return sorder;
	}
	
	
	private ServiceOrder updateExternalServiceOrderFlowOne( String serviceId, ServiceOrderUpdate servOrderUpd, Organization org ) {
		logger.info("Will makeExternalServiceOrderFlowOne to organization: " + org.getName() + ", id: " + org.getId());

		/**
		 * will create or fetch existing web client for this organization
		 */
		WebClient webclient = this.getOrganizationWebClient(org);
		FlowOneServiceOrderUpdate servOrder = new FlowOneServiceOrderUpdate( servOrderUpd );
		
		
		
		String abody = "";
		try {
			abody = toJsonString( servOrder );
			logger.debug( "updateExternalServiceOrderFlowOne = " + abody );
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		
		ServiceOrder sorder = new ServiceOrder();
		
		
		if ( webclient!=null ) {
			
			try {
			sorder = webclient.patch()
					.uri( org.findPartyCharacteristic("EXTERNAL_TMFAPI_SERVICE_ORDER_URLS").getValue().getValue()+"/"+serviceId  )
				      //.header("Authorization", "Basic " + encodedClientData)
				      .bodyValue( abody ) 
						//.attributes( ServletOAuth2AuthorizedClientExchangeFilterFunction.clientRegistrationId("authOpensliceProvider"))
						.retrieve()
						.onStatus(HttpStatusCode::is4xxClientError, response -> {
							logger.error("4xx eror");
					        return Mono.error(new RuntimeException("4xx"));
					      })
					      .onStatus(HttpStatusCode::is5xxServerError, response -> {
					    	  logger.error("5xx eror");
					        return Mono.error(new RuntimeException("5xx"));
					      })
					  .bodyToMono( new ParameterizedTypeReference<ServiceOrder>() {})
					  .block();
		
			}catch (Exception e) {
				logger.error(" error on web client request");
				this.invalidateOrganizationWebClient(org);
				e.printStackTrace();
			}
			 
			 
		} else  {
			logger.error("WebClient is null. Cannot be created.");
			this.invalidateOrganizationWebClient(org);
		}

		

		return sorder;
	}


		
	
	
	public org.etsi.osl.tmf.sim638.model.Service retrieveServiceFromInventory(@NotNull Organization org, @NotNull String externalServiceId) {
	
		logger.info("Will retrieveServiceFromInventory from organization: " + org.getName() + ", id: " + org.getId());

		for (int i = 0; i < 5; i++) { //try 5 times if something is wrong
			
			/**
			 * will create or fetch existing web client for this organization
			 */
			WebClient webclient = this.getOrganizationWebClient(org);
			
	
			org.etsi.osl.tmf.sim638.model.Service srvc = new org.etsi.osl.tmf.sim638.model.Service();
			if ( webclient!=null ) {
				
				try {
				srvc = webclient.get()
						.uri("/tmf-api/serviceInventory/v4/service/{id}", externalServiceId)
					      //.header("Authorization", "Basic " + encodedClientData)
							//.attributes( ServletOAuth2AuthorizedClientExchangeFilterFunction.clientRegistrationId("authOpensliceProvider"))
							.retrieve()
							.onStatus(HttpStatusCode::is4xxClientError, response -> {
								logger.error("4xx eror");
						        return Mono.error(new RuntimeException("4xx"));
						      })
						      .onStatus(HttpStatusCode::is5xxServerError, response -> {
						    	  logger.error("5xx eror");
						        return Mono.error(new RuntimeException("5xx"));
						      })
						  .bodyToMono( new ParameterizedTypeReference<org.etsi.osl.tmf.sim638.model.Service>() {})
						  .block();
			
	
				}catch (Exception e) {
					logger.error(" error on web client request");
					this.invalidateOrganizationWebClient(org);
					e.printStackTrace();
					try {
						Thread.sleep(3000);
					} catch (InterruptedException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				}
				 
				
			} else  {
				logger.error("WebClient is null. Cannot be created.");
				this.invalidateOrganizationWebClient(org);
				try {
					Thread.sleep(3000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
	
			return srvc;
		}
		
		return null;
		
	}

	public org.etsi.osl.tmf.sim638.model.Service updateExternalService(String externalPartnerServiceId, ServiceUpdate servUpdate, Organization org) {
		Characteristic ctype = org.findPartyCharacteristic("EXTERNAL_TMFAPI_CLIENTREGISTRATIONID");
		
		
		logger.info("Will updateExternalServiceOrder to organization: " + org.getName() + ", id: " + org.getId());

		/**
		 * will create or fetch existing web client for this organization
		 */
		WebClient webclient = this.getOrganizationWebClient(org);


		//EXTERNAL_TMFAPI_SERVICE_ORDER_URLS
		String url = "/tmf-api/serviceInventory/v4/service/" + externalPartnerServiceId;
		
		if ( ( org.findPartyCharacteristic("EXTERNAL_TMFAPI_SERVICE_INVENTORY_URLS") != null) &&
				(org.findPartyCharacteristic("EXTERNAL_TMFAPI_SERVICE_INVENTORY_URLS").getValue() != null) &&
				(!org.findPartyCharacteristic("EXTERNAL_TMFAPI_SERVICE_INVENTORY_URLS").getValue().getValue().equals("") )) {
			url = org.findPartyCharacteristic("EXTERNAL_TMFAPI_SERVICE_INVENTORY_URLS").getValue().getValue();
		}
		
		org.etsi.osl.tmf.sim638.model.Service serv = new org.etsi.osl.tmf.sim638.model.Service();
		
		if ( webclient!=null ) {
			
			try {
			serv = webclient.patch()
					.uri(url)
				      //.header("Authorization", "Basic " + encodedClientData)
				      .bodyValue( servUpdate ) 
						//.attributes( ServletOAuth2AuthorizedClientExchangeFilterFunction.clientRegistrationId("authOpensliceProvider"))
						.retrieve()
						.onStatus(HttpStatusCode::is4xxClientError, response -> {
							logger.error("4xx eror");
					        return Mono.error(new RuntimeException("4xx"));
					      })
					      .onStatus(HttpStatusCode::is5xxServerError, response -> {
					    	  logger.error("5xx eror");
					        return Mono.error(new RuntimeException("5xx"));
					      })
					  .bodyToMono( new ParameterizedTypeReference<org.etsi.osl.tmf.sim638.model.Service>() {})
					  .block();
			}catch (Exception e) {
				logger.error(" error on web client request");
				this.invalidateOrganizationWebClient(org);
				e.printStackTrace();
			}

			 
			 
		} else  {
			logger.error("WebClient is null. Cannot be created.");
		}

		

		return serv;
	}
		

}

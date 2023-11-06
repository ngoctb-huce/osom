package org.etsi.osl.osom.lcm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import javax.net.ssl.SSLException;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.etsi.osl.osom.partnerservices.GenericClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.reactive.function.client.WebClient;
import org.etsi.osl.tmf.common.model.Any;
import org.etsi.osl.tmf.common.model.EValueType;
import org.etsi.osl.tmf.common.model.service.Characteristic;
import org.etsi.osl.tmf.common.model.service.Note;
import org.etsi.osl.tmf.common.model.service.ServiceRef;
import org.etsi.osl.tmf.lcm.model.LCMRuleSpecification;
import org.etsi.osl.tmf.prm669.model.RelatedParty;
import org.etsi.osl.tmf.scm633.model.ServiceSpecRelationship;
import org.etsi.osl.tmf.sim638.model.Service;
import org.etsi.osl.tmf.so641.model.ServiceOrder;
import org.etsi.osl.tmf.so641.model.ServiceOrderCreate;
import org.etsi.osl.tmf.so641.model.ServiceOrderItemRelationship;
import org.etsi.osl.tmf.so641.model.ServiceOrderStateType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import reactor.core.publisher.Mono;

/**
 * @author ctranoris
 * 
 *
 */
public abstract class LcmBaseExecutor {

	private LCMRulesExecutorVariables vars;

	private LCMRuleSpecification lcmspec;

	private static final transient Log logger = LogFactory.getLog(LcmBaseExecutor.class.getName());

	public LCMRulesExecutorVariables run(LCMRulesExecutorVariables variables, LCMRuleSpecification lcmspec) {
		this.vars = variables;
		this.lcmspec = lcmspec;
		try {
			this.exec();
		} catch (Exception e) {
			vars.getCompileDiagnosticErrors().add(e.getLocalizedMessage());
			e.printStackTrace();
		}
		return this.vars;
	}

	/**
	 * this is overriden
	 */
	public abstract void exec();

	private void testF() {
	
	}

	


	/**
	 * @return the vars
	 */
	public LCMRulesExecutorVariables getVars() {
		return vars;
	}

	/**
	 * @param vars the vars to set
	 */
	public void setVars(LCMRulesExecutorVariables vars) {
		this.vars = vars;
	}

	/**
	 * @return the lcmspec
	 */
	public LCMRuleSpecification getLcmspec() {
		return lcmspec;
	}

	/**
	 * @param lcmspec the lcmspec to set
	 */
	public void setLcmspec(LCMRuleSpecification lcmspec) {
		this.lcmspec = lcmspec;
	}

	public String getCharValFromStringType(String charName) {
		logger.debug("getCharValFromStringType " + charName);
		Optional<Characteristic> c = getCharacteristicByName(charName);

		if (c.isPresent()) {
			String ttext = c.get().getValue().getValue();
			logger.debug("getCharValFromStringType size = " + ttext );
			logger.debug("getCharValFromStringType " + ttext);
			System.out.println("The value is : \n " + ttext);
			return c.get().getValue().getValue();
		}

		logger.debug("getCharValFromStringType NULL ");
		return null;

	}

	private Optional<Characteristic> getCharacteristicByName(String charName) {

		List<Characteristic> serviceCharacteristic;
		if (lcmspec.getLcmrulephase().equals("PRE_PROVISION") || this.vars.getService() == null) {
			serviceCharacteristic = this.vars.getServiceToCreate().getServiceCharacteristic();
		} else { // use as input the running service in all other phases!
			serviceCharacteristic = new ArrayList<>(this.vars.getService().getServiceCharacteristic());
		}

		logger.debug("getCharacteristicByName " + charName);
		if (serviceCharacteristic != null) {
			for (Characteristic c : serviceCharacteristic) {
				if (c.getName().equals(charName)) {
					if (c.getValue() != null) {
						if (c.getValue().getValue() != null) {
							return Optional.of(c);
						}
					}
				}
			}
		}
		Characteristic z = null;
		return Optional.ofNullable(z);
	}

	public void setCharValFromStringType(String charName, String newValue) {
		logger.debug("setCharValFromStringType " + charName + " = " + newValue);
		Optional<Characteristic> c = getCharacteristicByName(charName);
		
		if ( c.isPresent() ) {
			c.ifPresent(val -> val.getValue().setValue(newValue));
			copyCharacteristicToServiceToUpdate(c.get());			
		} else { //will add a new characteristic if this does not exist
			logger.debug(" setCharValFromStringType will add a new characteristic since this does not exist: " + charName + " = " + newValue);
			Characteristic newC = new Characteristic();
			newC.setName( charName );
			newC.setValue( new Any(newValue, ""));
			newC.setValueType("TEXT");
			copyCharacteristicToServiceToUpdate( newC );			
		}
		
	}

	private void copyCharacteristicToServiceToUpdate(Characteristic characteristic) {

		if ( this.vars.getServiceToUpdate() != null ) {
			this.vars.getServiceToUpdate().addServiceCharacteristicItem( characteristic );			
		}else if ( this.vars.getServiceToCreate()  != null ) {
			this.vars.getServiceToCreate().addServiceCharacteristicItem( characteristic );			
		}
		
	}

	public void setCharValNumber(String charName, int newValue) {

		logger.debug("setCharValNumber " + charName + " = " + newValue);
		Optional<Characteristic> c = getCharacteristicByName(charName);

		if ( c.isPresent() ) {
			c.ifPresent(val -> val.getValue().setValue("" + newValue));
			copyCharacteristicToServiceToUpdate(c.get());
		} else { //will add a new characteristic if this does not exist
			logger.debug(" setCharValNumber will add a new characteristic since this does not exist: " + charName + " = " + newValue);
			Characteristic newC = new Characteristic();
			newC.setName( charName );
			newC.setValue( new Any(newValue, ""));
			newC.setValueType("NUMBER");
			copyCharacteristicToServiceToUpdate( newC );			
		}
	}

	public int getCharValNumber(String charName) {
		logger.debug("getCharValNumber " + charName);
		Optional<Characteristic> c = getCharacteristicByName(charName);

		if (c.isPresent()) {
			logger.debug("getCharValNumber " + c.get().getValue().getValue());
			if (c.get().getValueType().equals(EValueType.BINARY.getValue())) {
				int i = Integer.parseInt(c.get().getValue().getValue());
				return i;
			} else if (c.get().getValueType().equals(EValueType.ENUM.getValue())) {
				int i = Integer.parseInt(c.get().getValue().getValue());
				return i;
			} else if (c.get().getValueType().equals(EValueType.INTEGER.getValue())) {
				int i = Integer.parseInt(c.get().getValue().getValue());
				return i;
			} else if (c.get().getValueType().equals(EValueType.lONGINT.getValue())) {
				int i = Integer.parseInt(c.get().getValue().getValue());
				return i;
			} else if (c.get().getValueType().equals(EValueType.SMALLINT.getValue())) {
				int i = Integer.parseInt(c.get().getValue().getValue());
				return i;
			}
		}

		logger.debug("getCharValNumber NULL ");
		return -1;
	}

	public String getCharValAsString(String charName) {
		logger.debug("getCharValAsString " + charName);
		Optional<Characteristic> c = getCharacteristicByName(charName);

		if (c.isPresent()) {
			logger.debug("getCharValAsString " + c.get().getValue().getValue());
			return c.get().getValue().getValue();
		}

		logger.debug("getCharValAsString NULL ");
		return null;

	}

	public Boolean getCharValFromBooleanType(String charName) {
		logger.debug("getCharValFromBooleanType " + charName);
		Optional<Characteristic> c = getCharacteristicByName(charName);

		if (c.isPresent() && c.get().getValue() != null) {
			logger.debug("getCharValFromBooleanType " + c.get().getValue().getValue());

			return c.get().getValue().getValue().toUpperCase().equals("TRUE");
		}

		logger.debug("getCharValFromBooleanType NULL ");
		return false;

	}

	public void setCharValFromBooleanType(String charName, boolean newValue) {

		logger.debug("setCharValFromBooleanType " + charName + " = " + newValue);
		Optional<Characteristic> c = getCharacteristicByName(charName);

		if ( c.isPresent() ) {
			c.ifPresent(val -> val.getValue().setValue("" + newValue));
			copyCharacteristicToServiceToUpdate(c.get());			
		} else { //will add a new characteristic if this does not exist
			logger.debug(" setCharacteristicOfCurrentService will add a new characteristic since this does not exist: " + charName + " = " + newValue);
			Characteristic newC = new Characteristic();
			newC.setName( charName );
			newC.setValue( new Any("" + newValue, ""));
			newC.setValueType("TEXT");
			copyCharacteristicToServiceToUpdate( newC );			
		}
	}

	/**
	 * 
	 * example setCharValNumber("Director return channel to cameras", 1);
	 * setCharValFromSetType("cirros_2vnf_nsd::Primitive::fsetup",
	 * "[{\"value\":\"1\",\"alias\":\"member_vnf_index\"},{\"value\":\"fsetup\",\"alias\":\"primitive\"},{\"value\":\"{
	 * \\\"tvg\\\": { \\\"ip\\\": \\\"\\\", \\\"channel1\\\": { \\\"mode\\\":
	 * \\\"0\\\" } } }\",\"alias\":\"confjson\"}]");
	 * 
	 * @param charName
	 * @param newValue
	 */
	public void setCharValFromSetType(String charName, String newValue) {

		logger.debug("setCharValFromBooleanType " + charName + " = " + newValue);
		Optional<Characteristic> c = getCharacteristicByName(charName);

		if ( c.isPresent() ) {
			c.ifPresent(val -> val.getValue().setValue(newValue));
			copyCharacteristicToServiceToUpdate(c.get());			
		} else { //will add a new characteristic if this does not exist
			logger.debug(" setCharacteristicOfCurrentService will add a new characteristic since this does not exist: " + charName + " = " + newValue);
			Characteristic newC = new Characteristic();
			newC.setName( charName );
			newC.setValue( new Any(newValue, ""));
			newC.setValueType("TEXT");
			copyCharacteristicToServiceToUpdate( newC );			
		}
		
	}

	public List<String> getCharValFromSetType(String charName) {
		logger.debug("getCharValFromSetType " + charName);
		Optional<Characteristic> c = getCharacteristicByName(charName);

//		if (lcmspec.getLcmrulephase().equals("PRE_PROVISION") || this.vars.getService() == null) {
//			serviceCharacteristic = this.vars.getServiceToCreate().getServiceCharacteristic();
//		} else { // use as input the running service in all other phases!
//			serviceCharacteristic = new ArrayList<>(this.vars.getService().getServiceCharacteristic());
//		}

		if (c.isPresent() && c.get().getValue() != null) {
			logger.debug("getCharValFromSetType " + c.get().getValue().getValue());

			String val = c.get().getValue().getValue();

			List<Any> as = null;
			try {
				as = toJsonObj(val, new TypeReference<List<Any>>() {
				});
				logger.debug("getCharValFromSetType " + as.toString());

				ArrayList<String> asret = new ArrayList<>();
				for (Any any : as) {
					asret.add(any.getValue());
				}
				return asret;
			} catch (IOException e) {
				 
				e.printStackTrace();
			}

			return null;
		}

		logger.debug("getCharValFromSetType NULL ");
		return null;
	}

	public boolean checkIfSetContainsValue(List<String> charValFromSetType, String value) {

		if (charValFromSetType != null) {
			for (String s : charValFromSetType) {
				if (s.equals(value))
					return true;
			}
		}

		return false;
	}

	public void logtext(String txt) {
		logger.info("From LCM Rule Log: " + txt);
	}

	public WebClient getAwebClient(String baseurl, String clientRegId, String aOAUTH2CLIENTID, String aOAUTHSECRET,
			String scopes, String aTOKEURI, String aUSERNAME, String aPASSWORD) {
		logger.info(baseurl);

		String[] aOAUTHscopes = null;
		if (scopes != null) {
			aOAUTHscopes = scopes.split(";");
		}

		GenericClient oac = new GenericClient(

				clientRegId, aOAUTH2CLIENTID, aOAUTHSECRET, aOAUTHscopes, aTOKEURI, aUSERNAME, aPASSWORD, baseurl);

		try {
			WebClient webClient;
			webClient = oac.createWebClient();
			return webClient;
		} catch (SSLException e) {
			logger.error(e.getLocalizedMessage());
			e.printStackTrace();
		}
		return null;
	}

	public String rest_block(String verb, String eurl, String headers, String apayload) {
		return rest_block(verb, eurl, headers, apayload, null, null, null, null, null, null, null);

	}

	public String rest_block(String verb, String eurl, String headers, String apayload, String baseurl,
			String aOAUTH2CLIENTID, String aOAUTHSECRET, String scopes, String aTOKEURI, String aUSERNAME,
			String aPASSWORD) {

		logger.debug(String.format(
				"verb: %s\n eurl: %s\n headers: %s\n apayload: %s\n baseurl: %s\n aOAUTH2CLIENTID: %s\n aOAUTHSECRET: %s\n scopes: %s\n aTOKEURI: %s\n aUSERNAME: %s\n ",
				verb, eurl, headers, apayload, baseurl, aOAUTH2CLIENTID, aOAUTHSECRET, scopes, aTOKEURI, aUSERNAME,
				aPASSWORD));
		

		System.out.println("============================================================================= \n");
		System.out.println("The value length is apayload= \n" + apayload.length());
		System.out.println("The value is apayload= \n" + apayload);
		System.out.println("============================================================================= \n");

		if (baseurl != null) {
			eurl = eurl.replace(baseurl, ""); // remove the baseurl if present
		}

		Consumer<HttpHeaders> httpHeaders = (t) -> {
			if (headers != null) {
				String[] hs = headers.split(";");
				for (String headervals : hs) {
					String[] ah = headervals.split("=");
					t.add(ah[0], ah[1]);
				}
			}

		};

		if (verb.equals("GET")) {
			return rest_block_GET(eurl, httpHeaders, apayload, baseurl, aOAUTH2CLIENTID, aOAUTHSECRET, scopes, aTOKEURI,
					aUSERNAME, aPASSWORD);
		} else if (verb.equals("POST")) {
			return rest_block_POST(eurl, httpHeaders, apayload, baseurl, aOAUTH2CLIENTID, aOAUTHSECRET, scopes,
					aTOKEURI, aUSERNAME, aPASSWORD);
		} else if (verb.equals("PUT")) {
			return rest_block_PUT(eurl, httpHeaders, apayload, baseurl, aOAUTH2CLIENTID, aOAUTHSECRET, scopes, aTOKEURI,
					aUSERNAME, aPASSWORD);
		} else if (verb.equals("PATCH")) {
			return rest_block_PATCH(eurl, httpHeaders, apayload, baseurl, aOAUTH2CLIENTID, aOAUTHSECRET, scopes,
					aTOKEURI, aUSERNAME, aPASSWORD);
		} else if (verb.equals("DELETE")) {
			return rest_block_DELETE(eurl, httpHeaders, apayload, baseurl, aOAUTH2CLIENTID, aOAUTHSECRET, scopes,
					aTOKEURI, aUSERNAME, aPASSWORD);
		}

		return null;
	}

	public String rest_block_GET(String eurl, Consumer<HttpHeaders> httpHeaders, String apayload, String baseurl,
			String aOAUTH2CLIENTID, String aOAUTHSECRET, String scopes, String aTOKEURI, String aUSERNAME,
			String aPASSWORD) {

		String clientRegId = "lcmBaseExecutor_WebClient";
		WebClient webclient = getAwebClient(baseurl, clientRegId, aOAUTH2CLIENTID, aOAUTHSECRET, scopes, aTOKEURI,
				aUSERNAME, aPASSWORD);
		String aresponse = null;
		if (webclient != null) {

			try {
				aresponse = webclient.get().uri(eurl).headers(httpHeaders).retrieve()
						.onStatus( HttpStatusCode::is4xxClientError , response -> {
							logger.error("4xx eror");
							return Mono.error(new RuntimeException("4xx"));
						}).onStatus(HttpStatusCode::is5xxServerError, response -> {
							logger.error("5xx eror");
							return Mono.error(new RuntimeException("5xx"));
						}).bodyToMono(new ParameterizedTypeReference<String>() {
						}).block();

			} catch (Exception e) {
				logger.error(" error on web client request for " + eurl);
				e.printStackTrace();
				try {
					Thread.sleep(3000);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
			}

		} else {
			logger.error("WebClient is null. Cannot be created for " + eurl);

			try {
				Thread.sleep(3000);
			} catch (InterruptedException e) {
				 
				e.printStackTrace();
			}
		}

		return aresponse;
	}

	public String rest_block_POST(String eurl, Consumer<HttpHeaders> httpHeaders, String apayload, String baseurl,
			String aOAUTH2CLIENTID, String aOAUTHSECRET, String scopes, String aTOKEURI, String aUSERNAME,
			String aPASSWORD) {
		String clientRegId = "lcmBaseExecutor_WebClient";
		WebClient webclient = getAwebClient(baseurl, clientRegId, aOAUTH2CLIENTID, aOAUTHSECRET, scopes, aTOKEURI,
				aUSERNAME, aPASSWORD);
		String aresponse = null;
		if (webclient != null) {

			try {
				aresponse = webclient.post().uri(eurl).headers(httpHeaders).bodyValue(apayload).retrieve()
						.onStatus(HttpStatusCode::is4xxClientError, response -> {
							logger.error("4xx eror");
							return Mono.error(new RuntimeException("4xx"));
						}).onStatus(HttpStatusCode::is5xxServerError, response -> {
							logger.error("5xx eror");
							return Mono.error(new RuntimeException("5xx"));
						}).bodyToMono(new ParameterizedTypeReference<String>() {
						}).block();

			} catch (Exception e) {
				logger.error(" error on web client request for " + eurl);
				e.printStackTrace();
				try {
					Thread.sleep(3000);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
			}

		} else {
			logger.error("WebClient is null. Cannot be created for " + eurl);

			try {
				Thread.sleep(3000);
			} catch (InterruptedException e) {
				 
				e.printStackTrace();
			}
		}

		return aresponse;
	}

	public String rest_block_PUT(String eurl, Consumer<HttpHeaders> httpHeaders, String apayload, String baseurl,
			String aOAUTH2CLIENTID, String aOAUTHSECRET, String scopes, String aTOKEURI, String aUSERNAME,
			String aPASSWORD) {
		String clientRegId = "lcmBaseExecutor_WebClient";
		WebClient webclient = getAwebClient(baseurl, clientRegId, aOAUTH2CLIENTID, aOAUTHSECRET, scopes, aTOKEURI,
				aUSERNAME, aPASSWORD);
		String aresponse = null;
		if (webclient != null) {

			try {
				aresponse = webclient.put().uri(eurl).headers(httpHeaders).bodyValue(apayload).retrieve()
						.onStatus(HttpStatusCode::is4xxClientError, response -> {
							logger.error("4xx eror");
							return Mono.error(new RuntimeException("4xx"));
						}).onStatus(HttpStatusCode::is5xxServerError, response -> {
							logger.error("5xx eror");
							return Mono.error(new RuntimeException("5xx"));
						}).bodyToMono(new ParameterizedTypeReference<String>() {
						}).block();

			} catch (Exception e) {
				logger.error(" error on web client request for " + eurl);
				e.printStackTrace();
				try {
					Thread.sleep(3000);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
			}

		} else {
			logger.error("WebClient is null. Cannot be created for " + eurl);

			try {
				Thread.sleep(3000);
			} catch (InterruptedException e) {
				 
				e.printStackTrace();
			}
		}

		return aresponse;
	}

	public String rest_block_PATCH(String eurl, Consumer<HttpHeaders> httpHeaders, String apayload, String baseurl,
			String aOAUTH2CLIENTID, String aOAUTHSECRET, String scopes, String aTOKEURI, String aUSERNAME,
			String aPASSWORD) {
		String clientRegId = "lcmBaseExecutor_WebClient";
		WebClient webclient = getAwebClient(baseurl, clientRegId, aOAUTH2CLIENTID, aOAUTHSECRET, scopes, aTOKEURI,
				aUSERNAME, aPASSWORD);
		String aresponse = null;
		if (webclient != null) {

			try {
				aresponse = webclient.patch().uri(eurl).headers(httpHeaders).bodyValue(apayload).retrieve()
						.onStatus(HttpStatusCode::is4xxClientError, response -> {
							logger.error("4xx eror");
							return Mono.error(new RuntimeException("4xx"));
						}).onStatus(HttpStatusCode::is5xxServerError, response -> {
							logger.error("5xx eror");
							return Mono.error(new RuntimeException("5xx"));
						}).bodyToMono(new ParameterizedTypeReference<String>() {
						}).block();

			} catch (Exception e) {
				logger.error(" error on web client request for " + eurl);
				e.printStackTrace();
				try {
					Thread.sleep(3000);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
			}

		} else {
			logger.error("WebClient is null. Cannot be created for " + eurl);

			try {
				Thread.sleep(3000);
			} catch (InterruptedException e) {
				 
				e.printStackTrace();
			}
		}

		return aresponse;
	}

	public String rest_block_DELETE(String eurl, Consumer<HttpHeaders> httpHeaders, String apayload, String baseurl,
			String aOAUTH2CLIENTID, String aOAUTHSECRET, String scopes, String aTOKEURI, String aUSERNAME,
			String aPASSWORD) {
		String clientRegId = "lcmBaseExecutor_WebClient";
		WebClient webclient = getAwebClient(baseurl, clientRegId, aOAUTH2CLIENTID, aOAUTHSECRET, scopes, aTOKEURI,
				aUSERNAME, aPASSWORD);
		String aresponse = null;
		if (webclient != null) {

			try {
				aresponse = webclient.delete().uri(eurl).headers(httpHeaders).retrieve()
						.onStatus( HttpStatusCode::is4xxClientError, response -> {
							logger.error("4xx eror");
							return Mono.error(new RuntimeException("4xx"));
						}).onStatus( HttpStatusCode::is5xxServerError, response -> {
							logger.error("5xx eror");
							return Mono.error(new RuntimeException("5xx"));
						}).bodyToMono(new ParameterizedTypeReference<String>() {
						}).block();

			} catch (Exception e) {
				logger.error(" error on web client request for " + eurl);
				e.printStackTrace();
				try {
					Thread.sleep(3000);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
			}

		} else {
			logger.error("WebClient is null. Cannot be created for " + eurl);

			try {
				Thread.sleep(3000);
			} catch (InterruptedException e) {
				 
				e.printStackTrace();
			}
		}

		return aresponse;
	}

	public String getCurrentServiceOrderPropValue(String propertyName, String... props) {

		logger.debug("getCurrentServiceOrderPropValue propertyName=" + propertyName);
		ServiceOrder serviceOrder = this.vars.getSorder();
		if (serviceOrder == null) {
			return null;
		}

		switch (propertyName) {
		case "state":
			return serviceOrder.getState().name();
		case "id":
			return serviceOrder.getId();
		case "externaId":
			return serviceOrder.getExternalId();
		case "serviceOrderObjectasJSON":
			try {
				return toJsonString(serviceOrder);
			} catch (IOException e) {
				logger.error(e.getLocalizedMessage());
				e.printStackTrace();
			}
		default:
			break;
		}

		return "";
	}

	public String getCurrentServicePropValue(String propertyName, String... props) {

		logger.debug("getCurrentServicePropValue propertyName=" + propertyName);
		Service service = this.vars.getService();
		if (service == null) {
			return null;
		}

		return getServicePropValue(service, propertyName, props);
	}

	public String getFromPayloadServicePropValue(String jsonpayload, String propertyName, String... props) {

		logger.debug("getFromPayloadServicePropValue propertyName=" + propertyName);

		Service service = null;
		try {
			service = toJsonObj(jsonpayload, new TypeReference<Service>() {
			});
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		if (service == null) {
			return null;
		}

		return getServicePropValue(service, propertyName, props);
	}

	private String getServicePropValue(Service service, String propertyName, String... props) {
		switch (propertyName) {
		case "state":
			return service.getState().name();
		case "name":
			return service.getName();
		case "hasStarted":
			return service.isHasStarted().toString();
		case "isServiceEnabled":
			return service.isIsServiceEnabled().toString();
		case "serviceType":
			return service.getServiceType();
		case "startMode":
			return service.getStartMode();
		case "serviceCharacteristicValue":
			if (props != null && props.length > 0) {
				Characteristic cp = service.getServiceCharacteristicByName(props[0]);
				logger.debug("getFromPayloadServicePropValue propertyName=" + propertyName + ',' + props[0]);
				if (cp != null && cp.getValue() != null) {
					return cp.getValue().getValue();
				}
			}
			break;
		case "serviceOrderID":
			if (service.getServiceOrder() != null) {
				return service.getServiceOrder().stream().findFirst().get().getId();
			}
			break;
		case "serviceSpecificationID":
			if (service.getServiceSpecificationRef() != null) {
				return service.getServiceSpecificationRef().getId();
			}
			break;
		case "serviceObjectasJSON":
			try {
				return toJsonString(service);
			} catch (IOException e) {
				logger.error(e.getLocalizedMessage());
				e.printStackTrace();
			}
		default:
			break;
		}

		return "";
	}


	public String getServiceRefPropValue(String serviceName,  String propertyName, String... props) {
		 
		logger.debug("getServiceRefPropValue propertyName=" + propertyName);
		Service ctxService = this.vars.getService();
		if (ctxService == null) {
			return "";
		}
		
		@NotNull @Valid ServiceRef refSrvice = null;
		
		for (ServiceRef sr : ctxService.getSupportingService()) {
			if ( sr.getName().equals(serviceName) ) {
				refSrvice = sr;
			}
		}

		if (refSrvice == null) {
			return "";
		}

		if (this.vars.getServiceOrderManager() != null) {
			Service aService = this.vars.getServiceOrderManager().retrieveService( refSrvice.getId() );	
			if ( aService!= null) {
				return  getServicePropValue(aService, propertyName, props);				
			}			
		}
		

		return "";
	}



	//createServiceRefIf("Bundle B", getServiceRefPropValue("BundleA", "state", "").equals("active")==true);
	public boolean createServiceRefIf(String serviceName, boolean b) {

		logger.debug( String.format("createServiceRefwhen serviceName=%s = %s", serviceName, b ) );
		

		String serviceIDToCheckDependcy=null;
		for (ServiceSpecRelationship specRels :  this.vars.getSpec().getServiceSpecRelationship()) {
			if ( specRels.getName().equals(serviceName) ) {
				serviceIDToCheckDependcy = specRels.getId();
			}
		}
		
		
		if (serviceIDToCheckDependcy != null) {		
			this.vars.getOutParams().put( serviceIDToCheckDependcy, Boolean.toString(b) );
		}
		
		return false;
	}
	
	
	public String createServiceOrder(String sorderJson) {

		logger.debug("createServiceOrder sorderJson=" + sorderJson);
		ServiceOrder contextServiceOrder = this.vars.getSorder();
		try {
			ServiceOrderCreate sonew = toJsonObj(sorderJson, new TypeReference<ServiceOrderCreate>() {
			});

			sonew.setRequestedStartDate(contextServiceOrder.getRequestedStartDate());
			sonew.setRequestedCompletionDate(contextServiceOrder.getRequestedCompletionDate());
			sonew.getOrderItem().stream().findFirst().get().setState(ServiceOrderStateType.ACKNOWLEDGED); // will be
																											// processed
																											// immediately
																											// by OSOM

			if (this.vars.getSoItem() != null) {
				ServiceOrderItemRelationship soitemrel = new ServiceOrderItemRelationship();
				;
				soitemrel.setId(this.vars.getSoItem().getId());
				soitemrel.setRelationshipType("DEPENDENCY");
				sonew.getOrderItem().stream().findFirst().get().addOrderItemRelationshipItem(soitemrel);
			}

			if (sonew.getRelatedParty() == null) {
				RelatedParty rp = new RelatedParty();
				rp.setName("OSOM LCM");
				rp.setRole("REQUESTER");
				sonew.addRelatedPartyItem(rp);
			}

			if (sonew.getNote() == null) {
				Note n = new Note();
				if (this.vars.getSorder() != null) {
					n.setText(String.format("Order created by LCM rule, %s, of orderid = %s", this.lcmspec.getName(),
							this.vars.getSorder().getId()));
				} else {
					n.setText(String.format("Order created by LCM rule, %s", this.lcmspec.getName()));
				}
				sonew.addNoteItem(n);
			}

			if (this.vars.getServiceOrderManager() != null) {
				ServiceOrder scorder = this.vars.getServiceOrderManager().createServiceOrder(sonew);
				return toJsonString(scorder);
			} else {

				logger.error("createServiceOrder serviceOrderManager is NULL!");
			}

		} catch (IOException e) {
			logger.error("createServiceOrder error=" + e.getLocalizedMessage());
			e.printStackTrace();
		}

		return null;
	}

	public void setCharacteristicOfCurrentService(String charName, String newValue) {
		logger.debug("setCharacteristicOfCurrentService " + charName + " = " + newValue);
		Optional<Characteristic> c = getCharacteristicByName(charName);

		if ( c.isPresent() ) {
			c.ifPresent(val -> val.getValue().setValue(newValue));
			copyCharacteristicToServiceToUpdate(c.get());			
		} else { //will add a new characteristic if this does not exist
			logger.debug(" setCharacteristicOfCurrentService will add a new characteristic since this does not exist: " + charName + " = " + newValue);
			Characteristic newC = new Characteristic();
			newC.setName( charName );
			newC.setValue( new Any(newValue, ""));
			newC.setValueType("TEXT");
			copyCharacteristicToServiceToUpdate( newC );			
		}
		
	}
	
	public String getJsonValueAsStringFromField(String jsonval, String fieldName) {
		//logger.debug("getJsonValueAsStringFromField " + fieldName + " .  jsonval=" + jsonval);
		ObjectMapper mapper = new ObjectMapper();
		try {
			JsonNode rootNode = mapper.readTree( jsonval );
			JsonNode ret = rootNode.get(fieldName);
			if ( ret !=null &&  ret.isValueNode()) {
				return ret.asText("");
			}			
			return ret.toPrettyString();
		} catch (JsonProcessingException e) {

			logger.error("getJsonValueAsStringFromField failed! " + fieldName + " .  jsonval=" + jsonval);
			e.printStackTrace();
		}
		return "";
	}

	public String getElementInJsonArrayFromIndex(String jsonval, int index) {
		ObjectMapper mapper = new ObjectMapper();
		try {
			JsonNode rootNode = mapper.readTree( jsonval );

			if (rootNode.isArray()  ) {
				ArrayNode jarr = (ArrayNode)rootNode;
				if (index<jarr.size()) {
					JsonNode jresnode = jarr.get(index);
					return jresnode.toString();
				}
			} else if (rootNode.isValueNode()) {
				return rootNode.asText("");
			}
			return rootNode.toPrettyString();
		} catch (JsonProcessingException e) {

			logger.error("getElementInJsonArrayFromIndex failed! index=" + index + " .  jsonval=" + jsonval);
			e.printStackTrace();
		}
		return "";
	}
	
	public String getElementInJsonArrayFromFieldValue(String jsonval, String fieldName, String value) {
		ObjectMapper mapper = new ObjectMapper();
		try {
			JsonNode rootNode = mapper.readTree( jsonval );

			if (rootNode.isArray()  ) {
				ArrayNode jarr = (ArrayNode)rootNode;
				for (JsonNode jsonNode : jarr) {
					if (jsonNode.get(fieldName) != null) {						
						if  (jsonNode.get(fieldName).isValueNode() ){				
							if (jsonNode.get(fieldName).asText().equals(value)) {
								return jsonNode.toString(); //object found in array
							}							
						}
						
					}
					
				}
			} else if (rootNode.isValueNode()) {
				return rootNode.asText("");
			}
			return rootNode.toPrettyString();
		} catch (JsonProcessingException e) {

			logger.error("getElementInJsonArrayFromFieldValue failed! fieldName=" + fieldName + " .  jsonval=" + jsonval);
			e.printStackTrace();
		}
		return "";
	}
	
	
	public String getValueFromJsonPath(String jsonval, String jsonpath) {
		
		try {
			Configuration conf = Configuration.defaultConfiguration();
			conf = conf.addOptions(Option.ALWAYS_RETURN_LIST);
			conf = conf.addOptions(Option.SUPPRESS_EXCEPTIONS );
			
			List<Object>  value = JsonPath.using( conf ).parse( jsonval ).read(jsonpath );
			
			if ( value == null ) {
				return "";
			}else if ( value.size() == 1 ) {
				return value.get(0).toString() ;				
			} else if ( value.size()>1 ) {
				return value.toString()  ;				
			}else {
				return "";
			}
			
		}catch (Exception e) {

			logger.error("getValueFromJsonPath failed! jsonpath=" + jsonpath + " .  jsonval=" + jsonval);
			e.printStackTrace();
		}
		
		return "";
	}

	static <T> T toJsonObj(String content, TypeReference<T> typeReference) throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
		return mapper.readValue(content, typeReference);
	}

	static String toJsonString(Object object) throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
		return mapper.writeValueAsString(object);
	}
}

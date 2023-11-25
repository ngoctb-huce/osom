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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import javax.net.ssl.SSLException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizationContext;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import io.netty.channel.ChannelOption;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import jakarta.servlet.http.HttpServletRequest;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.tcp.TcpClient;

//@Configuration
/**
 * @author ctranoris
 *
 * Since we need multiple clients to be created, we don;t use spring configuration, but they are created on demand
 * We implement here a Servlet integration
 * see: https://github.com/spring-projects/spring-security/blob/master/docs/manual/src/docs/asciidoc/_includes/servlet/oauth2/oauth2-client.adoc#oauth2Client-client-creds-grant
 */
public class GenericClient  {

	private static final transient Log log = LogFactory.getLog(GenericClient.class.getName());
	private String username;
	private String password;
	
	
	private String baseUrl;
	private String oauth2TokenURI;
	private String webClientRegistrationId;
	private String oauth2ClientId;
	private String oauth2ClientSecret;
	private String[] oauth2Scopes;
	private AuthorizationGrantType authorizationGrantType;
	
	/**
	 * Note: the constructor might change to support the instantiation of multiple clientRegistrations
	 *
	 * @param clientRegistrationId
	 * @param oauth2ClientId
	 * @param oauth2ClientSecret
	 * @param oauth2Scopes
	 * @param oauth2TokenURI
	 * @param username
	 * @param password
	 * @param baseUrl
	 */
	public GenericClient(
			String clientRegistrationId, 
			String oauth2ClientId, 
			String oauth2ClientSecret, 
			String[] oauth2Scopes, 
			String oauth2TokenURI,
			String username, 
			String password, 
			String baseUrl) {
		super();

		this.webClientRegistrationId = clientRegistrationId;

		this.oauth2ClientId = oauth2ClientId;
		this.oauth2ClientSecret = oauth2ClientSecret;
		this.oauth2Scopes = oauth2Scopes;
		this.oauth2TokenURI = oauth2TokenURI;		
		
		this.username = username;
		this.password = password;
		this.baseUrl = baseUrl;

		
		this.authorizationGrantType = AuthorizationGrantType.PASSWORD;
	}

	public WebClient createWebClient() throws SSLException{
			
		if ( oauth2ClientId != null ) {

			InMemoryClientRegistrationRepository clientRegistrations = (InMemoryClientRegistrationRepository) this.clientRegistrations() ;
			OAuth2AuthorizedClientService clientService = new InMemoryOAuth2AuthorizedClientService(clientRegistrations);
			OAuth2AuthorizedClientManager authorizedClientManager = this.authorizedClientManager(clientRegistrations, clientService);

			
			ServletOAuth2AuthorizedClientExchangeFilterFunction servletOAuth2AuthorizedClientExchangeFilterFunction =
					this.servletOAuth2AuthorizedClientExchangeFilterFunction(
							clientRegistrations,
							authorizedClientManager);
			
			ClientHttpConnector clientHttpConnector =  this.clientHttpConnector() ;
			
			return webClient(servletOAuth2AuthorizedClientExchangeFilterFunction, clientHttpConnector);		
			
		}else {

			ClientHttpConnector clientHttpConnector =  this.clientHttpConnector() ;
			return webClient( null, clientHttpConnector);	
		}
	}
	
	
	private WebClient webClient(
			ServletOAuth2AuthorizedClientExchangeFilterFunction servletOAuth2AuthorizedClientExchangeFilterFunction,
			ClientHttpConnector clientHttpConnector) {

		if ( servletOAuth2AuthorizedClientExchangeFilterFunction != null ) {
			return WebClient.builder()
		        	 .exchangeStrategies( getExchangeStrategies() )
					.baseUrl( this.baseUrl )
					.clientConnector(clientHttpConnector)
					.defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
					.defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
					.apply(servletOAuth2AuthorizedClientExchangeFilterFunction.oauth2Configuration())
					.filter(logRequest())
					.filter(logResponse())
					.build();
		}else 
			return WebClient.builder()
	        	 .exchangeStrategies( getExchangeStrategies() )
				.baseUrl( this.baseUrl )
				.clientConnector(clientHttpConnector)
				.filter(logRequest())
				.filter(logResponse())
				.build();
	}
	
	private ExchangeStrategies getExchangeStrategies() {

		ExchangeStrategies exchangeStrategies = ExchangeStrategies.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(-1)).build(); //spring.codec.max-in-memory-size=-1 ?? if use autoconfiguration
		return exchangeStrategies;
	}
	

	private ExchangeFilterFunction logRequest() {
		return (clientRequest, next) -> {
			log.debug("Request: " + clientRequest.method() + ", " + clientRequest.url());
			clientRequest.headers()
					.forEach((name, values) -> values.forEach(value -> log.debug("{" + name + "}={" + value + "}")));

			return next.exchange(clientRequest);
		};
	}
	
	private ExchangeFilterFunction logResponse() {
	    return ExchangeFilterFunction.ofResponseProcessor(clientResponse -> {
	      log.debug("Response: " + clientResponse.statusCode());
	      clientResponse.headers().asHttpHeaders()
			.forEach((name, values) -> values.forEach(value -> log.debug("{" + name + "}={" + value + "}")));
	      return Mono.just(clientResponse);
	    });
	  }

	//@Bean
	public ClientRegistrationRepository clientRegistrations() {

		log.info("WebClientConfiguration.clientRegistrations()");

		ClientRegistration clientRegistration;
		
		if ( oauth2ClientId != null) {
			clientRegistration = ClientRegistration
					.withRegistrationId( webClientRegistrationId ) //"authOpensliceProvider"
					.clientId( oauth2ClientId ) //"osapiWebClientId"
					.clientSecret( oauth2ClientSecret ) //"secret"
					.scope( oauth2Scopes ) //"admin"
					.authorizationGrantType( authorizationGrantType) //AuthorizationGrantType.PASSWORD
					.tokenUri( oauth2TokenURI )//"http://portal.openslice.io/osapi-oauth-server/oauth/token"
					.build();
			
		} else {
			clientRegistration = ClientRegistration
					.withRegistrationId( webClientRegistrationId ) //"authOpensliceProvider"
					.authorizationGrantType( authorizationGrantType) //AuthorizationGrantType.PASSWORD
					.build();			
		}
		
		
		return new InMemoryClientRegistrationRepository(clientRegistration);
	}

	  //@Bean
	public ServletOAuth2AuthorizedClientExchangeFilterFunction servletOAuth2AuthorizedClientExchangeFilterFunction(
			ClientRegistrationRepository clientRegistrations,
			
			OAuth2AuthorizedClientManager authorizedClientManager) {

		ServletOAuth2AuthorizedClientExchangeFilterFunction oauth = new ServletOAuth2AuthorizedClientExchangeFilterFunction(
				authorizedClientManager);

		// oauth.setDefaultOAuth2AuthorizedClient(true);
		//oauth.setDefaultClientRegistrationId("authOpensliceProvider");
		oauth.setDefaultClientRegistrationId( this.webClientRegistrationId );
		
		// oauth.setAccessTokenExpiresSkew(Duration.ofSeconds(30));

		return oauth;
	}

	    //@Bean
	public ClientHttpConnector clientHttpConnector() throws SSLException {

		log.info("WebClientConfiguration.clientHttpConnector()");
		
		SslContext sslContext = SslContextBuilder
				.forClient()
				.trustManager()
				.trustManager(InsecureTrustManagerFactory.INSTANCE)
				.build();

		TcpClient tcpClient = TcpClient.create()
				//.wiretap(true) //logging on reactor.netty.tcp.TcpClient level to DEBUG 
				.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 30000)
				.doOnConnected(connection -> connection.addHandlerLast
						(new ReadTimeoutHandler(30)).addHandlerLast(new WriteTimeoutHandler(30))
						);

		return new ReactorClientHttpConnector(
   			 HttpClient
   			 .from(tcpClient)
   			 .wiretap(true)//To enable it, you must set the logger reactor.netty.http.client.HttpClient level to DEBUG 
   			 .secure( sslContextSpec -> sslContextSpec.sslContext(sslContext) )
   			 );
	}

	    //@Bean
	public OAuth2AuthorizedClientManager authorizedClientManager(
			ClientRegistrationRepository clientRegistrationRepository, 
			OAuth2AuthorizedClientService clientService) {

		OAuth2AuthorizedClientProvider authorizedClientProvider = OAuth2AuthorizedClientProviderBuilder.builder()
				// .clientCredentials()
				.password().refreshToken().build();

		AuthorizedClientServiceOAuth2AuthorizedClientManager authorizedClientManager = 
				new AuthorizedClientServiceOAuth2AuthorizedClientManager(
				clientRegistrationRepository, clientService);
		authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider);
		authorizedClientManager.setContextAttributesMapper(contextAttributesMapper());
		return authorizedClientManager;
	}

	private Function<OAuth2AuthorizeRequest, Map<String, Object>> contextAttributesMapper() {
		return authorizeRequest -> {
			Map<String, Object> contextAttributes = Collections.emptyMap();
			HttpServletRequest servletRequest = authorizeRequest.getAttribute(HttpServletRequest.class.getName());
			// String username = "admin";//
			// servletRequest.getParameter(OAuth2ParameterNames.USERNAME);
			// String password =
			// "openslice";//servletRequest.getParameter(OAuth2ParameterNames.PASSWORD);
			if (StringUtils.hasText(username) && StringUtils.hasText(password)) {
				contextAttributes = new HashMap<>();

				// `PasswordOAuth2AuthorizedClientProvider` requires both attributes
				contextAttributes.put(OAuth2AuthorizationContext.USERNAME_ATTRIBUTE_NAME, this.username);
				contextAttributes.put(OAuth2AuthorizationContext.PASSWORD_ATTRIBUTE_NAME, this.password);
			}
			return contextAttributes;
		};
	}

	/**
	 * @return the username
	 */
	public String getUsername() {
		return username;
	}

	/**
	 * @param username the username to set
	 */
	public void setUsername(String username) {
		this.username = username;
	}

	/**
	 * @return the password
	 */
	public String getPassword() {
		return password;
	}

	/**
	 * @param password the password to set
	 */
	public void setPassword(String password) {
		this.password = password;
	}

	/**
	 * @return the oauth2TokenURI
	 */
	public String getOauth2TokenURI() {
		return oauth2TokenURI;
	}

	/**
	 * @param oauth2TokenURI the oauth2TokenURI to set
	 */
	public void setOauth2TokenURI(String oauth2TokenURI) {
		this.oauth2TokenURI = oauth2TokenURI;
	}

	/**
	 * @return the webClientRegistrationId
	 */
	public String getWebClientRegistrationId() {
		return webClientRegistrationId;
	}

	/**
	 * @param webClientRegistrationId the webClientRegistrationId to set
	 */
	public void setWebClientRegistrationId(String webClientRegistrationId) {
		this.webClientRegistrationId = webClientRegistrationId;
	}

	/**
	 * @return the oauth2ClientId
	 */
	public String getOauth2ClientId() {
		return oauth2ClientId;
	}

	/**
	 * @param oauth2ClientId the oauth2ClientId to set
	 */
	public void setOauth2ClientId(String oauth2ClientId) {
		this.oauth2ClientId = oauth2ClientId;
	}

	/**
	 * @return the oauth2ClientSecret
	 */
	public String getOauth2ClientSecret() {
		return oauth2ClientSecret;
	}

	/**
	 * @param oauth2ClientSecret the oauth2ClientSecret to set
	 */
	public void setOauth2ClientSecret(String oauth2ClientSecret) {
		this.oauth2ClientSecret = oauth2ClientSecret;
	}

	

	
	
}

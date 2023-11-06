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
package org.etsi.osl.osom;

import java.util.List;

import javax.net.ssl.SSLException;
import org.etsi.osl.osom.partnerservices.GenericClient;
import org.flowable.spring.impl.test.FlowableSpringExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import org.etsi.osl.tmf.scm633.model.ServiceSpecification;
import reactor.core.publisher.Mono;


public class ExternalSImportClientFlowone {


	private static final transient Logger log = LoggerFactory.getLogger( ExternalSImportClientFlowone.class.getName());




	public static void main(String[] args) throws Exception {
	
		GenericClient oac = new GenericClient(
				
				"aflowone", 
				"flowone", 
				"", 
				new String[0], 
				"http://10.0.96.9:58281/oauth/token", 
				"test3", 
				"password", 
				"http://10.0.96.9:58281" );
		
		WebClient webClient;
			webClient = oac.createWebClient();

		
		ServiceSpecification specs = webClient.get()
				.uri("/flowone/v1/servicecatalog?name=NetworkSlice")
					.attributes( ServletOAuth2AuthorizedClientExchangeFilterFunction.clientRegistrationId("aflowone"))
					.retrieve()
				  .bodyToMono( new ParameterizedTypeReference< ServiceSpecification>() {})
				  .block();
		

		System.out.println("order date: " + specs.toString()  );
		
//		if ( specs!=null ) {
//			for (ServiceSpecification o : specs) {
//				System.out.println("order date: " + o.toString()  );
//				
//			}			
//		}
		
//		
//
//		
//		
//		
//		
//		ExchangeStrategies exchangeStrategies = ExchangeStrategies.builder()
//                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(-1)).build(); //spring.codec.max-in-memory-size=-1 ?? if use autoconfiguration
//
//		var tcpClient = TcpClient.create()
//			      .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 2_000)
//			      .doOnConnected(connection ->
//			        connection.addHandlerLast(new ReadTimeoutHandler(2))
//			          .addHandlerLast(new WriteTimeoutHandler(2)));
//		SslContext sslContext = SslContextBuilder
//				.forClient()
//				.trustManager(InsecureTrustManagerFactory.INSTANCE)
//				.build();
//		WebClient webClient3 =    WebClient.builder()
//	        	 .exchangeStrategies(exchangeStrategies)
//	        	 .clientConnector(new ReactorClientHttpConnector(
//	        			 HttpClient.from(tcpClient)
//	        			 .secure( sslContextSpec -> sslContextSpec.sslContext(sslContext) ))
//	        			 )
//	            .baseUrl("https://patras5g.eu")
//	            .defaultCookie("cookieKey", "cookieValue")
//	            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
//	            .filter(ExchangeFilterFunctions.basicAuthentication("username", "pass"))
//	            .defaultUriVariables(Collections.singletonMap("url", "https://patras5g.eu"))
//	            .build();
//		
//		String resp= webClient3.get()
//				.uri("/apiportal/services/api/repo/vxfs")
//					.attributes( ServletOAuth2AuthorizedClientExchangeFilterFunction.clientRegistrationId("authOpensliceProvider"))
//					.retrieve()
//					.onStatus(HttpStatus::is4xxClientError, response -> {
//				        System.out.println("4xx eror");
//				        return Mono.error(new RuntimeException("4xx"));
//				      })
//				      .onStatus(HttpStatus::is5xxServerError, response -> {
//				        System.out.println("5xx eror");
//				        return Mono.error(new RuntimeException("5xx"));
//				      })
//				  .bodyToMono( String.class)
//				  .block();
//				
//
//				System.out.println("resp: " + resp );
	}

	private ExchangeFilterFunction logRequest() {
	    return (clientRequest, next) -> {
	      log.info("Request: {} {}", clientRequest.method(), clientRequest.url());
	      log.info("--- Http Headers: ---");
	      clientRequest.headers().forEach(this::logHeader);
	      log.info("--- Http Cookies: ---");
	      clientRequest.cookies().forEach(this::logHeader);
	      return next.exchange(clientRequest);
	    };
	  }
	 
	  private ExchangeFilterFunction logResponse() {
	    return ExchangeFilterFunction.ofResponseProcessor(clientResponse -> {
	      log.info("Response: {}", clientResponse.statusCode());
	      clientResponse.headers().asHttpHeaders()
	        .forEach((name, values) -> values.forEach(value -> log.info("{}={}", name, value)));
	      return Mono.just(clientResponse);
	    });
	  }
	  
	  
	  private void logHeader(String name, List<String> values) {
		    values.forEach(value -> log.info("{}={}", name, value));
	 }
	  
	  
	
}

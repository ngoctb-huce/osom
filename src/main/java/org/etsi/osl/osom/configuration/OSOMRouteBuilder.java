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
package org.etsi.osl.osom.configuration;

import java.util.Date;
import java.util.Map;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.etsi.osl.osom.management.ServiceOrderManager;
import org.etsi.osl.osom.serviceactions.ServiceActionCheck;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.etsi.osl.model.ExperimentMetadata;
import org.etsi.osl.model.Product;
import org.etsi.osl.model.ValidationJob;
import org.etsi.osl.model.ValidationStatus;
import org.etsi.osl.model.VxFMetadata;
import org.etsi.osl.tmf.am642.model.AlarmCreate;
import org.etsi.osl.tmf.so641.model.ServiceOrder;

@Configuration
//@RefreshScope
@Component
public class OSOMRouteBuilder extends RouteBuilder {

	private static final transient Log logger = LogFactory.getLog(OSOMRouteBuilder.class.getName());

	


    @Value("${CRD_DEPLOY_CR_REQ}")
    private String CRD_DEPLOY_CR_REQ = "";
    
    
    
	public void configure() {
		




      from("direct:retriesCRD_DEPLOY_CR_REQ")
      .errorHandler(deadLetterChannel("direct:retriesDeadLetters")
              .maximumRedeliveries( 10 ) //let's try 10 times to send it....
              .redeliveryDelay( 30000 ).useOriginalMessage()
              //.deadLetterHandleNewException( false )
              //.logExhaustedMessageHistory(false)
              .logExhausted(true)
              .logHandled(true)
              //.retriesExhaustedLogLevel(LoggingLevel.WARN)
              .retryAttemptedLogLevel( LoggingLevel.WARN) )
      .to(CRD_DEPLOY_CR_REQ);
      
      
      /**
       * dead Letter Queue Users if everything fails to connect
       */
      from("direct:retriesDeadLetters")
      //.setBody()
      //.body(String.class)
      .process( ErroneousValidationProcessor )
      .to("stream:out");
      
//    .errorHandler(deadLetterChannel("direct:dlq_bugzilla")
//            .maximumRedeliveries( 4 ) //let's try for the next 120 mins to send it....
//            .redeliveryDelay( 60000 ).useOriginalMessage()
//            .deadLetterHandleNewException( false )
//            //.logExhaustedMessageHistory(false)
//            .logExhausted(true)
//            .logHandled(true)
//            //.retriesExhaustedLogLevel(LoggingLevel.WARN)
//            .retryAttemptedLogLevel( LoggingLevel.WARN) )

//		from("jms:queue:OSOM.NEW_SERVICEORDER_PROCESS")
//			.log(LoggingLevel.INFO, log, "New OSOM.IN.SERVICEORDER message received!")
//			.to("log:DEBUG?showBody=true&showHeaders=true")
//			.unmarshal().json( JsonLibrary.Jackson, ServiceOrder.class, true)
//			.log(LoggingLevel.INFO, log, "Order id = ${body.id} ")
//			.bean( ServiceOrderManager.class, "processOrder")
//			;
//		
//		from("jms:queue:OSOM.NEW_SERVICEORDER_PROCESS.LIST_PENDING")
//		.log(LoggingLevel.INFO, log, "New OSOM.NEW_SERVICEORDER_PROCESS.LIST_PENDING message received!")
//		.to("log:DEBUG?showBody=true&showHeaders=true")
//		.bean( ServiceOrderManager.class, "getTasks")
//		;
//		
//
//		
//		
//		from("jms:queue:OSOM.HUMAN_COMPLETE_ORDER_TASK")
//		.log(LoggingLevel.INFO, log, "New OSOM.HUMAN_COMPLETE_ORDER_TASK message received!")
//		.to("log:DEBUG?showBody=true&showHeaders=true")
//		//.unmarshal().json( JsonLibrary.Jackson, ServiceOrder.class, true)
//		//.log(LoggingLevel.INFO, log, "Order id = ${body.id}")
//		.bean( ServiceOrderManager.class, "humanComplete");
		
		
		
		//create route here to get service spec by id from model via bus
		
//		
//		from("activemq:OSOMIN_TEXT")
//		.log(LoggingLevel.INFO, log, "New activemq:OSOMIN_TEXT message received")
//		.setBody(constant("46"))
//		.to("stream:out")
//		.end();
//		
//		from("seda:OSOMIN_SERVICEORDERTEXT").log(LoggingLevel.INFO, log, "New seda:OSOMIN_SERVICEORDERTEXT message received")
//		.to("stream:out");;
//		
	}
	
	Processor ErroneousValidationProcessor = new Processor() {
      
      @Override
      public void process(Exchange exchange) throws Exception {

          Map<String, Object> headers = exchange.getIn().getHeaders(); 
          Product aProd = exchange.getIn().getBody( Product.class ); 
          
                  
          if (aProd instanceof VxFMetadata) {
              ((VxFMetadata) aProd).setValidationStatus( ValidationStatus.COMPLETED );
          } else if (aProd instanceof ExperimentMetadata) {
              ((ExperimentMetadata) aProd).setValidationStatus( ValidationStatus.COMPLETED );
          }
          
          
          if ( aProd.getValidationJobs() != null ) {
              ValidationJob j = new ValidationJob();
              j.setDateCreated( new Date() );
              j.setJobid("ERROR");
              j.setValidationStatus(false);
              j.setOutputLog( "Error from the OSOM Route builder Service" );
              aProd.getValidationJobs().add(j);
          }
          
          exchange. getOut().setBody( aProd  );
          // copy attachements from IN to OUT to propagate them
          //exchange.getOut().setAttachments(exchange.getIn().getAttachments());
          
      }
  };

}

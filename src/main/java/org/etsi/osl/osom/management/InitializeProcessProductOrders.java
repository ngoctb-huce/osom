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

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.etsi.osl.tmf.common.model.Any;
import org.etsi.osl.tmf.common.model.service.Characteristic;
import org.etsi.osl.tmf.common.model.service.Note;
import org.etsi.osl.tmf.common.model.service.ServiceSpecificationRef;
import org.etsi.osl.tmf.pcm620.model.ProductOffering;
import org.etsi.osl.tmf.pcm620.model.ProductOfferingRef;
import org.etsi.osl.tmf.pcm620.model.ProductSpecification;
import org.etsi.osl.tmf.po622.model.ProductOrder;
import org.etsi.osl.tmf.po622.model.ProductOrderItem;
import org.etsi.osl.tmf.po622.model.ProductOrderStateType;
import org.etsi.osl.tmf.po622.model.ProductOrderUpdate;
import org.etsi.osl.tmf.prm669.model.RelatedParty;
import org.etsi.osl.tmf.so641.model.ServiceOrder;
import org.etsi.osl.tmf.so641.model.ServiceOrderCreate;
import org.etsi.osl.tmf.so641.model.ServiceOrderItem;
import org.etsi.osl.tmf.so641.model.ServiceOrderStateType;
import org.etsi.osl.tmf.so641.model.ServiceRestriction;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import jakarta.validation.Valid;

@Component(value = "initializeProcessProductOrders") // bean name
public class InitializeProcessProductOrders implements JavaDelegate {

	private static final transient Log logger = LogFactory.getLog(InitializeProcessProductOrders.class.getName());

	@Autowired
	private ProductOrderManager productOrderManager;
	

    @Autowired
    private ServiceOrderManager serviceOrderManager;
    

    @Value("${spring.application.name}")
    private String compname;

	@Autowired
	private RuntimeService runtimeService;

	public void execute(DelegateExecution execution) {

		logger.info("Check if there are new Product Orders for Orchestration: " + execution.getVariables().toString());
	

		if (execution.getVariable("productOrdersToBeProcessed") instanceof ArrayList) {

			List<String> ordersToBeProcessed = (ArrayList<String>) execution.getVariable("productOrdersToBeProcessed");
			for (String oId : ordersToBeProcessed) {

			  //process each order request
			  processOrderRequest( oId );
				
			}

		}

	}

  /**
   * For each product order create a service order and perform the neccessary references
   * @param oId
   */
  private void processOrderRequest(String oId) {
      
    //create equivalent service order
    ProductOrder prodOrder = productOrderManager.retrieveProductOrder(oId);
    logger.debug("Will send message that Product Order is IN-PROGRESS orderid= " + oId );
    
    ServiceOrder serviceOrder = createServiceOrderFromProductOrder(prodOrder);
    
    if (serviceOrder==null) {
      logger.error("Service Order is NULL " );
      
      return;
    }
    
    ProductOrderUpdate productOrderUpd = new ProductOrderUpdate();
    productOrderUpd.setState(ProductOrderStateType.INPROGRESS);
    for (ProductOrderItem poi : prodOrder.getProductOrderItem() ) {
      if ( poi.getProduct() != null ) {
        Characteristic charact = new Characteristic();
        charact.setName("_SERVICE_ORDER_ID_");
        charact.setValueType("TEXT");
        charact.setValue(new Any( serviceOrder.getId() ));        
        poi.getProduct().addProductCharacteristicItem(charact);
      }
      productOrderUpd.addProductOrderItemItem(poi);
    }
     
    productOrderManager.updateProducteOrder( oId, productOrderUpd );                    
    
  }

  private ServiceOrder createServiceOrderFromProductOrder(ProductOrder prodOrder) {


    ServiceOrderCreate servOrder = new ServiceOrderCreate();
    servOrder.setCategory("Automated order");
    servOrder.setDescription("Automatically created for product order " + prodOrder.getId());
    servOrder.setRequestedStartDate(prodOrder.getRequestedStartDate());
    servOrder.setRequestedCompletionDate(prodOrder.getExpectedCompletionDate());
    RelatedParty rpi = new RelatedParty();
    rpi.setName("OSOM-ProductOrder");
    rpi.setRole("REQUESTER");
    rpi.setId("OSOM-ProductOrder");
    servOrder.addRelatedPartyItem(rpi );

    Note noteItemOrder = new Note();
    noteItemOrder.text("Automatically created for product order " + prodOrder.getId());
    noteItemOrder.setAuthor(compname);
    servOrder.addNoteItem(noteItemOrder);

    for (ProductOrderItem poi : prodOrder.getProductOrderItem()) {
      @Valid
      ProductOfferingRef poffRefpoi = poi.getProductOffering();

      ProductOffering pOffer = productOrderManager.retrieveProductOffering(poffRefpoi.getId());
      if (pOffer == null) {
        logger.error("Product Order - ProductOffering in ProductOrderItem is NULL");
        return null;
      }
      
      if ( poi.getProduct() == null ) {
        logger.error("Product Order - Product in ProductOrderItem is NULL");
        return null;        
      }

      ProductSpecification pSepc =
          productOrderManager.retrieveProductSpec(poi.getProduct().getProductSpecification().getId());
      if (pSepc == null) {
        logger.error("Product Order - ProductSpecification in Product, ProductOrderItem is NULL");
        return null;
      }

      logger.debug("Product Order - ProductOffering name= " + pOffer.getName());
      logger.debug("Product Order - ProductSpecification name= " + pSepc.getName());


      for (ServiceSpecificationRef serviceSpec : pSepc.getServiceSpecification()) {
        ServiceOrderItem soi = new ServiceOrderItem();
        servOrder.getOrderItem().add(soi);
        soi.setState(ServiceOrderStateType.ACKNOWLEDGED);

        ServiceRestriction serviceRestriction = new ServiceRestriction();
        ServiceSpecificationRef aServiceSpecificationRef = new ServiceSpecificationRef();
        aServiceSpecificationRef.setId( serviceSpec.getId() );
        aServiceSpecificationRef.setName( serviceSpec.getName());
        aServiceSpecificationRef.setVersion( serviceSpec.getVersion());

        serviceRestriction.setServiceSpecification(aServiceSpecificationRef);

//        πρεπει να perasoyme τιμες από το product order στο service order item
//        εδω debug
        
        for (Characteristic servChar : poi.getProduct().getProductCharacteristic()  ) {
          servChar.setUuid(null);
          serviceRestriction.addServiceCharacteristicItem(servChar);
        }

        

        Characteristic refProductOrderChar = new Characteristic();
        refProductOrderChar.setName("_PRODUCT_ORDER_ID_");
        refProductOrderChar.setValueType("TEXT");
        refProductOrderChar.setValue(new Any( prodOrder.getId() ));
        serviceRestriction.addServiceCharacteristicItem(refProductOrderChar);
        
        Characteristic refProductOrderItemChar = new Characteristic();
        refProductOrderItemChar.setName("_PRODUCT_ORDER_ITEM_ID_");
        refProductOrderItemChar.setValueType("TEXT");
        refProductOrderItemChar.setValue(new Any( poi.getId() ));
        serviceRestriction.addServiceCharacteristicItem(refProductOrderItemChar);
        
        soi.setService(serviceRestriction);
        
      }
      

    }

    ServiceOrder externalSOrder = serviceOrderManager.createServiceOrder(servOrder);


    return externalSOrder;


  }
  
  
}

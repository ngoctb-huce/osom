package org.etsi.osl.osom.management;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.camel.ProducerTemplate;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.etsi.osl.tmf.pcm620.model.ProductOffering;
import org.etsi.osl.tmf.pcm620.model.ProductSpecification;
import org.etsi.osl.tmf.po622.model.ProductOrder;
import org.etsi.osl.tmf.po622.model.ProductOrderStateType;
import org.etsi.osl.tmf.po622.model.ProductOrderUpdate;
import org.etsi.osl.tmf.so641.model.ServiceOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import jakarta.validation.constraints.NotNull;

@Service
public class ProductOrderManager {


  private static final transient Log logger =
      LogFactory.getLog(ProductOrderManager.class.getName());


  @Autowired
  private ProducerTemplate template;

  @Value("${CATALOG_GET_PRODUCTORDER_IDS_BY_STATE}")
  private String CATALOG_GET_PRODUCTORDER_IDS_BY_STATE = "";

  @Value("${CATALOG_GET_PRODUCTORDER_BY_ID}")
  private String CATALOG_GET_PRODUCTORDER_BY_ID = "";


  @Value("${CATALOG_UPD_PRODUCTORDER_BY_ID}")
  private String CATALOG_UPD_PRODUCTORDER_BY_ID = "";


  @Value("${CATALOG_GET_PRODUCTSPEC_BY_ID}")
  private String CATALOG_GET_PRODUCTSPEC_BY_ID = "";


  @Value("${CATALOG_GET_PRODUCTOFFERING_BY_ID}")
  private String CATALOG_GET_PRODUCTOFFERING_BY_ID = "";
  
  

  public List<String> retrieveOrdersByState(ProductOrderStateType orderState) {
    logger.info("will retrieve Product Orders " + orderState.toString() + " from catalog ");
    try {
      Map<String, Object> map = new HashMap<>();
      map.put("orderstate", orderState.toString());
      Object response =
          template.requestBodyAndHeaders(CATALOG_GET_PRODUCTORDER_IDS_BY_STATE, "", map);

      logger.debug("will retrieve Service Orders " + orderState.toString()
          + " from catalog response: " + response.getClass());
      if (!(response instanceof String)) {
        logger.error("List  object is wrong.");
        return null;
      }
      // String[] sor = toJsonObj( (String)response, String[].class );

      ArrayList<String> sor = toJsonObj((String) response, ArrayList.class);
      logger.debug("retrieveOrdersByState response is: " + response);

      // return asList(sor);
      return sor;

    } catch (Exception e) {
      logger.error("Cannot retrieve Listof Product Orders " + orderState.toString()
          + " from catalog. " + e.toString());
    }
    return null;
  }

  public ProductOrder retrieveProductOrder(String orderid) {
    logger.info("will retrieve Product Order from catalog orderid=" + orderid);
    try {
      Object response = template.requestBody(CATALOG_GET_PRODUCTORDER_BY_ID, orderid);

      if (!(response instanceof String)) {
        logger.error("Product Order object is wrong.");
        return null;
      }
      logger.debug("retrieveProductOrder response is: " + response);
      ProductOrder sor = toJsonObj((String) response, ProductOrder.class);

      return sor;

    } catch (Exception e) {
      logger.error("Cannot retrieve Product Order details from catalog. " + e.toString());
    }
    return null;
  }



  public ProductSpecification retrieveProductSpec(@NotNull String id) {
    logger.info("will retrieve ProductSpecification from catalog orderid=" + id);
    try {
      Object response = template.requestBody(CATALOG_GET_PRODUCTSPEC_BY_ID, id);

      if (!(response instanceof String)) {
        logger.error("ProductSpecification object is wrong.");
        return null;
      }
      logger.debug("retrieveProductSpecification response is: " + response);
      ProductSpecification sor = toJsonObj((String) response, ProductSpecification.class);

      return sor;

    } catch (Exception e) {
      logger.error("Cannot retrieve ProductSpecification  details from catalog. " + e.toString());
    }
    return null;
  }

  public ProductOffering retrieveProductOffering(@NotNull String id) {
    logger.info("will retrieve ProductOffering from catalog orderid=" + id);
    try {
      Object response = template.requestBody(CATALOG_GET_PRODUCTOFFERING_BY_ID, id);

      if (!(response instanceof String)) {
        logger.error("ProductOffering object is wrong.");
        return null;
      }
      logger.debug("retrievProductOffering response is: " + response);
      ProductOffering sor = toJsonObj((String) response, ProductOffering.class);

      return sor;

    } catch (Exception e) {
      logger.error("Cannot retrieve ProductOffering details from catalog. " + e.toString());
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

  public void updateProducteOrder(String orderid, ProductOrderUpdate productOrderUpd) {
    logger.info("will set Product Order in progress orderid=" + orderid);
    try {

      template.sendBodyAndHeader(CATALOG_UPD_PRODUCTORDER_BY_ID, toJsonString(productOrderUpd),
          "orderid", orderid);


    } catch (Exception e) {
      logger.error("Cannot set Product Order status from catalog. " + e.toString());
    }

  }


}

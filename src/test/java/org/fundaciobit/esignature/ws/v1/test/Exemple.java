package org.fundaciobit.esignature.ws.v1.test;


import java.net.URL;  
import java.util.Map; 
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;


import java.util.Map;import javax.xml.ws.BindingProvider;


import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.log4j.Logger;
import org.fundaciobit.esginature.ws.api.v1.DataToValidateDTO;
import org.fundaciobit.esginature.ws.api.v1.DigestAlgorithm;
import org.fundaciobit.esginature.ws.api.v1.MimeType;
import org.fundaciobit.esginature.ws.api.v1.RemoteDocument;
import org.fundaciobit.esginature.ws.api.v1.SimpleReport;
import org.fundaciobit.esginature.ws.api.v1.SoapDocumentValidationService;
import org.fundaciobit.esginature.ws.api.v1.SoapDocumentValidationServiceImplService;


public class Exemple{
  
  public static final Logger log = Logger.getLogger(Exemple.class);
  
  public static void main(String[] args) {

    try {
      
   // Adreça servidor
      final String endpointBase = "http://localhost:8080/services/soap/";
      
      
      SoapDocumentValidationService validationService;
      {
        String endpoint = endpointBase + "validation";
        SoapDocumentValidationServiceImplService service;
        URL wsdl = new URL(endpoint + "?wsdl");
        service = new SoapDocumentValidationServiceImplService(wsdl);
        validationService = service.getSoapDocumentValidationServiceImplPort();

        Map<String, Object> reqContext;
        reqContext = ((BindingProvider) validationService).getRequestContext();
        reqContext.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, endpoint);
      }
      
     //File f1 = new File("src/test/resources/XAdESLTA.xml");
     //validateDocXml(f1,validationService);
     File f2= new File("src/test/resources/hola_signat.pdf");
     validateDocPdf(f2,validationService);
     File f3 = new File("src/test/resources/Test_7_RESULT_WARN.pdf");
     validateDocPdf(f3,validationService);
      
     
      try {
      
    	  
      } finally {
      }

    }catch (Exception e) {
      System.err.println("Error desconegut: " + e.getMessage());
    }

  }
  
  private static void validateDocXml(File f, SoapDocumentValidationService validationService) {
    
    try {
    
    MimeType mt = new MimeType();
    mt.setMimeTypeString("text/xml");
    
    byte[] b = FileUtils.readFileToByteArray(f);
    RemoteDocument rem = new RemoteDocument();
    rem.setBytes(b);
    rem.setDigestAlgorithm(null);
    rem.setMimeType(mt);
    rem.setName(f.getName());
    
    
    org.fundaciobit.esginature.ws.api.v1.DataToValidateDTO dataToValidateDTO = new DataToValidateDTO();
    
    dataToValidateDTO.setSignedDocument(rem);
    dataToValidateDTO.setOriginalDocument(null);
    dataToValidateDTO.setPolicy(null);
    
    
    
    System.out.println("VALIDANT FIRMA:");
    org.fundaciobit.esginature.ws.api.v1.WsReportsDTO ret;
    ret = validationService.validateSignature(dataToValidateDTO);
    System.out.println("**REPORT**");
    printReport(ret);    
  }catch (Exception e) {
    System.err.println("Error desconegut: " + e.getMessage());
  }
    
    
  }
  
  
private static void validateDocPdf(File f, SoapDocumentValidationService validationService) {
    
    try {
    
    MimeType mt = new MimeType();
    mt.setMimeTypeString("application/pdf");
    
    byte[] b = FileUtils.readFileToByteArray(f);
    RemoteDocument rem = new RemoteDocument();
    rem.setBytes(b);
    rem.setDigestAlgorithm(null);
    rem.setMimeType(mt);
    rem.setName(f.getName());
    
    org.fundaciobit.esginature.ws.api.v1.DataToValidateDTO dataToValidateDTO = new DataToValidateDTO();
    dataToValidateDTO.setSignedDocument(rem);
    dataToValidateDTO.setOriginalDocument(null);
    dataToValidateDTO.setPolicy(null);
    
    System.out.println("VALIDANT FIRMA:");
    org.fundaciobit.esginature.ws.api.v1.WsReportsDTO ret;
    ret = validationService.validateSignature(dataToValidateDTO);
    System.out.println("**REPORT**\n");
    printReport(ret);
    
  }catch (Exception e) {
    System.err.println("Error desconegut: " + e.getMessage());
  }
    
    
  }

  private static void printReport(org.fundaciobit.esginature.ws.api.v1.WsReportsDTO ret) {
   
    System.out.println("Document: "+ret.getSimpleReport().getDocumentName());
    System.out.println("Estat de firma: " + ret.getSimpleReport().getValidSignaturesCount() + " signatures correctes de "+(ret.getSimpleReport().getSignaturesCount()-ret.getSimpleReport().getValidSignaturesCount()));
    
    System.out.println("Firmes incorrectes: " + (ret.getSimpleReport().getSignaturesCount()-ret.getSimpleReport().getValidSignaturesCount())+"\n");
    
    if(ret.getSimpleReport().getSignaturesCount()==0) {
      System.out.println("NOT_PASSED");
    }else if(ret.getSimpleReport().getSignaturesCount() == ret.getSimpleReport().getValidSignaturesCount()){
      System.out.println("TOTAL_PASSED");
    }else {
      System.out.println("PARTIAL_PASSED");
    }
    
    System.out.println("Format de firma: "+(ret.getSimpleReport().getSignature().get(0).getSignatureFormat()));
    System.out.println("Cadena de certificat: "+(ret.getSimpleReport().getSignature().get(0).getSignedBy()));
    System.out.println("Id de certificat: "+(ret.getSimpleReport().getSignature().get(0).getId()));
    
    
    System.out.println("Politica de firma: "+(ret.getSimpleReport().getPolicy().getPolicyName()));
   
    System.out.println("Contsancia de firma: "+ret.getSimpleReport().getSignature().get(0).getBestSignatureTime());
    
    System.out.println("Politica: "+ret.getSimpleReport().getPolicy());
    System.out.println("Hora de validacio: "+ret.getSimpleReport().getValidationTime());
  }
  
}

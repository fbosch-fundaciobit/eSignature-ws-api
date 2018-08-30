package org.fundaciobit.pluginsib.validatesignature.esignature.ws;


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Properties;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Marshaller;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import javax.xml.namespace.QName;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.fundaciobit.pluginsib.validatesignature.esignature.ws.Certificate2;
import org.fundaciobit.pluginsib.validatesignature.esignature.ws.DataToValidateDTO;
import org.fundaciobit.pluginsib.validatesignature.esignature.ws.MimeType;
import org.fundaciobit.pluginsib.validatesignature.esignature.ws.RemoteDocument;
import org.fundaciobit.pluginsib.validatesignature.esignature.ws.SimpleReport;
import org.fundaciobit.pluginsib.validatesignature.esignature.ws.SoapDocumentValidationService;
import org.fundaciobit.pluginsib.validatesignature.esignature.ESignatureValidateSignaturePlugin;
import org.fundaciobit.plugins.validatesignature.api.IValidateSignaturePlugin;
import org.fundaciobit.plugins.validatesignature.api.SignatureRequestedInformation;
import org.fundaciobit.plugins.validatesignature.api.ValidateSignatureRequest;
import org.fundaciobit.plugins.validatesignature.api.ValidateSignatureResponse;


public class SoapDocumentValidationClient{
  
  public static final Logger log = Logger.getLogger(SoapDocumentValidationClient.class);
  
  public static void main(String[] args) {
    try {      
      
      Properties p = new Properties();
      p.load(new FileInputStream("./config/config.properties"));
      String propertyKeyBase = "org.fundaciobit.exemple.";
      //p.put(propertyKeyBase+ESignatureValidateSignaturePlugin.URL_BASE_PROPERTY, endpointBase);
      
      IValidateSignaturePlugin api = new ESignatureValidateSignaturePlugin(propertyKeyBase, p);
      
      File signatureData= new File("resultsCAdES/foto.jpg_cades_detached_UpgradedTo_CAdES_XL.csig");
      byte[] b = FileUtils.readFileToByteArray(signatureData);
      
      
      
      
      ValidateSignatureRequest vsr = new ValidateSignatureRequest();
      vsr.setSignatureData(b);
      vsr.setLanguage("ca");
      
      //File signedDocumentData= new File("foto.jpg");
      //byte[] b1 = FileUtils.readFileToByteArray(signedDocumentData);      
      //vsr.setSignedDocumentData(b1);
      
      
      SignatureRequestedInformation sri = new SignatureRequestedInformation();
      sri.setReturnSignatureTypeFormatProfile(true); //tipo format i resultat
      sri.setReturnCertificateInfo(true);
      sri.setReturnCertificates(true);
      sri.setReturnValidationChecks(true);
      sri.setValidateCertificateRevocation(true);
      sri.setReturnTimeStampInfo(true);
      
      vsr.setSignatureRequestedInformation(sri);
      
      
      ValidateSignatureResponse vresp = api.validateSignature(vsr);
      
      StringWriter writer = new StringWriter();
      JAXBContext context = JAXBContext.newInstance(ValidateSignatureResponse.class);
      Marshaller m = context.createMarshaller();
      m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
      m.marshal(new JAXBElement(new QName(ValidateSignatureResponse.class.getSimpleName()), ValidateSignatureResponse.class, vresp), writer);
      System.out.println(writer.toString());
      
      new FileOutputStream(new File(signatureData.getName() + "_validation.xml")).write(writer.toString().getBytes());
     
      }catch (Exception e) {
        e.printStackTrace();
      System.err.println("Error desconegut: " + e.getMessage());
    }
  }


  private static void validateDoc(File f, SoapDocumentValidationService validationService, MimeType mt) throws IOException {
    
    byte[] b = FileUtils.readFileToByteArray(f);
    RemoteDocument rem = new RemoteDocument();
    rem.setBytes(b);
    rem.setDigestAlgorithm(null);
    rem.setMimeType(mt);
    rem.setName(f.getName());
    
    org.fundaciobit.pluginsib.validatesignature.esignature.ws.DataToValidateDTO dataToValidateDTO = new DataToValidateDTO();
    dataToValidateDTO.setSignedDocument(rem);
    dataToValidateDTO.setOriginalDocument(null);
    dataToValidateDTO.setPolicy(null);
    
    System.out.println("VALIDANT FIRMA:");
    org.fundaciobit.pluginsib.validatesignature.esignature.ws.WsReportsDTO ret;
    ret = validationService.validateSignature(dataToValidateDTO);
    System.out.println("**REPORT**\n");
    
  }

  private static void printSimpleReport(org.fundaciobit.pluginsib.validatesignature.esignature.ws.WsReportsDTO ret) {
    
    System.out.println("RESULTAT DE FIRMA");
    SimpleReport simple = ret.getSimpleReport();
   
    System.out.println("Document Name: "+simple.getDocumentName());
    System.out.println("Estat de firma: " + simple.getValidSignaturesCount() + " signatures correctes de "+(simple.getSignaturesCount()-ret.getSimpleReport().getValidSignaturesCount()));
    System.out.println("Firmes incorrectes: " + (simple.getSignaturesCount()-simple.getValidSignaturesCount())+"\n");
    
    if(simple.getSignaturesCount()==0) {
      System.out.println("NOT_PASSED");
    }else if(simple.getSignaturesCount() == simple.getValidSignaturesCount()){
      System.out.println("TOTAL_PASSED");
    }else {
      System.out.println("PARTIAL_PASSED");
    }
    
    System.out.println("Format de firma: "+(simple.getSignature().get(0).getSignatureFormat()));
    System.out.println("Cadena de certificat: "+(simple.getSignature().get(0).getSignedBy()));
    System.out.println("Id de certificat: "+(simple.getSignature().get(0).getId()));
        
    System.out.println("Politica de firma: "+(simple.getPolicy().getPolicyName()));
    System.out.println("Contsancia de firma: "+simple.getSignature().get(0).getBestSignatureTime());
    
    System.out.println("Politica: "+simple.getPolicy());
    System.out.println("Hora de validacio: "+simple.getValidationTime());
    
  }

  private static void printCertificateInfo(org.fundaciobit.pluginsib.validatesignature.esignature.ws.WsReportsDTO ret){
    for(Certificate2 cert: ret.getDiagnosticData().getUsedCertificates().getCertificate()){
      System.out.println("-Nombre del certificado:" + cert.getCommonName());
      System.out.println("Pais: " + cert.getCountryName());
      System.out.println("E-mail: " + cert.getEmail());
      System.out.println("Localidad: " + cert.getLocality());
      System.out.println("ID: " + cert.getId());
      System.out.println("Unitat organitzativa: " + cert.getOrganizationalUnit());
      System.out.println("Nom de la organitzacio: " + cert.getOrganizationName());
      System.out.println("Authority Informaton Access URL: " + cert.getAuthorityInformationAccessUrls());
    }
  }
  
  
  private static void logReport(org.fundaciobit.pluginsib.validatesignature.esignature.ws.WsReportsDTO ret) {
    
    try {
      Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("src/test/resources/log.txt"), "utf-8"));
      
}catch (Exception e) {
  
}
  }
  
  
  
/*  private static void validateDocXml(File f, SoapDocumentValidationService validationService) {    
      try {    
      MimeType mt = new MimeType();
      mt.setMimeTypeString("text/xml");
      validateDoc(f, validationService,mt);    
    }catch (Exception e) {
      System.err.println("Error desconegut: " + e.getMessage());
    }
  }*/
    
  /*private static void validateDocPdf(File f, SoapDocumentValidationService validationService) {
      try {
      MimeType mt = new MimeType();
      mt.setMimeTypeString("application/pdf");
      validateDoc(f, validationService,mt);    
    }catch (Exception e) {
      System.err.println("Error desconegut: " + e.getMessage());
    }  
  }*/

  
  /*public static void main2(String[] args) {
  try {      
    // Adreça servidor
    final String endpointBase = "http://localhost:8080/services/soap/";
    //Construcio del servei      
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
    
   //Arxius a validar:
   File f1 = new File("src/test/resources/XAdESLTA.xml");
   validateDocXml(f1,validationService);
   File f2= new File("src/test/resources/hola_signat.pdf");
   validateDocPdf(f2,validationService);
   File f3 = new File("src/test/resources/Test_7_RESULT_WARN.pdf");
   validateDocPdf(f3,validationService);
   
   try {}finally {}
    }catch (Exception e) {
    System.err.println("Error desconegut: " + e.getMessage());
  }
      
}*/
    
  
  
  
}

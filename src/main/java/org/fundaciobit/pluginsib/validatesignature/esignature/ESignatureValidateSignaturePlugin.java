package org.fundaciobit.pluginsib.validatesignature.esignature;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import javax.xml.crypto.MarshalException;
import javax.xml.crypto.dsig.Reference;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.ws.BindingProvider;

import org.bouncycastle.asn1.cms.ContentInfo;
import org.bouncycastle.asn1.cms.SignedData;
import org.bouncycastle.cms.CMSSignedData;
import org.fundaciobit.pluginsib.validatesignature.esignature.ws.BasicSignature;
import org.fundaciobit.pluginsib.validatesignature.esignature.ws.Certificate;
import org.fundaciobit.pluginsib.validatesignature.esignature.ws.Certificate2;
import org.fundaciobit.pluginsib.validatesignature.esignature.ws.Certificate3;
import org.fundaciobit.pluginsib.validatesignature.esignature.ws.CertificatePolicy;
import org.fundaciobit.pluginsib.validatesignature.esignature.ws.CertifiedRole;
import org.fundaciobit.pluginsib.validatesignature.esignature.ws.ContainerInfo;
import org.fundaciobit.pluginsib.validatesignature.esignature.ws.DataToValidateDTO;
import org.fundaciobit.pluginsib.validatesignature.esignature.ws.DetailedReport;
import org.fundaciobit.pluginsib.validatesignature.esignature.ws.DetailedReport2;
import org.fundaciobit.pluginsib.validatesignature.esignature.ws.DiagnosticData;
import org.fundaciobit.pluginsib.validatesignature.esignature.ws.DigestAlgoAndValue;
import org.fundaciobit.pluginsib.validatesignature.esignature.ws.DistinguishedName;
import org.fundaciobit.pluginsib.validatesignature.esignature.ws.ManifestFile;
import org.fundaciobit.pluginsib.validatesignature.esignature.ws.MimeType;
import org.fundaciobit.pluginsib.validatesignature.esignature.ws.OID;
import org.fundaciobit.pluginsib.validatesignature.esignature.ws.RemoteDocument;
import org.fundaciobit.pluginsib.validatesignature.esignature.ws.Signature;
import org.fundaciobit.pluginsib.validatesignature.esignature.ws.Signature2;
import org.fundaciobit.pluginsib.validatesignature.esignature.ws.Signature2.CertificateChain.ChainItem;
import org.fundaciobit.pluginsib.validatesignature.esignature.ws.SignatureLevel;
import org.fundaciobit.pluginsib.validatesignature.esignature.ws.SimpleReport;
import org.fundaciobit.pluginsib.validatesignature.esignature.ws.SoapDocumentValidationService;
import org.fundaciobit.pluginsib.validatesignature.esignature.ws.SoapDocumentValidationServiceImplService;
import org.fundaciobit.pluginsib.validatesignature.esignature.ws.Timestamp;
import org.fundaciobit.pluginsib.validatesignature.esignature.ws.TrustedList;

import org.fundaciobit.plugins.validatesignature.api.AbstractValidateSignaturePlugin;
import org.fundaciobit.plugins.validatesignature.api.CertificateInfo;
import org.fundaciobit.plugins.validatesignature.api.IValidateSignaturePlugin;
import org.fundaciobit.plugins.validatesignature.api.SignatureCheck;
import org.fundaciobit.plugins.validatesignature.api.SignatureDetailInfo;
import org.fundaciobit.plugins.validatesignature.api.SignatureRequestedInformation;
import org.fundaciobit.plugins.validatesignature.api.TimeStampInfo;
import org.fundaciobit.plugins.validatesignature.api.ValidateSignatureRequest;
import org.fundaciobit.plugins.validatesignature.api.ValidateSignatureResponse;
import org.fundaciobit.plugins.validatesignature.api.ValidationStatus;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

//import es.gob.afirma.i18n.Language;
//import es.gob.afirma.signature.SigningException;
import net.java.xades.security.xml.XMLSignatureElement;

public class ESignatureValidateSignaturePlugin extends AbstractValidateSignaturePlugin
    implements IValidateSignaturePlugin {

  public static final String ESIGNATURE_BASE_PROPERTIES = VALIDATE_SIGNATURE_BASE_PROPERTY + "esignature.";
  public static final String URL_BASE_PROPERTY = ESIGNATURE_BASE_PROPERTIES + "url";
  public static final String DEBUG_BASE_PROPERTY = ESIGNATURE_BASE_PROPERTIES + "debug";
  public static final Map<String, String> localSignType2PluginSignType = new HashMap<String, String>();
  public static final Map<String, TypeFormatProfile> esignature2TypeFormatProfile = new HashMap<String, TypeFormatProfile>();
  
  final String DISTINGUISHED_NAME_FORMAT = "RFC";

  static {
    
    final String PADES_BASELINE_B = "PAdES-BASELINE-B";
    final String PAdES_BASELINE_T = "PAdES-BASELINE-T";
    
    final String XADES_BASELINE_T = "XAdES-BASELINE-T";
    final String XADES_BASELINE_LT = "XAdES-BASELINE-LT";
    final String XADES_BASELINE_LTA = "XAdES-BASELINE-LTA";
    final String XADES_X = "XAdES-X";
    final String XADES_C = "XAdES-C";
       
    final String CADES_BASELINE_T = "CAdES-BASELINE-T";
    final String CADES_BASELINE_LTA = "CAdES-BASELINE-LTA";
    final String CADES_BASELINE_LT = "CAdES-BASELINE-LT";
    final String CADES_X = "CAdES-101733-X";
    

    esignature2TypeFormatProfile.put(PADES_BASELINE_B,
        new TypeFormatProfile(SIGNTYPE_PAdES, SIGNFORMAT_IMPLICIT_ENVELOPED_ATTACHED, SIGNPROFILE_PADES_LTV));
    
    esignature2TypeFormatProfile.put(PAdES_BASELINE_T,
        new TypeFormatProfile(SIGNTYPE_PAdES, SIGNFORMAT_IMPLICIT_ENVELOPED_ATTACHED, SIGNPROFILE_T));
    
    
    
    esignature2TypeFormatProfile.put(XADES_BASELINE_T,
        new TypeFormatProfile(SIGNTYPE_XAdES, null, SIGNPROFILE_T));
    
    esignature2TypeFormatProfile.put(XADES_BASELINE_LT,
        new TypeFormatProfile(SIGNTYPE_XAdES, null, SIGNPROFILE_XL1));
    
    esignature2TypeFormatProfile.put(XADES_BASELINE_LTA,
        new TypeFormatProfile(SIGNTYPE_XAdES, null, SIGNPROFILE_A));
    
    esignature2TypeFormatProfile.put(XADES_X,
        new TypeFormatProfile(SIGNTYPE_XAdES, null, SIGNPROFILE_X1));
    
    esignature2TypeFormatProfile.put(XADES_C,
        new TypeFormatProfile(SIGNTYPE_XAdES, null, SIGNPROFILE_X2));
    
    
    
    esignature2TypeFormatProfile.put(CADES_BASELINE_T,
        new TypeFormatProfile(SIGNTYPE_CAdES, null, SIGNPROFILE_T));
    
    esignature2TypeFormatProfile.put(CADES_BASELINE_LT,
        new TypeFormatProfile(SIGNTYPE_CAdES, null, SIGNPROFILE_XL1));
    
    esignature2TypeFormatProfile.put(CADES_BASELINE_LTA,
        new TypeFormatProfile(SIGNTYPE_CAdES, null, SIGNPROFILE_A));

    esignature2TypeFormatProfile.put(CADES_X,
        new TypeFormatProfile(SIGNTYPE_CAdES, null, SIGNPROFILE_X1));
  }

  public ESignatureValidateSignaturePlugin() {
    super();
    // TODO Auto-generated constructor stub
  }
  public ESignatureValidateSignaturePlugin(String propertyKeyBase, Properties properties) {
    super(propertyKeyBase, properties);
    // TODO Auto-generated constructor stub
  }
  public ESignatureValidateSignaturePlugin(String propertyKeyBase) {
    super(propertyKeyBase);
    // TODO Auto-generated constructor stub
  }
  @Override
  public SignatureRequestedInformation getSupportedSignatureRequestedInformation() {
    // TODO Auto-generated method stub
    return null;
  }
  @Override
  public SignatureRequestedInformation getSupportedSignatureRequestedInformationBySignatureType(String signType) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public ValidateSignatureResponse validateSignature(ValidateSignatureRequest validationRequest) throws Exception {

    // DEFINICIO DE CONSTANTS:
    boolean returnTypeFormatProfile = validationRequest.getSignatureRequestedInformation().getReturnSignatureTypeFormatProfile();
    boolean returnCertificateInfo = validationRequest.getSignatureRequestedInformation().getReturnCertificateInfo();
    boolean returnCertificates = validationRequest.getSignatureRequestedInformation().getReturnCertificates();
    boolean returnValidationChecks = validationRequest.getSignatureRequestedInformation().getReturnValidationChecks();
    boolean returnCertificateRevocation = validationRequest.getSignatureRequestedInformation().getValidateCertificateRevocation();
    boolean returnTimeStampInfo = validationRequest.getSignatureRequestedInformation().getReturnTimeStampInfo();
    
    
    // Final String endpoint = "http://localhost:8080/services/soap/validation";
    final String endpoint = this.getPropertyRequired(URL_BASE_PROPERTY);
    final boolean debug = "true".equals(this.getProperty(DEBUG_BASE_PROPERTY));

    // CONSTRUCCIO DEL SERVEI
    SoapDocumentValidationService validationService;
    {
      SoapDocumentValidationServiceImplService service;
      URL wsdl = new URL(endpoint + "?wsdl");
      service = new SoapDocumentValidationServiceImplService(wsdl);
      validationService = service.getSoapDocumentValidationServiceImplPort();
      Map<String, Object> reqContext;
      reqContext = ((BindingProvider) validationService).getRequestContext();
      reqContext.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, endpoint);
    }

    //DECLARACIÓ DELS REMOTEDOCUMENT
    RemoteDocument remSign = new RemoteDocument();
    RemoteDocument doc = null;
    byte[] sigData = validationRequest.getSignatureData();

    //FIRMA
    if (sigData != null) {
      remSign.setBytes(sigData);
      remSign.setDigestAlgorithm(null);
      remSign.setMimeType(getMimeType(sigData));
      remSign.setName(null);

      //DOCUMENT FIRMAT
      byte[] docData = validationRequest.getSignedDocumentData();
      if (docData != null) {
        doc = new RemoteDocument();
        doc.setBytes(docData);
        doc.setDigestAlgorithm(null);
        doc.setMimeType(getMimeType(docData));
        doc.setName(null);
      }
    }
    
    //Objecte final a validar "DataToValidate"
    org.fundaciobit.pluginsib.validatesignature.esignature.ws.DataToValidateDTO dataToValidateDTO = new DataToValidateDTO();
    dataToValidateDTO.setSignedDocument(remSign);
    dataToValidateDTO.setOriginalDocument(doc);
    dataToValidateDTO.setPolicy(null);

    //Validacio del document
    org.fundaciobit.pluginsib.validatesignature.esignature.ws.WsReportsDTO signatureReport;
    signatureReport = validationService.validateSignature(dataToValidateDTO);

    //Variables per treballar amb les dades retornades de la validacio
    SimpleReport simple = signatureReport.getSimpleReport();
    DiagnosticData diagnostic = signatureReport.getDiagnosticData();
    DetailedReport2 detailed = signatureReport.getDetailedReport();

    if (debug){
      System.out.println("");
      System.out.println("****************************************");
      System.out.println("************ D E B U G  O N ************");
      System.out.println("****************************************");
      debuggerOn(signatureReport);
    }

    ValidateSignatureResponse validateSignatureResponse = new ValidateSignatureResponse();

    //Resultat de la validacio: Sempre es retorna
    ValidationStatus status = new ValidationStatus();
    if (simple.getSignaturesCount() == simple.getValidSignaturesCount()) {
      status.setStatus(ValidationStatus.SIGNATURE_VALID);
    } else {
      
      status.setStatus(ValidationStatus.SIGNATURE_INVALID);
      
      
    }
    validateSignatureResponse.setValidationStatus(status);

    //Signature Detail Info List:
    List<SignatureDetailInfo> detailsList = new ArrayList<SignatureDetailInfo>();
    SignatureDetailInfo sdi = new SignatureDetailInfo();
    
    StringBuffer errorBuffer = new StringBuffer();
    
    int i=1;
    //Resultats opcionals de la validacio:
    for (Signature2 sig2 : diagnostic.getSignatures().getSignature()) {
      
      errorBuffer.append("\nFirma"+i+"\n" + "Indication: "+ simple.getSignature().get(i-1).getIndication() + "\n"
          + "Subindication: "+ simple.getSignature().get(i-1).getSubIndication()+"\n");
      
      
      
      if(returnTypeFormatProfile){        
        TypeFormatProfile tfp = esignature2TypeFormatProfile.get(sig2.getSignatureFormat());

        if(tfp != null) {
          if(sig2.getSignatureFormat().equals("PAdES-BASELINE-B") && sig2.getTimestamps().getTimestamp().size() == 0) {
            validateSignatureResponse.setSignType(SIGNTYPE_PAdES);
            validateSignatureResponse.setSignFormat(getSignFormat(SIGNTYPE_PAdES,sigData));
              if(sig2.getPolicy() == null) {
                validateSignatureResponse.setSignProfile(SIGNPROFILE_BES);
              }else{
                validateSignatureResponse.setSignProfile(SIGNPROFILE_EPES);
              }
          }else{
            validateSignatureResponse.setSignType(tfp.type);
            validateSignatureResponse.setSignFormat(getSignFormat(tfp.type,sigData));
            validateSignatureResponse.setSignProfile(tfp.profile); 
          } 
        }
      }
      
      //Certificate Info
      if (returnCertificateInfo) {
        sdi.setAlgorithm(sig2.getBasicSignature().getDigestAlgoUsedToSignThisToken());
        sdi.setCertificateInfo(constructCertificateInfo(diagnostic));
      }

      //Certificates (binaris)
      if(returnCertificates){
        List<byte[]> cadena = new ArrayList<byte[]>();
        sdi.setAlgorithm(sig2.getBasicSignature().getDigestAlgoUsedToSignThisToken());
        for (Certificate2 ce : diagnostic.getUsedCertificates().getCertificate()) {
          cadena.add(ce.getBase64Encoded());
        }
        if (cadena.size() > 0){
          sdi.setCertificateChain(cadena.toArray(new byte[cadena.size()][]));
        }
      }

      //Validation Checks.
      // NO ES TROBEN ELS CHECKS A ESIGNATURE 
      if (returnValidationChecks){ 
        sdi.setAlgorithm(sig2.getBasicSignature().getDigestAlgoUsedToSignThisToken());
        /*List<SignatureCheck> validChecks = new ArrayList<SignatureCheck>();
        SignatureCheck e = new SignatureCheck();
        e.setName();
        e.setType();
        validChecks.add(e); 
        sdi.setValidChecks(validChecks);*/
      }

      // Cert. Revocation
      if (returnCertificateRevocation) {
        sdi.setAlgorithm(sig2.getBasicSignature().getDigestAlgoUsedToSignThisToken());
      }

      // TimeStampInfo
      if (returnTimeStampInfo) {
        TimeStampInfo tsi = new TimeStampInfo();
        HashMap<String, Certificate2> timestampCertificateMap = new HashMap<String,Certificate2>();
        tsi.setAlgorithm(sig2.getBasicSignature().getDigestAlgoUsedToSignThisToken());
      
        if(sig2.getTimestamps().getTimestamp().size()>0) {
          for(Timestamp ts: sig2.getTimestamps().getTimestamp()) {
            for(Certificate2 cert: diagnostic.getUsedCertificates().getCertificate()) {
              if(ts.getSigningCertificate().getId().equals(cert.getId())) {
                timestampCertificateMap.put(ts.getSigningCertificate().getId(), cert);
              }
            }
          }
        }           
        
        for(Timestamp ts: sig2.getTimestamps().getTimestamp()){
            for(DistinguishedName ds: timestampCertificateMap.get(ts.getSigningCertificate().getId()).getIssuerDistinguishedName()) {
              if(ds.getValue().contains(DISTINGUISHED_NAME_FORMAT)) {
                tsi.setCertificateIssuer(ds.getValue());
                break;
              }else{
                tsi.setCertificateIssuer(ds.getValue());
              }
          }
            
          for(DistinguishedName ds: timestampCertificateMap.get(ts.getSigningCertificate().getId()).getSubjectDistinguishedName()) {
            if(ds.getValue().contains(DISTINGUISHED_NAME_FORMAT)) {
              tsi.setCertificateSubject(ds.getValue());
              break;
            }else {
              tsi.setCertificateSubject(ds.getValue());
            }
          } 
        }  
       sdi.setTimeStampInfo(tsi);
      }
      sdi.setAlgorithm(sig2.getBasicSignature().getDigestAlgoUsedToSignThisToken());
      detailsList.add(sdi);
    }
    
    status.setErrorMsg(errorBuffer.toString());
    SignatureDetailInfo[] detailsArray = new SignatureDetailInfo[detailsList.size()];
    detailsList.toArray(detailsArray);
    validateSignatureResponse.setSignatureDetailInfo((detailsArray));
    
    return validateSignatureResponse;
  }

  private MimeType getMimeType(byte[] signatureData) throws IOException {

    InputStream is = new BufferedInputStream(new ByteArrayInputStream(signatureData));
    String mimeType = URLConnection.guessContentTypeFromStream(is);

    MimeType mt = new MimeType();
    mt.setMimeTypeString(mimeType);
    return mt;
  }

  private CertificateInfo constructCertificateInfo(DiagnosticData diagnostic) {

    CertificateInfo ci = new CertificateInfo();
    final boolean debug = "true".equals(this.getProperty(DEBUG_BASE_PROPERTY));
    
    if (debug) {
      System.out.println("\n\n");
      System.out.println("**** C E R T I F I C A T E  I N F O ****");
      System.out.println("Lista de atributos no especificados:\n");
    }

    for (Certificate2 cert : diagnostic.getUsedCertificates().getCertificate()) {

      //ci.setClasificacion();
      //ci.setExtensionUsoCertificado();
      
      if(cert.getId()!=null) {
        ci.setIdEmisor(cert.getId());
      }else if(debug) {
        System.out.println("Certificate Id = NULL");
      }
      
      if(cert.getCertificatePolicies().getCertificatePolicy().size()>0) {
        ci.setIdPolitica(cert.getCertificatePolicies().getCertificatePolicy().get(0).getValue());
      }else if(debug) {
        System.out.println("Certificate Policy Id = NULL");
      }
      
      if (cert.getSerialNumber().toString() != null) {
        ci.setNumeroSerie(cert.getSerialNumber().toString());
      }else if(debug){
        System.out.println("Num de Serie: NULL.");
      }
      
      if (cert.getOrganizationName()!=null) {
        ci.setOrganizacion(cert.getOrganizationName());
      } else if (debug) {
        System.out.println("Certificate Organizacion: NULL.");
      }
      //ci.setOrganizacionEmisora();
      
      if (cert.getCountryName() != null) {
        ci.setPais(cert.getCountryName());
      } else if (debug) {
        System.out.println("Pais: NULL.");
      }

      
      if (cert.getCertificatePolicies().getCertificatePolicy().get(0).getValue() != null) {
        ci.setPolitica(cert.getCertificatePolicies().getCertificatePolicy().get(0).getValue());
      } else if (debug) {
        System.out.println("Politica: NULL.");
      }
      
      //ci.setSubject();
      //ci.setTipoCertificado();
      
      if (cert.getOrganizationName() != null) {
        ci.setUnidadOrganizativa(cert.getOrganizationName());
      } else if (debug) {
        System.out.println("Unidad Organizativa: NULL.");
      }
      
      if (cert.getSigningCertificate() != null) {
        ci.setUsoCertificado(cert.getSigningCertificate().getSigned());
      } else if (debug){
        System.out.println("Uso Certificado: NULL.");
      }
      
      if(cert.getNotBefore()!=null) {
        ci.setValidoDesde(cert.getNotBefore().toGregorianCalendar().getTime());
      }
      
      if(cert.getNotAfter()!=null) {
        ci.setValidoHasta(cert.getNotAfter().toGregorianCalendar().getTime());
      }
      //ci.setVersionPolitica();
      
    }
    return ci;
  }

  private void debuggerOn(org.fundaciobit.pluginsib.validatesignature.esignature.ws.WsReportsDTO signatureReport) {

    SimpleReport simple = signatureReport.getSimpleReport();
    DiagnosticData diagnostic = signatureReport.getDiagnosticData();
    DetailedReport2 detailed = signatureReport.getDetailedReport();

    System.out.println("\n");
    System.out.println("**** S I M P L E   R E P O R T ****\n");

    System.out.println("Simple.getContainerType: " + simple.getContainerType());
    System.out.println("Simple.getDocumentName: " + simple.getDocumentName());
    System.out.println("simple.getSignaturesCount: " + simple.getSignaturesCount());
    System.out.println("simple.getValidSignaturesCount: " + simple.getValidSignaturesCount());
    System.out.println("simple.getPolicy.getPolicyName: " + simple.getPolicy().getPolicyName());
    // System.out.println("Policy Description:
    // "+simple.getPolicy().getPolicyDescription());
    System.out
        .println("diagnostic.getSignatures.getSignature.get(0).getBasicSignature.getDigestAlgoUsedToSignThisToken: "
            + diagnostic.getSignatures().getSignature().get(0).getBasicSignature().getDigestAlgoUsedToSignThisToken());
    System.out.println("simple.getSignature.get(0).getCertificateChain.getCertificate.get(0).getQualifiedName: "
        + simple.getSignature().get(0).getCertificateChain().getCertificate().get(0).getQualifiedName());
    for (SimpleReport.Signature s : simple.getSignature()) {
      System.out.println("simple.getSignature.get(i).getId" + s.getId());
      System.out.println("simple.getSignature.get(i).getFileName:" + s.getFilename());
      System.out.println("simple.getSignature.get(i).getIndication: " + s.getIndication());
      System.out.println("simple.getSignature.get(i).getSignatureFormat: " + s.getSignatureFormat());
      System.out.println("simple.getSignature.get(i).getSignedBy: " + s.getSignedBy());
      System.out.println("simple.getSignature.get(i).getSubIndication: " + s.getSubIndication());
      System.out.println("simple.getSignature.get(i).getBestSignatureTime: " + s.getBestSignatureTime());
      System.out.println("simple.getSignature.get(i).getSignatureLevel.getValue: " + s.getSignatureLevel().getValue());
      System.out.println(
          "simple.getSignature.get(i).getSignatureLevel.getDescription: " + s.getSignatureLevel().getDescription());
    }

    System.out.println("\n");
    System.out.println("**** D I A G N O S T I C   R E P O R T ****");
    System.out.println("");

    System.out.println("  diagnostic.getDocumentName: " + diagnostic.getDocumentName());
    System.out.println("  diagnostic.getValidationDate: " + diagnostic.getValidationDate());

    System.out.println("");
    System.out.println("- C E R T I F I C A T E S: ");

    for (Certificate2 cert : diagnostic.getUsedCertificates().getCertificate()) {
      System.out.println("\n");
      System.out.println("** CERTIFICATE NAME: " + cert.getCommonName());
      System.out.println("cert.getCountryName: " + cert.getCountryName());
      System.out.println("cert.getEmail: " + cert.getEmail());
      System.out.println("cert.getGivenName: " + cert.getGivenName());
      System.out.println("cert.getId: " + cert.getId());
      System.out.println("cert.getLocality: " + cert.getLocality());
      System.out.println("cert.getOrganizationalUnit: " + cert.getOrganizationalUnit());
      System.out.println("cert.getOrganizationName: " + cert.getOrganizationName());
      System.out.println("cert.getPseudonym: " + cert.getPseudonym());
      System.out.println("cert.getPublicKeyEncriptionAlgo: " + cert.getPublicKeyEncryptionAlgo());
      System.out.println("cert.getPublicKeySize: " + cert.getPublicKeySize());
      System.out.println("cert.getState: " + cert.getState());
      System.out.println("cert.getSurname: " + cert.getSurname());
      System.out.println("cert.getAuthorityInformationAccessUrls:");
      if (cert.getAuthorityInformationAccessUrls().getUrl() != null) {
        for (String str : cert.getAuthorityInformationAccessUrls().getUrl()) {
          System.out.println("        URL: " + str);
        }
      } else {
        System.out.println("        -no authority information access urls-");
      }

      System.out.println("cert.getBase64Encoded: " + cert.getBase64Encoded());
      System.out.println("cert.getBasicSignature.");
      BasicSignature basicSign = cert.getBasicSignature();

      System.out
          .println("      getDigestAlgorithmUsedToSignThisToken: " + basicSign.getDigestAlgoUsedToSignThisToken());
      System.out.println(
          "      getEncryptionAlgorithmUsedToSignThisToken: " + basicSign.getEncryptionAlgoUsedToSignThisToken());
      System.out.println("      getKeyLengthUsedToSignThisToken: " + basicSign.getKeyLengthUsedToSignThisToken());
      System.out.println("      getMaskGenerationFunctionToSignThisToken: "
          + basicSign.getMaskGenerationFunctionUsedToSignThisToken());

      // Cadena de certificats.
      System.out.println("Certificate Chain: ");
      if (cert.getCertificateChain() != null) {
        for (org.fundaciobit.pluginsib.validatesignature.esignature.ws.Certificate2.CertificateChain.ChainItem chain : cert
            .getCertificateChain().getChainItem()) {
          System.out.println("    chain.getId: " + chain.getId());
          System.out.println("    chain.getSource: " + chain.getSource());
        }
      } else {
        System.out.println("    -no certificate chain-");
      }

      // Certificats de politiques
      System.out.println("CertificatePolicies:");
      if (cert.getCertificatePolicies().getCertificatePolicy() != null) {
        for (CertificatePolicy policy : cert.getCertificatePolicies().getCertificatePolicy()) {
          System.out.println("    Policy Value: " + policy.getValue());
          System.out.println("    Policy CpsUrl: " + policy.getCpsUrl());
          System.out.println("      Policy Description: " + policy.getDescription());
        }
      } else {
        System.out.println("    -no certificate policies-");
      }

      // Distribution points.
      System.out.println("Distribution Points: ");
      if (cert.getCRLDistributionPoints().getUrl() != null) {
        for (String s : cert.getCRLDistributionPoints().getUrl()) {
          System.out.println("    Distribution point: " + s);
        }
      } else {
        System.out.println("   -no distribution points-");
      }

      // Digest algorithms and values.
      System.out.println("DigestAlgoAndValues:");
      if (cert.getDigestAlgoAndValues().getDigestAlgoAndValue() != null) {
        for (DigestAlgoAndValue algoValues : cert.getDigestAlgoAndValues().getDigestAlgoAndValue()) {
          System.out.println("    Digest Method: " + algoValues.getDigestMethod());
          System.out.println("    Digest Value: " + algoValues.getDigestValue());
        }
      } else {
        System.out.println("    -no digest algortihms and values-");
      }

      // Exteded key usages.
      System.out.println("ExtendedKeyUsages:");
      if (cert.getExtendedKeyUsages().getOid() != null) {
        for (OID oid : cert.getExtendedKeyUsages().getOid()) {
          System.out.println("    Extended Key value: " + oid.getValue());
          System.out.println("    Extended Key Description: " + oid.getDescription());
        }
      } else {
        System.out.println("    -no extended keys usage-");
      }

      // Info and messages
      System.out.println("Info:");
      if (cert.getInfo().getMessage() != null) {
        for (Certificate2.Info.Message message : cert.getInfo().getMessage()) {
          System.out.println("    Message Id: " + message.getId());
          System.out.println("    Message Value: " + message.getValue());
        }
      } else {
        System.out.println("    -no info or messages-");
      }
      System.out.println("\n");
    }
    System.out.println("\n");
    System.out.println("**** S I G N A T U R E S   R E P O R T ****\n");

    for (Signature2 sig2 : diagnostic.getSignatures().getSignature()) {
      System.out.println("sig2.getContentHints: " + sig2.getContentHints());
      System.out.println("sig2.getContentIdentifier: " + sig2.getContentIdentifier());
      System.out.println("sig2.getContentType: " + sig2.getContentType());
      System.out.println("sig2.getContentHints: " + sig2.getContentHints());
      System.out.println("sig2.getParentId: " + sig2.getParentId());
      System.out.println("sig2.getSignatureFilename" + sig2.getSignatureFilename());
      System.out.println("sig2.getSignatureFormat: " + sig2.getSignatureFormat());
      System.out.println("-Basic Signature: ");
      System.out.println("    basicSignature.getDigestAlgoUsedToSignThisToken: "
          + sig2.getBasicSignature().getDigestAlgoUsedToSignThisToken());
      System.out.println("    basicSignature.getEncryptionAlgoUsedToSignThisToken: "
          + sig2.getBasicSignature().getEncryptionAlgoUsedToSignThisToken());
      System.out.println("    basicSignature.getKeyLengthToSignThisToken: "
          + sig2.getBasicSignature().getKeyLengthUsedToSignThisToken());
      System.out.println("    basicSignature.getMaskGenerationFunctionUsedToSignThisToken: "
          + sig2.getBasicSignature().getMaskGenerationFunctionUsedToSignThisToken());

      System.out.println("-Certificate Chain: ");
      for (ChainItem ci : sig2.getCertificateChain().getChainItem()) {
        System.out.println("    chainItem.getId: " + ci.getId());
        System.out.println("    chainItem.getSource: " + ci.getSource());
      }
      System.out.println("-Certified Roles: ");
      for (CertifiedRole cr : sig2.getCertifiedRoles()) {
        System.out.println("    certifiedRole.getCategory: " + cr.getCategory());
        System.out.println("    certifiedRole.getCertifiedRole: " + cr.getCertifiedRole());
        System.out.println("    certifiedRole.getNotAfter: " + cr.getNotAfter());
        System.out.println("    certifiedRole.getNotBefore: " + cr.getNotBefore());
      }

      System.out.println("-Claimed Roles: ");
      for (String s : sig2.getClaimedRoles().getClaimedRoles()) {
        System.out.println("    claimedRole.getClaimedRoles: " + s);
      }

      System.out.println("-Commitment Type Indications: ");
      if (sig2.getCommitmentTypeIndication() != null) {
        for (String s : sig2.getCommitmentTypeIndication().getIndication()) {
          System.out.println("    Indication: " + s);
        }
      } else {
        System.out.println("    - No Commitment Type Indications: ");
      }

      if (diagnostic.getContainerInfo() != null) {

        System.out.println("\n");
        System.out.println("**** C O N T A I N E R  I N F O   R E P O R T ****\n");

        ContainerInfo contInfo = diagnostic.getContainerInfo();
        System.out.println("contInfo.getContainerType: " + contInfo.getContainerType());
        System.out.println("contInfo.getMimeType: " + contInfo.getMimeTypeContent());
        System.out.println("contInfo.getZipComment: " + contInfo.getZipComment());

        System.out.println("-Content Files: ");
        for (String s : diagnostic.getContainerInfo().getContentFiles().getContentFile()) {
          System.out.println("    Content File: " + s);
        }

        System.out.println("-Manifest Files: ");
        for (ManifestFile mf : diagnostic.getContainerInfo().getManifestFiles().getManifestFile()) {
          System.out.println("    manifestFile.getFilename: " + mf.getFilename());
          System.out.println("    manifestFile.getSignatureFilename: " + mf.getSignatureFilename());
          System.out.println("    -Entries: ");
          for (String s : mf.getEntries().getEntry()) {
            System.out.println("        Entry: " + s);
          }
        }

        System.out.println("\n\n");
        System.out.println("**** L I S T  O F  T R U S T E D  L I S T   R E P O R T ****\n");

        TrustedList tl = diagnostic.getListOfTrustedLists();

        System.out.println("trustedList.getCountryCode: " + tl.getCountryCode());
        System.out.println("trustedList.getUrl: " + tl.getUrl());
        System.out.println("trustedList.getSequenceNumber: " + tl.getSequenceNumber());
        System.out.println("trustedList.getVersion: " + tl.getVersion());

        System.out.println("-Content Files: ");
        for (TrustedList tlst : diagnostic.getTrustedLists().getTrustedList()) {
          System.out.println("    trustedList.getCountryCode: " + tlst.getCountryCode());
          System.out.println("    trustedList.getUrl: " + tlst.getUrl());
          System.out.println("    trustedList.getVersion: " + tlst.getVersion());
          System.out.println("    trustedList.getSequenceNumber: " + tlst.getSequenceNumber());
          System.out.println("    trustedList.getIssueDate: " + tlst.getIssueDate());
          System.out.println("    trustedList.getLastLoading: " + tlst.getLastLoading());
          System.out.println("    trustedList.getNextUpdate: " + tlst.getNextUpdate());
        }

      }
    }
  }
  
  /**
   * AQUEST MÈTODE ESTA DUPLICAT AL PLUGIN-INTEGR@
   * @param signType
   * @param signData
   * @return
   * @throws Exception
   */
  protected String getSignFormat(String signType, final byte[] signData) throws Exception {
    String signFormat;
    if (SIGNTYPE_CMS.equals(signType)) { // "CMS";
      // TODO Això no se si es correcte !!!!!!!
      try {
        signFormat = getCAdESFormat(signData);
      } catch(Throwable th) {
        log.error("Error intentant obtenir el format d'una firma CMS emprant el mètode getCAdESFormat(): " + th.getMessage(), th);
        signFormat = null;
      }
    } else if (SIGNTYPE_CAdES.equals(signType)) { // "CAdES";
      signFormat = getCAdESFormat(signData);
    } else if (SIGNTYPE_XAdES.equals(signType)) { // "XAdES";
      signFormat = getXAdESFormat(signData);
    } else if (SIGNTYPE_ODF.equals(signType)) { // "ODF";
      signFormat = SIGNFORMAT_IMPLICIT_ENVELOPED_ATTACHED;
    } else if (SIGNTYPE_PDF.equals(signType)) { // "PDF"; //
      signFormat = SIGNFORMAT_IMPLICIT_ENVELOPED_ATTACHED;
    } else if (SIGNTYPE_PAdES.equals(signType)) { // "PAdES";
      signFormat = SIGNFORMAT_IMPLICIT_ENVELOPED_ATTACHED;
    } else if (SIGNTYPE_OOXML.equals(signType)) { // "OOXML";
      signFormat = SIGNFORMAT_IMPLICIT_ENVELOPED_ATTACHED;
    } else if (SIGNTYPE_XML_DSIG.equals(signType)) { // "XML_DSIG";
      // TODO Això no se si es correcte !!!!!!!
      try {
        signFormat = getXAdESFormat(signData);
      } catch(Throwable th) {
        log.error("Error intentant obtenir el format d'una firma XML_DSIG emprant el"
            + " mètode getXAdESFormat(): " + th.getMessage(), th);
        signFormat = null;
      }
    } else {
      log.warn("Error intentant trobar el format de una firma amb tipus desconegut: "
        + signType, new Exception());
      signFormat = null;
    }
    return signFormat;
  }
  
  /**
   * AQUEST MÈTODE ESTA DUPLICAT AL PLUGIN-INTEGR@ 
   * @param eSignature
   * @return
   * @throws SigningException
   */
  public static String getXAdESFormat(byte[] signature) throws Exception {
    
    DocumentBuilderFactory dBFactory = DocumentBuilderFactory.newInstance();
    dBFactory.setNamespaceAware(true);

    Document eSignature = dBFactory.newDocumentBuilder().parse(
        new ByteArrayInputStream(signature));

    XMLSignature xmlSignature;
    String rootName = eSignature.getDocumentElement().getNodeName();
    if (rootName.equalsIgnoreCase("ds:Signature") || rootName.equals("ROOT_COSIGNATURES")) {
      // "XAdES Enveloping"
      return SIGNFORMAT_IMPLICIT_ENVELOPING_ATTACHED;
    }
    NodeList signatureNodeLs = eSignature.getElementsByTagName("ds:Manifest");
    if (signatureNodeLs.getLength() > 0) {
      // "XAdES Externally Detached
      return SIGNFORMAT_EXPLICIT_EXTERNALLY_DETACHED;
    }
    NodeList signsList = eSignature.getElementsByTagNameNS(
        "http://www.w3.org/2000/09/xmldsig#", "Signature");
    if (signsList.getLength() == 0) {
      throw new Exception("La llista de firmes esta buida: XS003");
    }
    Node signatureNode = signsList.item(0);
    try {
      xmlSignature = new XMLSignatureElement((Element) signatureNode).getXMLSignature();
    } catch (MarshalException e) {
      throw new Exception(e);
    }
    List<?> references = xmlSignature.getSignedInfo().getReferences();
    for (int i = 0; i < references.size(); ++i) {
      if (!"".equals(((Reference) references.get(i)).getURI()))
        continue;
      // "XAdES Enveloped"
      return SIGNFORMAT_IMPLICIT_ENVELOPED_ATTACHED;
    }
    // "XAdES Detached"
    return SIGNFORMAT_EXPLICIT_DETACHED;
  }
   
  /**
   * AQUEST MÈTODE ESTA DUPLICAT AL PLUGIN-INTEGR@  
   * @param signature
   * @return
   * @throws Exception
   */
  public static String getCAdESFormat(byte[] signature) throws Exception {

    CMSSignedData cmsSignedData = new CMSSignedData(signature);

    // ContentInfo contentInfo = cmsSignedData.toASN1Structure();
    // //getContentInfo();
    ContentInfo contentInfo;
    try {
      java.lang.reflect.Method method;
      method = cmsSignedData.getClass().getMethod("toASN1Structure");

      contentInfo = (ContentInfo) method.invoke(cmsSignedData);

    } catch (Exception e) {
      java.lang.reflect.Method method;
      method = cmsSignedData.getClass().getMethod("getContentInfo");

      contentInfo = (ContentInfo) method.invoke(cmsSignedData);
    }

    // (Object)contentInfo.getContent()
    java.lang.reflect.Method method;
    method = contentInfo.getClass().getMethod("getContent");

    Object encodable = method.invoke(contentInfo);

    SignedData signedData = SignedData.getInstance(encodable);
    ContentInfo contentInfo2 = signedData.getEncapContentInfo();
    boolean isImplicit = false;
    if (contentInfo2 != null) {

      // signedData.getEncapContentInfo().getContent() != null) {
      java.lang.reflect.Method method2;
      method2 = contentInfo2.getClass().getMethod("getContent");

      Object obj = method2.invoke(contentInfo2);

      if (obj != null) {
        isImplicit = true;
      }
    }

    if (isImplicit) {
      //  "CAdES attached/implicit signature";
      return SIGNFORMAT_IMPLICIT_ENVELOPING_ATTACHED;
    } else {
      //  "CAdES detached/explicit signature"
      return SIGNFORMAT_EXPLICIT_DETACHED;
    }
  }
  
  
  
}

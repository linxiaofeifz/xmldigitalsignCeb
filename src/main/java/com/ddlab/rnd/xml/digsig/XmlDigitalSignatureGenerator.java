package com.ddlab.rnd.xml.digsig;

import com.ddlab.rnd.crypto.KryptoUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.servlet.http.HttpServletResponse;
import javax.xml.crypto.MarshalException;
import javax.xml.crypto.dsig.*;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import javax.xml.crypto.dsig.keyinfo.KeyValue;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.security.*;
import java.util.Collections;

/**
 * This class is used to provide convenient methods to digitally sign an XML
 * document.
 *
 * @author <a href="mailto:debadatta.mishra@gmail.com">Debadatta Mishra</a>
 * @since 2013
 */
@Component
public class XmlDigitalSignatureGenerator {

    Logger logger = LoggerFactory.getLogger(XmlDigitalSignatureGenerator.class);

    @Autowired
    private ResourceLoader resourceLoader;

    @Autowired
    private KryptoUtil kryptoUtil;

//    private static final String CEB_SIGNED_XML_FILE_PATH = "classpath:xml" +
//            "/GzeportTransfer_ceb_sign.xml";

    /**
     * Method used to get the XML document by parsing
     *
     * @param xmlFilePath , file path of the XML document
     * @return Document
     */
    public Document getXmlDocument(String xmlFilePath) {
        Document doc = null;
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        InputStream fis = null;
        try {
            logger.info("xmlFilePath: " + xmlFilePath);
            fis = resourceLoader.getResource(xmlFilePath).getInputStream();
            doc = dbf.newDocumentBuilder().parse(fis);
        } catch (ParserConfigurationException ex) {
            ex.printStackTrace();
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        } catch (SAXException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            if(fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return doc;
    }

    /**
     * Method used to get the KeyInfo
     *
     * @param xmlSigFactory
     * @param publicKeyPath
     * @return KeyInfo
     */
    private KeyInfo getKeyInfo(XMLSignatureFactory xmlSigFactory, String publicKeyPath) {
        KeyInfo keyInfo = null;
        KeyValue keyValue = null;
        PublicKey publicKey = kryptoUtil.getStoredPublicKey(publicKeyPath);
        KeyInfoFactory keyInfoFact = xmlSigFactory.getKeyInfoFactory();

        try {
            keyValue = keyInfoFact.newKeyValue(publicKey);
        } catch (KeyException ex) {
            ex.printStackTrace();
        }
        keyInfo = keyInfoFact.newKeyInfo(Collections.singletonList(keyValue));
        return keyInfo;
    }

    /*
     * Method used to store the signed XMl document
     */
    public String storeSignedDoc(Document doc, HttpServletResponse response, boolean cebFirst) {
        TransformerFactory transFactory = TransformerFactory.newInstance();
        Transformer trans = null;
        try {
            trans = transFactory.newTransformer();
        } catch (TransformerConfigurationException ex) {
            ex.printStackTrace();
        }
        try {
            StreamResult streamRes = new StreamResult(response.getOutputStream());
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            if(cebFirst) {
                streamRes = new StreamResult(bos);
            }
            trans.transform(new DOMSource(doc), streamRes);
            if(cebFirst){
                return new String(bos.toByteArray());
            }
        } catch (TransformerException ex) {
            ex.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Method used to attach a generated digital signature to the existing
     * document
     *
     * @param privateKeyFilePath
     * @param publicKeyFilePath
     */
    public String generateXMLDigitalSignature( String privateKeyFilePath,
                                            String publicKeyFilePath,
                                            Document doc, HttpServletResponse response, boolean cebFirst) {
        XMLSignatureFactory xmlSigFactory = XMLSignatureFactory.getInstance("DOM");
        PrivateKey privateKey = kryptoUtil.getStoredPrivateKey(privateKeyFilePath);
        DOMSignContext domSignCtx = new DOMSignContext(privateKey, doc.getDocumentElement());
        Reference ref = null;
        SignedInfo signedInfo = null;
        try {
            ref = xmlSigFactory.newReference("", xmlSigFactory.newDigestMethod(DigestMethod.SHA1, null),
                    Collections.singletonList(xmlSigFactory.newTransform(Transform.ENVELOPED,
                    (TransformParameterSpec) null)), null, null);
            signedInfo = xmlSigFactory.newSignedInfo(
                    xmlSigFactory.newCanonicalizationMethod(CanonicalizationMethod.INCLUSIVE,
                    (C14NMethodParameterSpec) null),
                    xmlSigFactory.newSignatureMethod(SignatureMethod.RSA_SHA1, null),
                    Collections.singletonList(ref));
        } catch (NoSuchAlgorithmException ex) {
            ex.printStackTrace();
        } catch (InvalidAlgorithmParameterException ex) {
            ex.printStackTrace();
        }
        //Pass the Public Key File Path 
        KeyInfo keyInfo = getKeyInfo(xmlSigFactory, publicKeyFilePath);
        //Create a new XML Signature
        XMLSignature xmlSignature = xmlSigFactory.newXMLSignature(signedInfo, keyInfo);
        try {
            //Sign the document
            xmlSignature.sign(domSignCtx);
        } catch (MarshalException ex) {
            ex.printStackTrace();
        } catch (XMLSignatureException ex) {
            ex.printStackTrace();
        }
        //Store the digitally signed document inta a location
        return storeSignedDoc(doc, response, cebFirst);
    }
}

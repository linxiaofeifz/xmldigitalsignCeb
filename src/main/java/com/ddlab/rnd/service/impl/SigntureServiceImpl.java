package com.ddlab.rnd.service.impl;

import com.ddlab.rnd.service.ISigntureService;
import com.ddlab.rnd.utils.EncodeUtil;
import com.ddlab.rnd.xml.digsig.XmlDigitalSignatureGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 功能说明 ：
 *
 * @ version Rversion 1.0.0
 * 修改时间       | 修改内容
 */

@Service
public class SigntureServiceImpl implements ISigntureService {

    Logger logger = LoggerFactory.getLogger(SigntureServiceImpl.class);

    @Autowired
    private XmlDigitalSignatureGenerator xmlDigitalSignatureGenerator;

    private static final String ORIGIN_XML_FILE_PATH =  "classpath:xml" +
            "/GzeportTransfer_origin.xml";

    private static final String SIGNED_XML_FILE_PATH = "classpath:xml" +
            "/GzeportTransfer_sign.xml";

    @Value("${privateKeyFilePath:classpath:/keys/privatekey.key}")
    private String privateKeyFilePath;

    @Value("${publicKeyFilePath:classpath:/keys/publickey.key}")
    private String publicKeyFilePath;

    public void sign(String xmlStr, HttpServletResponse response, boolean cebRequest) {
        // 将请求数据转换成document
        Document doc = converXmlString2Document(xmlStr);
        // 根据请求数据，修改和转换origin数据
        Document originDocument = converOriginDoc(doc, ORIGIN_XML_FILE_PATH, xmlStr, cebRequest);
        // 根据转换后的origin数据进行签名
        genSign(originDocument, response);
    }

    public void signCeb(String xmlStr, HttpServletResponse response) {
        // 将请求数据转换成document
        Document doc = converXmlString2Document(xmlStr);
        // 根据转换后的origin数据进行签名
        String signedCeb = genSignCeb(doc, response);
        logger.info("signed ceb:" + signedCeb);
        sign(signedCeb, response, true);
    }

    private String genSignCeb( Document originDocument, HttpServletResponse response) {
        return xmlDigitalSignatureGenerator.generateXMLDigitalSignature(privateKeyFilePath, publicKeyFilePath,
                originDocument, response, true);
    }

    private void genSign( Document originDocument, HttpServletResponse response) {
        xmlDigitalSignatureGenerator.generateXMLDigitalSignature(privateKeyFilePath, publicKeyFilePath,
                originDocument, response, false);
    }

    private Document converXmlString2Document(String xmlStr) {
        StringReader sr = new StringReader(xmlStr);
        InputSource is = new InputSource(sr);
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder= null;
        Document doc = null;
        try {
            builder = factory.newDocumentBuilder();
            doc = builder.parse(is);
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return doc;
    }

    private Document converOriginDoc(Document dataDoc, String originXmlFilePath,
                                String dataStr, boolean cebRequest) {
        logger.info("originXmlFilePath: " + originXmlFilePath);
        Document originXmlDocument = xmlDigitalSignatureGenerator.getXmlDocument(originXmlFilePath);
        changeOriginHead(dataDoc, originXmlDocument, cebRequest);
        changeOriginData(dataStr, originXmlDocument);
        return originXmlDocument;
    }

    /**
     * 功能说明：根据原始数据的head修改oringin文件的head部分
     * @param dataDoc
     * @param originXmlDocument
     * @return void
     */
    private void changeOriginHead(Document dataDoc,  Document originXmlDocument, boolean cebRequest) {
        Element dataDocRoot = dataDoc.getDocumentElement();
        NodeList logisticsList = dataDocRoot.getElementsByTagName("Head");
        if(cebRequest) {
            logisticsList = dataDocRoot.getElementsByTagName("ceb:LogisticsHead");
        }
        Node logisticsNode = logisticsList.item(0);
        Map<String, String> parseMap = paserDataHead(logisticsNode);
        if(cebRequest) {
            parseMap.put("MessageType", dataDocRoot.getTagName().split(":")[1]);
        }

        Element originDocRoot = originXmlDocument.getDocumentElement();
        NodeList originHeadList = originDocRoot.getElementsByTagName("Head");
        Node originHead = originHeadList.item(0);
        setOriginHead(originHead, parseMap, cebRequest);
    }

    private void changeOriginData(String dataStr,
                                 Document originXmlDocument) {

        Element originDocRoot = originXmlDocument.getDocumentElement();
        NodeList originDataList = originDocRoot.getElementsByTagName("Data");
        Node originData = originDataList.item(0);
        if(originData instanceof Element) {
            Element ele = (Element)originData;
            try {
                ele.setTextContent(EncodeUtil.encodeBase64(dataStr.getBytes("UTF-8")));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
    }

    private Map<String, String> paserDataHead(Node dataHead) {
        Map<String, String> resultMap = new HashMap<>();
        NodeList childNodes = dataHead.getChildNodes();
        for(int i=0; i<childNodes.getLength(); i++) {
            Node node = childNodes.item(i);
            if(node instanceof Element) {
                Element ele = (Element) node;
                String eleName = ele.getTagName();
                switch (eleName) {
                    case "ceb:appTime":
                        resultMap.put("SendTime", ele.getTextContent());
                        break;
                    case "MessageID":
                    case "MessageType":
                    case "Sender":
                    case "Receiver":
                    case "SendTime":
                        resultMap.put(eleName, ele.getTextContent());
                        break;
                }
            }
        }
        return resultMap;
    }

    private void setOriginHead(Node originHead, Map<String,
            String> dataMap, boolean cebRequest) {
        NodeList childNodes = originHead.getChildNodes();
        for(int i=0; i<childNodes.getLength(); i++) {
            Node node = childNodes.item(i);
            if(node instanceof Element) {
                Element ele = (Element) node;
                String eleName = ele.getTagName();
                Set<String> keySet = dataMap.keySet();
                if("Receivers".equals(eleName)) {
                    if(!cebRequest) {
                        NodeList receiverList = ele.getElementsByTagName("Receiver");
                        for(int j=0; j<receiverList.getLength(); j++) {
                            Node item = receiverList.item(j);
                            if(item instanceof Element) {
                                Element receiverEle = (Element) item;
                                receiverEle.setTextContent(dataMap.get("Receiver"));
                            }
                        }
                    }
                } else {
                    if(keySet.contains(eleName)) {
                        ele.setTextContent(dataMap.get(eleName));
                    }
                }
            }
        }
    }

}

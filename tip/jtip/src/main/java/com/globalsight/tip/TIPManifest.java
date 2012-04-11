package com.globalsight.tip;

import static com.globalsight.tip.TIPConstants.*;
import static com.globalsight.tip.XMLUtil.*;
import static javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.globalsight.tip.TIPConstants.ContributorTool;
import com.globalsight.tip.TIPConstants.Creator;
import com.globalsight.tip.TIPConstants.ObjectFile;

public class TIPManifest {

    private TIPPackage tipPackage;
    private String packageId;
    private TIPTask task; // Either request or response
    private TIPCreator creator = new TIPCreator();
    
    private Map<String, TIPObjectSection> objectSections = 
        new HashMap<String, TIPObjectSection>();    
    
    TIPManifest(TIPPackage tipPackage) {
        this.tipPackage = tipPackage;
    }

    static TIPManifest newManifest(TIPPackage tipPackage) {
        TIPManifest manifest = new TIPManifest(tipPackage);
        manifest.setPackageId(UUID.randomUUID().toString());
        return manifest;
    }
    
    void saveToStream(OutputStream saveStream) throws TIPException { 
        try {
            Document document = new ManifestDOMBuilder(this).makeDocument();
            validate(document);
            TransformerFactory factory = TransformerFactory.newInstance();
            Transformer transformer = factory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty(
                    "{http://xml.apache.org/xslt}indent-amount", "2");
            transformer.transform(new DOMSource(document), 
                    new StreamResult(saveStream));
        }
        catch (Exception e) {
            throw new TIPException(e);
        }
    }
    
    // XXX This should blow away any existing settings 
    void loadFromStream(InputStream manifestStream) 
                throws TIPValidationException, IOException, ParserConfigurationException {
        Document document = parse(manifestStream);
        validate(document);
        loadManifest(document);
    }    
    
    private void loadManifest(Document document) 
                            throws TIPValidationException {
        Element manifest = getFirstChildElement(document);
        loadDescriptor(getFirstChildByName(manifest, GLOBAL_DESCRIPTOR));
        loadPackageObjects(getFirstChildByName(manifest, PACKAGE_OBJECTS));
    }
    
    private void loadDescriptor(Element descriptor) 
                            throws TIPValidationException {
        packageId = getChildTextByName(descriptor, UNIQUE_PACKAGE_ID);
        
        creator = loadCreator(getFirstChildByName(descriptor, PACKAGE_CREATOR));
        // Either load the request or the response, depending on which is
        // present
        task = loadTaskRequestOrResponse(descriptor);
    }
    
    private TIPCreator loadCreator(Element creatorEl) {
        TIPCreator creator = new TIPCreator();
        creator.setName(getChildTextByName(creatorEl, Creator.NAME));
        creator.setId(getChildTextByName(creatorEl, Creator.ID));
        creator.setDate(
            loadDate(getFirstChildByName(creatorEl, Creator.UPDATE)));
        creator.setTool(loadTool(
                getFirstChildByName(creatorEl, TOOL)));
        return creator;
    }
    
    private TIPTask loadTaskRequestOrResponse(Element descriptor) 
                        throws TIPValidationException {
        Element requestEl = getFirstChildByName(descriptor, TASK_REQUEST);
        if (requestEl != null) {
            return loadTaskRequest(requestEl);
        }
        return loadTaskResponse(getFirstChildByName(descriptor, 
                                 TASK_RESPONSE));
    }
    
    private void loadTask(Element taskEl, TIPTask task) {
        task.setTaskType(getChildTextByName(taskEl, Task.TYPE));
        task.setSourceLocale(getChildTextByName(taskEl, Task.SOURCE_LANGUAGE));
        task.setTargetLocale(getChildTextByName(taskEl, Task.TARGET_LANGUAGE));
    }
    
    private TIPTaskRequest loadTaskRequest(Element requestEl) {
        TIPTaskRequest request = new TIPTaskRequest();
        loadTask(getFirstChildByName(requestEl, TASK), request);
        return request;
    }
    
    private TIPTaskResponse loadTaskResponse(Element responseEl) 
                                throws TIPValidationException {
        TIPTaskResponse response = new TIPTaskResponse();
        loadTask(getFirstChildByName(responseEl, TASK), response);
        Element inResponseTo = getFirstChildByName(responseEl, 
                                TaskResponse.IN_RESPONSE_TO);
        response.setRequestPackageId(getChildTextByName(inResponseTo,
                                                 UNIQUE_PACKAGE_ID));
        response.setRequestCreator(loadCreator(
                getFirstChildByName(inResponseTo, PACKAGE_CREATOR)));
        response.setComment(getChildTextByName(responseEl, 
                            TaskResponse.COMMENT));
        String rawMessage = getChildTextByName(responseEl, 
                            TaskResponse.MESSAGE);
        TIPTaskResponse.Message msg = TIPTaskResponse.Message.fromValue(rawMessage);
        if (msg == null) {
            throw new TIPValidationException(
                    "Invalid ResponseMessage value: " + msg);
        }
        response.setMessage(msg);
        return response;
    }
    
    private Date loadDate(Element dateNode) {
        return DateUtil.parseTIPDate(getTextContent(dateNode));
    }
    
    private TIPTool loadTool(Element toolEl) {
        TIPTool tool = new TIPTool();
        tool.setName(getChildTextByName(toolEl, ContributorTool.NAME));
        tool.setId(getChildTextByName(toolEl, ContributorTool.ID));
        tool.setVersion(getChildTextByName(toolEl, ContributorTool.VERSION));
        return tool;
    }
    
    private void loadPackageObjects(Element parent) 
                            throws TIPValidationException {
        // parse all the sections
        NodeList children = 
            parent.getElementsByTagName(PACKAGE_OBJECT_SECTION);
        for (int i = 0; i < children.getLength(); i++) {
            TIPObjectSection section = 
                loadPackageObjectSection((Element)children.item(i));
            // Don't allow duplicate sections
            if (objectSections.containsKey(section.getType())) {
                throw new TIPValidationException("Duplicate object section: " +
                                                  section.getType());
            }
            objectSections.put(section.getType(), section);
        }
    }
    
    private TIPObjectSection loadPackageObjectSection(Element section) 
                    throws TIPValidationException {
        TIPObjectSection objectSection = new TIPObjectSection(
                section.getAttribute(ATTR_SECTION_NAME),
                section.getAttribute(ATTR_SECTION_TYPE));
        objectSection.setPackage(tipPackage);
        NodeList children = section.getElementsByTagName(OBJECT_FILE);
        for (int i = 0; i < children.getLength(); i++) {
            objectSection.addObject(loadObjectFile((Element)children.item(i)));
        }
        return objectSection;
    }
    
    private TIPObjectFile loadObjectFile(Element file) 
                            throws TIPValidationException {
        TIPObjectFile object = new TIPObjectFile();
        object.setPackage(tipPackage);
        String rawSequence = file.getAttribute(ObjectFile.ATTR_SEQUENCE);
        try {
            object.setSequence(Integer.parseInt(rawSequence));
        }
        catch (NumberFormatException e) {
            throw new TIPValidationException(
                    "Invalid sequence value: '" + rawSequence + "'");
        }
        object.setLocation(getChildTextByName(file, ObjectFile.LOCATION));
        object.setName(getChildTextByName(file, ObjectFile.NAME));
        return object;
    }

    Document parse(InputStream is) throws ParserConfigurationException, 
                                    TIPValidationException, IOException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            return builder.parse(is);
        }
        catch (SAXException e) {
            throw new TIPValidationException(e);
        }
        finally {
            is.close();
        }
    }
    
    void validate(Document dom) throws TIPValidationException {
        try {
            InputStream is = 
                getClass().getResourceAsStream("/TIPPManifest-1_4.xsd");
            SchemaFactory factory = 
                SchemaFactory.newInstance(W3C_XML_SCHEMA_NS_URI);
            Schema schema = factory.newSchema(new StreamSource(is));
            // need an error handler
            Validator validator = schema.newValidator();
            validator.validate(new DOMSource(dom));
            is.close();
        }
        catch (Exception e) {
            throw new TIPValidationException(e);
        }
    }

    public String getPackageId() {
        return packageId;
    }

    public void setPackageId(String packageId) {
        this.packageId = packageId;
    }
    
    public TIPCreator getCreator() {
        return creator;
    }

    public void setCreator(TIPCreator creator) {
        this.creator = creator;
    }

    public TIPTask getTask() {
        return task;
    }
    
    public void setTask(TIPTask task) {
        this.task = task;
    }
    
    public boolean isResponse() {
        return (task instanceof TIPTaskResponse);
    }

    /**
     * Return a collection of all object sections with a given type.  
     * @param type section type
     * @return (possibly empty) collection of object sections
     */
    public TIPObjectSection getObjectSection(String type) {
        return objectSections.get(type);
    }
    
    /**
     * Return a collection of all object sections.
     * @return (possibly empty) collection of object sections
     */
    public Collection<TIPObjectSection> getObjectSections() {
        return objectSections.values();
    }
    
    public TIPObjectSection addObjectSection(String name, String type) {
        TIPObjectSection section = new TIPObjectSection(name, type);
        section.setPackage(tipPackage);
        objectSections.put(type, section);
        return section;
    }
    
    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (o == null || !(o instanceof TIPManifest)) return false;
        TIPManifest m = (TIPManifest)o;
        return m.getPackageId().equals(getPackageId()) &&
                m.getCreator().equals(getCreator()) &&
                m.getTask().equals(getTask()) &&
                m.getObjectSections().equals(getObjectSections());
    }
    
    @Override
    public String toString() {
        return "TIPManifest(id=" + getPackageId() + ", creator=" + getCreator()
                + ", task=" + getTask() + ", sections=" + getObjectSections() 
                + ")";
    }
}

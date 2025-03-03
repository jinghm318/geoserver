/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.api;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.internal.JsonContext;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import javax.xml.namespace.QName;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.geoserver.data.test.CiteTestData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.mock.web.MockHttpServletResponse;

public class OGCApiTestSupport extends GeoServerSystemTestSupport {

    protected String getEncodedName(QName qName) {
        if (qName.getPrefix() != null) {
            return qName.getPrefix() + "__" + qName.getLocalPart();
        } else {
            return qName.getLocalPart();
        }
    }

    protected DocumentContext getAsJSONPath(String path, int expectedHttpCode) throws Exception {
        MockHttpServletResponse response = getAsMockHttpServletResponse(path, expectedHttpCode);
        return getAsJSONPath(response);
    }

    protected DocumentContext getAsJSONPath(MockHttpServletResponse response)
            throws UnsupportedEncodingException {
        assertThat(response.getContentType(), containsString("json"));
        JsonContext json = (JsonContext) JsonPath.parse(response.getContentAsString());
        if (!isQuietTests()) {
            print(json(response));
        }
        return json;
    }

    protected MockHttpServletResponse getAsMockHttpServletResponse(
            String path, int expectedHttpCode) throws Exception {
        MockHttpServletResponse response = getAsServletResponse(path);

        assertEquals(expectedHttpCode, response.getStatus());
        return response;
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        // init xmlunit
        Map<String, String> namespaces = new HashMap<>();
        namespaces.put("atom", "http://www.w3.org/2005/Atom");
        namespaces.put("ogc", "http://www.opengis.net/ogc");
        registerNamespaces(namespaces);

        CiteTestData.registerNamespaces(namespaces);

        XMLUnit.setXpathNamespaceContext(new SimpleNamespaceContext(namespaces));
    }

    /**
     * Allows subclasses to register namespaces. By default, does not add any, subclasses can
     * manipulate the namespaces map as they see fit.
     *
     * @param namespaces
     */
    protected void registerNamespaces(Map<String, String> namespaces) {}

    protected Document getAsJSoup(String url) throws Exception {
        MockHttpServletResponse response = getAsMockHttpServletResponse(url, 200);
        assertEquals("text/html", response.getContentType());

        LOGGER.log(Level.INFO, "Last request returned\n:" + response.getContentAsString());

        // parse the HTML
        Document document = Jsoup.parse(response.getContentAsString());
        return document;
    }

    protected byte[] getAsByteArray(String url) throws Exception {
        MockHttpServletResponse response = getAsMockHttpServletResponse(url, 200);
        return response.getContentAsByteArray();
    }

    protected JsonContext convertYamlToJsonPath(String yaml) throws Exception {
        ObjectMapper yamlReader = new ObjectMapper(new YAMLFactory());
        Object obj = yamlReader.readValue(yaml, Object.class);

        ObjectMapper jsonWriter = new ObjectMapper();
        JsonContext json = (JsonContext) JsonPath.parse(jsonWriter.writeValueAsString(obj));
        return json;
    }

    /** Retuns a single element out of an array, checking that there is just one */
    protected Object getSingle(DocumentContext json, String path) {
        List items = json.read(path);
        assertEquals(
                "Found " + items.size() + " items for this path, but was expecting one: " + path,
                1,
                items.size());
        return items.get(0);
    }

    /** Checks the specified jsonpath exists in the document */
    protected boolean exists(DocumentContext json, String path) {
        List items = json.read(path);
        return items.size() > 0;
    }
}

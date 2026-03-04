package pl.pekao.zesportal.service;

import org.springframework.stereotype.Component;
import pl.pekao.zesportal.entity.JtuxedoServiceField;
import pl.pekao.zesportal.service.dto.ParsedServiceDefinition;
import pl.pekao.zesportal.service.dto.ParsedServiceDefinition.ParsedFieldDefinition;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Parsuje plik XML z definicjami usług (format np. tuxedo_maestro.xml)
 * i zwraca listę usług z polami wejściowymi (input).
 */
@Component
public class XmlServiceDefinitionParser {

    private static final Pattern FIX_UNQUOTED_ATTR = Pattern.compile("(\\s)(xmlns|xsi):([\\w-]+)=([^\"'\\s][^\\s>]*)");

    /**
     * Parsuje zawartość XML i zwraca listę usług (elementy &lt;service&gt;).
     * Używa tylko pól z sekcji &lt;input&gt; (pola wejściowe do wywołania usługi).
     *
     * @param xmlContent zawartość pliku XML (UTF-8)
     * @return lista zparsowanych definicji (może być pusta przy błędzie parsowania)
     * @throws IllegalArgumentException gdy XML jest nieprawidłowy
     */
    public List<ParsedServiceDefinition> parse(String xmlContent) {
        if (xmlContent == null || xmlContent.isBlank()) {
            return List.of();
        }
        String normalized = normalizeXmlAttributes(xmlContent);
        var factory = DocumentBuilderFactory.newDefaultInstance();
        factory.setIgnoringComments(true);
        factory.setCoalescing(true);
        try {
            var builder = factory.newDocumentBuilder();
            var doc = builder.parse(new ByteArrayInputStream(normalized.getBytes(StandardCharsets.UTF_8)));
            doc.getDocumentElement().normalize();
            return parseServices(doc);
        } catch (Exception e) {
            throw new IllegalArgumentException("Nie można odczytać pliku XML: " + e.getMessage(), e);
        }
    }

    /** Naprawia niecytowane atrybuty w root (np. xmlns:xsi=http://...) */
    private String normalizeXmlAttributes(String xml) {
        return FIX_UNQUOTED_ATTR.matcher(xml).replaceAll("$1$2:$3=\"$4\"");
    }

    private List<ParsedServiceDefinition> parseServices(org.w3c.dom.Document doc) {
        List<ParsedServiceDefinition> result = new ArrayList<>();
        var serviceNodes = doc.getElementsByTagName("service");
        for (int i = 0; i < serviceNodes.getLength(); i++) {
            var serviceEl = serviceNodes.item(i);
            if (serviceEl.getNodeType() != org.w3c.dom.Node.ELEMENT_NODE) continue;
            ParsedServiceDefinition def = parseOneService((org.w3c.dom.Element) serviceEl);
            if (def.getName() != null && !def.getName().isBlank()) {
                result.add(def);
            }
        }
        return result;
    }

    private ParsedServiceDefinition parseOneService(org.w3c.dom.Element serviceEl) {
        String name = getFirstChildText(serviceEl, "name");
        String description = getFirstChildText(serviceEl, "description");
        List<ParsedFieldDefinition> inputFields = new ArrayList<>();
        var inputList = serviceEl.getElementsByTagName("input");
        if (inputList.getLength() > 0) {
            var inputEl = (org.w3c.dom.Element) inputList.item(0);
            var fieldNodes = inputEl.getElementsByTagName("field");
            for (int j = 0; j < fieldNodes.getLength(); j++) {
                var fieldEl = (org.w3c.dom.Element) fieldNodes.item(j);
                String fmlname = getFirstChildText(fieldEl, "fmlname");
                if (fmlname == null || fmlname.isBlank()) continue;
                String type = getFirstChildText(fieldEl, "type");
                String occurence = getFirstChildText(fieldEl, "occurence");
                String defaultVal = getFirstChildText(fieldEl, "description-default");
                inputFields.add(new ParsedFieldDefinition(fmlname, type, occurence, defaultVal));
            }
        }
        return new ParsedServiceDefinition(name, description, inputFields);
    }

    private static String getFirstChildText(org.w3c.dom.Element parent, String tagName) {
        var list = parent.getElementsByTagName(tagName);
        if (list.getLength() == 0) return null;
        var el = list.item(0);
        String text = el.getTextContent();
        return text != null ? text.trim() : null;
    }

    /**
     * Mapuje typ z XML na FieldDataType.
     */
    public static JtuxedoServiceField.FieldDataType mapDataType(String xmlType) {
        if (xmlType == null || xmlType.isBlank()) return JtuxedoServiceField.FieldDataType.STRING;
        String t = xmlType.toLowerCase().replaceAll("\\s+", "");
        switch (t) {
            case "int":
            case "integer":
                return JtuxedoServiceField.FieldDataType.INTEGER;
            case "long":
                return JtuxedoServiceField.FieldDataType.LONG;
            case "decimal":
            case "number":
                return JtuxedoServiceField.FieldDataType.DECIMAL;
            case "date":
                return JtuxedoServiceField.FieldDataType.DATE;
            case "bool":
            case "boolean":
                return JtuxedoServiceField.FieldDataType.BOOLEAN;
            default:
                return JtuxedoServiceField.FieldDataType.STRING;
        }
    }

    /**
     * Mapuje occurrence z XML na FieldCardinality.
     */
    public static JtuxedoServiceField.FieldCardinality mapCardinality(String occurence) {
        if (occurence == null || occurence.isBlank()) return JtuxedoServiceField.FieldCardinality.ONE_TO_ONE;
        String o = occurence.trim().toLowerCase();
        if (o.contains("0") && o.contains("1") && !o.contains("n")) return JtuxedoServiceField.FieldCardinality.ZERO_TO_ONE;
        if ("1".equals(o) || o.startsWith("1..1") || o.contains("1 do 1")) return JtuxedoServiceField.FieldCardinality.ONE_TO_ONE;
        if ("n".equals(o) || o.contains("0..n") || o.contains("0 do n")) return JtuxedoServiceField.FieldCardinality.ZERO_TO_MANY;
        if (o.contains("1..n") || o.contains("1 do n")) return JtuxedoServiceField.FieldCardinality.ONE_TO_MANY;
        return JtuxedoServiceField.FieldCardinality.ONE_TO_ONE;
    }
}

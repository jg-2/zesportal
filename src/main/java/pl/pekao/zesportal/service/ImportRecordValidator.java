package pl.pekao.zesportal.service;

import pl.pekao.zesportal.entity.JtuxedoServiceField;
import pl.pekao.zesportal.entity.JtuxedoServiceField.FieldCardinality;
import pl.pekao.zesportal.entity.JtuxedoServiceField.FieldDataType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Walidacja rekordu importu – tylko dla pól obecnych w rekordzie (z definicji mapowań).
 * Definicja usługi nie ma znaczenia dla tego, co jest w rekordzie; liczy się wyłącznie definicja mapowań.
 * Sprawdzane są tylko: puste wartości (gdy pole jest w rekordzie), maxLength i typ dla pól z mapowania.
 */
public final class ImportRecordValidator {

    private static final DateTimeFormatter[] DATE_PARSERS = {
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("dd.MM.yyyy"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy")
    };

    private ImportRecordValidator() { }

    /**
     * Sprawdza tylko pola, które są w rekordzie (wynik mapowań). Nie zgłasza błędów za pola
     * wymagane w usłudze, ale nieobecne w mapowaniu – plik wczytuje się według definicji mapowań.
     *
     * @param record mapa: nazwa pola → wartość (tylko pola z mapowań)
     * @param fields definicje pól usługi (do sprawdzenia maxLength/typ dla pól z rekordu)
     * @return mapa: nazwa pola → komunikat błędu (tylko pola z błędami)
     */
    public static Map<String, String> validateRecord(Map<String, Object> record, List<JtuxedoServiceField> fields) {
        Map<String, String> errors = new HashMap<>();
        if (record == null) return errors;
        Map<String, JtuxedoServiceField> fieldsByName = fields != null
                ? fields.stream().filter(f -> f.getName() != null && !f.getName().isBlank())
                        .collect(Collectors.toMap(JtuxedoServiceField::getName, f -> f, (a, b) -> a))
                : Map.of();

        for (Map.Entry<String, Object> e : record.entrySet()) {
            String name = e.getKey();
            if (name == null || name.isBlank()) continue;
            Object val = e.getValue();
            String str = val != null ? val.toString().trim() : "";
            boolean empty = str.isEmpty();
            JtuxedoServiceField field = fieldsByName.get(name);

            if (field != null && (field.getCardinality() == FieldCardinality.ONE_TO_ONE || field.getCardinality() == FieldCardinality.ONE_TO_MANY)) {
                if (empty) {
                    errors.put(name, "Wymagane (pusta wartość w pliku)");
                    continue;
                }
            }
            if (empty) continue;

            if (field != null && field.getMaxLength() != null && field.getMaxLength() > 0 && str.length() > field.getMaxLength()) {
                errors.put(name, "Max " + field.getMaxLength() + " znaków");
                continue;
            }

            if (field != null) {
                FieldDataType type = field.getDataType();
                if (type != null && type != FieldDataType.STRING) {
                    String typeError = validateType(str, type);
                    if (typeError != null) errors.put(name, typeError);
                }
            }
        }
        return errors;
    }

    private static String validateType(String value, FieldDataType type) {
        switch (type) {
            case INTEGER:
                try {
                    Integer.parseInt(value);
                    return null;
                } catch (NumberFormatException ex) {
                    return "Nieprawidłowa liczba całkowita";
                }
            case LONG:
                try {
                    Long.parseLong(value);
                    return null;
                } catch (NumberFormatException ex) {
                    return "Nieprawidłowa liczba całkowita";
                }
            case DECIMAL:
                try {
                    new BigDecimal(value.replace(',', '.'));
                    return null;
                } catch (NumberFormatException ex) {
                    return "Nieprawidłowa liczba";
                }
            case DATE:
                for (DateTimeFormatter f : DATE_PARSERS) {
                    try {
                        LocalDate.parse(value, f);
                        return null;
                    } catch (DateTimeParseException ignored) { }
                }
                return "Nieprawidłowa data (np. yyyy-MM-dd lub dd.MM.yyyy)";
            case BOOLEAN:
                String v = value.toLowerCase();
                if ("true".equals(v) || "false".equals(v) || "1".equals(v) || "0".equals(v)
                        || "tak".equals(v) || "nie".equals(v) || "t".equals(v) || "n".equals(v)) {
                    return null;
                }
                return "Wartość logiczna (tak/nie, true/false, 1/0)";
            default:
                return null;
        }
    }
}

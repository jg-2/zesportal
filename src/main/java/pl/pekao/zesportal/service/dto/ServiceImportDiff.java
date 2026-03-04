package pl.pekao.zesportal.service.dto;

import java.util.List;

/**
 * Różnice między istniejącą definicją usługi a wersją z pliku XML (do podglądu przed nadpisaniem).
 */
public class ServiceImportDiff {

    private final boolean descriptionChanged;
    private final String descriptionOld;
    private final String descriptionNew;
    private final List<String> addedFieldNames;
    private final List<String> removedFieldNames;
    private final List<String> modifiedFieldDescriptions;

    public ServiceImportDiff(boolean descriptionChanged, String descriptionOld, String descriptionNew,
                             List<String> addedFieldNames, List<String> removedFieldNames,
                             List<String> modifiedFieldDescriptions) {
        this.descriptionChanged = descriptionChanged;
        this.descriptionOld = descriptionOld != null ? descriptionOld : "";
        this.descriptionNew = descriptionNew != null ? descriptionNew : "";
        this.addedFieldNames = addedFieldNames != null ? addedFieldNames : List.of();
        this.removedFieldNames = removedFieldNames != null ? removedFieldNames : List.of();
        this.modifiedFieldDescriptions = modifiedFieldDescriptions != null ? modifiedFieldDescriptions : List.of();
    }

    public boolean isDescriptionChanged() {
        return descriptionChanged;
    }

    public String getDescriptionOld() {
        return descriptionOld;
    }

    public String getDescriptionNew() {
        return descriptionNew;
    }

    public List<String> getAddedFieldNames() {
        return addedFieldNames;
    }

    public List<String> getRemovedFieldNames() {
        return removedFieldNames;
    }

    public List<String> getModifiedFieldDescriptions() {
        return modifiedFieldDescriptions;
    }

    public boolean hasAnyChanges() {
        return descriptionChanged || !addedFieldNames.isEmpty() || !removedFieldNames.isEmpty() || !modifiedFieldDescriptions.isEmpty();
    }
}

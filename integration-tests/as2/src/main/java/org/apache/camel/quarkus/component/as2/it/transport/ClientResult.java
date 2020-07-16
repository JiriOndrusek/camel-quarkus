package org.apache.camel.quarkus.component.as2.it.transport;

public class ClientResult {

    boolean isDispositionNotificationMultipartReportEntity;

    int partsCount;

    String secondPartClassName;

    public boolean isDispositionNotificationMultipartReportEntity() {
        return isDispositionNotificationMultipartReportEntity;
    }

    public void setDispositionNotificationMultipartReportEntity(boolean dispositionNotificationMultipartReportEntity) {
        isDispositionNotificationMultipartReportEntity = dispositionNotificationMultipartReportEntity;
    }

    public int getPartsCount() {
        return partsCount;
    }

    public void setPartsCount(int partsCount) {
        this.partsCount = partsCount;
    }

    public String getSecondPartClassName() {
        return secondPartClassName;
    }

    public void setSecondPartClassName(String secondPartClassName) {
        this.secondPartClassName = secondPartClassName;
    }
}

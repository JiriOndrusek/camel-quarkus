package org.apache.camel.quarkus.component.as2.it;

public class Result {

    boolean isDispositionNotificationMultipartReportEntity;

    int partsCount;

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
}

package org.apache.camel.quarkus.component.azure.storage.datalake.it;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import com.azure.storage.file.datalake.models.ListFileSystemsOptions;
import jakarta.enterprise.context.ApplicationScoped;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.azure.storage.datalake.DataLakeConstants;

@ApplicationScoped
public class AzureStorageDatalakeRoutes extends RouteBuilder {

    @Override
    public void configure() throws Exception {

        /* usage example */

        from("direct:downloadFile")
                .toD("azure-storage-datalake://${header.accountName}/${header.filesystemName}?fileName=${header.fileName}&serviceClient=#azureDatalakeServiceClient")
                .toD("file://target/test-files/?fileName=${header.fileName}");

        /* Producer examples */

        //listFileSystem
        from("direct:datalakeListFileSystem")
                .process(exchange -> {
                    //required headers can be added here
                    exchange.getIn().setHeader(DataLakeConstants.LIST_FILESYSTEMS_OPTIONS,
                            new ListFileSystemsOptions().setMaxResultsPerPage(10));
                })
                .toD("azure-storage-datalake://${header.accountName}/${header.filesystemName}?operation=listFileSystem&serviceClient=#azureDatalakeServiceClient");

        //createFileSystem
        from("direct:datalakeCreateFilesystem")
                .toD("azure-storage-datalake://${header.accountName}?operation=createFileSystem&serviceClient=#azureDatalakeServiceClient");

        //listPaths
        from("direct:datalakeListPaths")
                .toD("azure-storage-datalake://${header.accountName}/${header.filesystemName}?operation=listPaths&serviceClient=#azureDatalakeServiceClient");

        //getFile
        from("direct:datalakeGetFile")
                .toD("azure-storage-datalake://${header.accountName}/${header.filesystemName}?operation=getFile&fileName=test.txt&serviceClient=#azureDatalakeServiceClient");

        //deleteFile
        from("direct:datalakeDeleteFile")
                .toD("azure-storage-datalake://${header.accountName}/${header.filesystemName}?operation=deleteFile&fileName=test.txt&serviceClient=#azureDatalakeServiceClient");

        //downloadToFile
        from("direct:datalakeDownloadToFile")
                .toD("azure-storage-datalake://${header.accountName}/${header.filesystemName}?operation=downloadToFile&fileName=test.txt&fileDir=target/operation-files&serviceClient=#azureDatalakeServiceClient");

        //downloadLink
        from("direct:datalakeDownloadLink")
                .toD("azure-storage-datalake://${header.accountName}/${header.filesystemName}?operation=downloadLink&fileName=test.txt&serviceClient=#azureDatalakeServiceClient");

        //appendToFile
        from("direct:datalakeAppendToFile")
                .process(exchange -> {
                    final String data = exchange.getIn().getHeader("append", String.class);
                    final InputStream inputStream = new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));
                    exchange.getIn().setBody(inputStream);
                })
                .toD("azure-storage-datalake://${header.accountName}/${header.filesystemName}?operation=appendToFile&fileName=test.txt&serviceClient=#azureDatalakeServiceClient");

        //flushToFile
        from("direct:datalakeFlushToFile")
                .process(exchange -> {
                    exchange.getIn().setHeader(DataLakeConstants.POSITION, 0);
                })
                .toD("azure-storage-datalake://${header.accountName}/${header.filesystemName}?operation=flushToFile&fileName=test.txt&serviceClient=#azureDatalakeServiceClient");

        //upload
        from("direct:datalakeUpload")
                .process(exchange -> {
                    final String data = "1";
                    final InputStream inputStream = new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));
                    exchange.getIn().setBody(inputStream);
                })
                .toD("azure-storage-datalake://${header.accountName}/${header.filesystemName}?operation=upload&fileName=test.txt&serviceClient=#azureDatalakeServiceClient");
    }
}

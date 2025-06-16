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
                .toF("azure-storage-datalake://${header.accountName}/${header.filesystemName}?operation=listFileSystem&serviceClient=#azureDatalakeServiceClient");

        //createFileSystem
        from("direct:datalakeCreateFilesystem")
                .toF("azure-storage-datalake://${header.accountName}?operation=createFileSystem&serviceClient=#azureDatalakeServiceClient");

        //upload
        from("direct:datalakeUpload")
                .process(exchange -> {
                    final String data = "Uploaded by Camel!";
                    final InputStream inputStream = new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));
                    exchange.getIn().setBody(inputStream);
                })
                .toF("azure-storage-datalake://${header.accountName}/${header.filesystemName}?operation=upload&fileName=test.txt&serviceClient=#azureDatalakeServiceClient");
    }
}

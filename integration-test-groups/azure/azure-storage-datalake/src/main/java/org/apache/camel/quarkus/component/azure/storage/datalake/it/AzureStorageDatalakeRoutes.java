package org.apache.camel.quarkus.component.azure.storage.datalake.it;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import com.azure.storage.file.datalake.models.ListFileSystemsOptions;
import com.azure.storage.file.datalake.options.FileQueryOptions;
import jakarta.enterprise.context.ApplicationScoped;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.azure.storage.datalake.DataLakeConstants;

@ApplicationScoped
public class AzureStorageDatalakeRoutes extends RouteBuilder {

    public static String FILE_CONTENT = "Hello World!" + UUID.randomUUID();
    public static String FILE_NAME = "operations.txt";
    public static String FILE_NAME2 = "test/file.txt";
    private static String CLIENT_SUFFIX = "&serviceClient=#azureDatalakeServiceClient";

    @Override
    public void configure() throws Exception {

        /* usage example */

        from("direct:downloadFile")
                .toD("azure-storage-datalake://${header.accountName}/${header.filesystemName}?fileName=${header.fileName}"
                        + CLIENT_SUFFIX)
                .toD("file://target/test-files/?fileName=${header.fileName}");

        /* Producer examples */

        //listFileSystem
        from("direct:datalakeListFileSystem")
                .process(exchange -> {
                    //required headers can be added here
                    exchange.getIn().setHeader(DataLakeConstants.LIST_FILESYSTEMS_OPTIONS,
                            new ListFileSystemsOptions().setMaxResultsPerPage(10));
                })
                .toD("azure-storage-datalake://${header.accountName}/${header.filesystemName}?operation=listFileSystem"
                        + CLIENT_SUFFIX);

        //createFileSystem
        from("direct:datalakeCreateFilesystem")
                .toD("azure-storage-datalake://${header.accountName}?operation=createFileSystem" + CLIENT_SUFFIX);

        //listPaths
        from("direct:datalakeListPaths")
                .toD("azure-storage-datalake://${header.accountName}/${header.filesystemName}?operation=listPaths"
                        + CLIENT_SUFFIX);

        //getFile
        from("direct:datalakeGetFile")
                .toD("azure-storage-datalake://${header.accountName}/${header.filesystemName}?operation=getFile&fileName=${header.fileName}"
                        + CLIENT_SUFFIX);

        //deleteFile
        from("direct:datalakeDeleteFile")
                .toD("azure-storage-datalake://${header.accountName}/${header.filesystemName}?operation=deleteFile&fileName="
                        + FILE_NAME + CLIENT_SUFFIX);

        //downloadToFile
        from("direct:datalakeDownloadToFile")
                .toD("azure-storage-datalake://${header.accountName}/${header.filesystemName}?operation=downloadToFile&fileName="
                        + FILE_NAME + "&fileDir=target/operation-files" + CLIENT_SUFFIX);

        //downloadLink
        from("direct:datalakeDownloadLink")
                .toD("azure-storage-datalake://${header.accountName}/${header.filesystemName}?operation=downloadLink&fileName="
                        + FILE_NAME + CLIENT_SUFFIX);

        //appendToFile
        from("direct:datalakeAppendToFile")
                .process(exchange -> {
                    final String data = exchange.getIn().getHeader("append", String.class);
                    final InputStream inputStream = new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));
                    exchange.getIn().setBody(inputStream);
                })
                .toD("azure-storage-datalake://${header.accountName}/${header.filesystemName}?operation=appendToFile&fileName="
                        + FILE_NAME + CLIENT_SUFFIX);

        //flushToFile
        from("direct:datalakeFlushToFile")
                .process(exchange -> {
                    exchange.getIn().setHeader(DataLakeConstants.POSITION, 8);
                })
                .toD("azure-storage-datalake://${header.accountName}/${header.filesystemName}?operation=flushToFile&fileName="
                        + FILE_NAME + CLIENT_SUFFIX);

        //openQueryInputStream
        from("direct:openQueryInputStream")
                .process(exchange -> {
                    exchange.getIn().setHeader(DataLakeConstants.QUERY_OPTIONS,
                            new FileQueryOptions("SELECT * from BlobStorage"));
                })
                .toD("azure-storage-datalake://${header.accountName}/${header.filesystemName}?operation=openQueryInputStream&fileName="
                        + FILE_NAME + CLIENT_SUFFIX);

        //upload
        from("direct:datalakeUpload")
                .process(exchange -> {
                    final InputStream inputStream = new ByteArrayInputStream(FILE_CONTENT.getBytes(StandardCharsets.UTF_8));
                    exchange.getIn().setBody(inputStream);
                })
                .toD("azure-storage-datalake://${header.accountName}/${header.filesystemName}?operation=upload&fileName="
                        + FILE_NAME + CLIENT_SUFFIX);

        // uploadFromFile
        from("direct:datalakeUploadFromFile")
                .toD("azure-storage-datalake://${header.accountName}/${header.filesystemName}?operation=uploadFromFile&fileName="
                        + FILE_NAME2 + CLIENT_SUFFIX);

        // createFile
        from("direct:datalakeCreateFile")
                .toD("azure-storage-datalake://${header.accountName}/${header.filesystemName}?operation=createFile&fileName=${header.fileName}"
                        + CLIENT_SUFFIX);

        //        //deleteDirectory
        //        from("direct:datalakeDeleteDirectory")
        //                .toD("azure-storage-datalake://${header.accountName}/${header.filesystemName}?operation=deleteDirectory"
        //                        + CLIENT_SUFFIX);
    }
}

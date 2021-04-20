package org.apache.camel.quarkus.component.google.storage;

import java.io.IOException;

import com.google.api.client.googleapis.services.AbstractGoogleClient;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.UriTemplate;
import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Substitute;

//@TargetClass(value = AbstractGoogleClientRequest.class)
public final class AbstractGoogleClientRequestSubstitute {

    //    @Alias
    //    private MediaHttpUploader uploader;
    //
    //    @Alias
    //    private HttpRequest buildHttpRequest(boolean usingHead) throws IOException {
    //        return null;
    //    }
    //
    //    @Alias
    //    public GenericUrl buildHttpRequestUrl() {
    //        return null;
    //    }
    @Alias
    private AbstractGoogleClient abstractGoogleClient;
    @Alias
    private String uriTemplate;

    @Alias
    private HttpResponse executeUnparsed(boolean usingHead) throws IOException {
        //        HttpResponse response;
        //        if (uploader == null) {
        //            // normal request (not upload)
        //            response = buildHttpRequest(usingHead).execute();
        //        } else {
        //            // upload request
        //            GenericUrl httpRequestUrl = buildHttpRequestUrl();
        //            HttpRequest httpRequest =
        //                    getAbstractGoogleClient()
        //                            .getRequestFactory()
        //                            .buildRequest(requestMethod, httpRequestUrl, httpContent);
        //            boolean throwExceptionOnExecuteError = httpRequest.getThrowExceptionOnExecuteError();
        //
        //            response =
        //                    uploader
        //                            .setInitiationHeaders(requestHeaders)
        //                            .setDisableGZipContent(disableGZipContent)
        //                            .upload(httpRequestUrl);
        //            response.getRequest().setParser(getAbstractGoogleClient().getObjectParser());
        //            // process any error
        //            if (throwExceptionOnExecuteError && !response.isSuccessStatusCode()) {
        //                throw newExceptionOnError(response);
        //            }
        //        }
        //        // process response
        //        lastResponseHeaders = response.getHeaders();
        //        lastStatusCode = response.getStatusCode();
        //        lastStatusMessage = response.getStatusMessage();
        //        return response;
        return null;
    }

    @Substitute
    public GenericUrl buildHttpRequestUrl() {
        System.out.println("11_11_11_11_11 this:" + this);
        String s = UriTemplate.expand(abstractGoogleClient.getBaseUrl(), uriTemplate, this, true);
        System.out.println("11_11_11_11_11 url:" + s);
        return new GenericUrl(s);
    }

    @Substitute
    public HttpResponse executeUnparsed() throws IOException {
        HttpResponse reso = null;
        try {
            System.out.println("1010101001010 start");
            reso = executeUnparsed(false);
            System.out.println("1010101001010 response" + reso.getStatusCode());
            return reso;
        } catch (Exception e) {
            System.out.println("1010101001010 exception: " + e);
            e.printStackTrace();
            throw e;
        }

        // for debugging purposes - to show content
        //        String s = null;
        //        try {
        //             s = new BufferedReader(
        //                    new InputStreamReader(reso.getContent(), StandardCharsets.UTF_8))
        //                    .lines()
        //                    .collect(Collectors.joining("\n"));
        //        } catch (Exception e) {
        //            System.out.println("1010101001010 2exception2: " + e);
        //            e.printStackTrace();
        //        }
        //        System.out.println("101010101010 content: " + s);

        //        System.out.println("1010101001010 returning: " + reso);
        //        return reso;
    }
}

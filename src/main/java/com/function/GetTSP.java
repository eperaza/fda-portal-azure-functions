package com.function;

import com.azure.identity.ManagedIdentityCredential;
import com.azure.identity.ManagedIdentityCredentialBuilder;
import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.SecretClientBuilder;
import com.azure.security.keyvault.secrets.models.KeyVaultSecret;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;
import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.BlobProperties;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.commons.lang3.StringUtils;

import com.function.utils.Constants;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.util.Date;
import java.util.Optional;

/**
 * Azure Functions with HTTP Trigger.
 */
public class GetTSP {

    private final Logger logger = LoggerFactory.getLogger(GetTSP.class);

    public final static String TSP_CONFIG_ZIP_CONTAINER = "aircraft-config-package";
    /**
     * This function listens at endpoint "/api/getTSP". Two ways to invoke it
     * using "curl" command in bash:
     * 1. curl -d "HTTP Body" {your host}/api/HttpExample
     * 2. curl "{your host}/api/HttpExample?name=HTTP%20Query"
     * 
     * @throws IOException
     * @throws URISyntaxException
     * @throws StorageException
     */
    @FunctionName("getTSP")
    public HttpResponseMessage run(
            @HttpTrigger(name = "req", methods = { HttpMethod.GET,
                    HttpMethod.POST }, authLevel = AuthorizationLevel.FUNCTION) HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) throws IOException, StorageException, URISyntaxException {
        context.getLogger().info("Java HTTP trigger processed a request.");

        // Parse query parameter
        final String query = request.getQueryParameters().get("airline");
        final String airline = request.getBody().orElse(query);

        if (airline == null) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("Please pass a name on the query string or in the request body").build();
        } else {

            String res = getTSP(airline);
            return request.createResponseBuilder(HttpStatus.OK).body(res).build();
        }
    }

    public String getTSP(String airline) throws IOException, StorageException, URISyntaxException {
        Date lastModified = null;

        String airlineGroup = airline.replace(Constants.AAD_GROUP_AIRLINE_PREFIX, StringUtils.EMPTY);

        String containerName = TSP_CONFIG_ZIP_CONTAINER;
        String fileName = new StringBuilder(airline).append("/").append(airline).append("-config-pkg.zip")
                .toString();
        logger.debug("filename is: {}", fileName);
        CloudStorageAccount storageAccount = getcloudStorageAccount();

        CloudBlobClient serviceClient = storageAccount.createCloudBlobClient();
        CloudBlobContainer container = serviceClient.getContainerReference(containerName);
        CloudBlockBlob blob = container.getBlockBlobReference(fileName);
        // NOTE: MUST call downloadAttributes or your properties will return null
        blob.downloadAttributes();
        BlobProperties blobProps = blob.getProperties();

        lastModified = blobProps.getLastModified();

        return lastModified.toString();

    }

    private CloudStorageAccount getcloudStorageAccount() {
        try {
            return CloudStorageAccount.parse(System.getenv("STORAGE_CONNECTION_STRING"));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException();
        } catch (URISyntaxException urise) {
            throw new IllegalArgumentException();
        } catch (InvalidKeyException e) {
            throw new IllegalArgumentException();
        }
    }
}

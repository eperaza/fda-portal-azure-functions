package com.function;

import com.microsoft.azure.functions.annotation.*;
import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.CloudBlob;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.specialized.BlockBlobClient;
import com.function.pojos.EventSchema;
import com.function.pojos.OutputEventSchemaData;
import com.google.common.base.Charsets;
import com.microsoft.azure.functions.*;

/**
 * Azure Functions with Event Grid trigger.
 */
public class SetupMobileConfig {
    /**
     * This function will be invoked when an event is received from Event Grid.
     * 
     * @throws URISyntaxException
     * @throws InvalidKeyException
     * @throws StorageException
     * @throws IOException
     */
    @FunctionName("setupMobileConfig")
    public void run(
            @EventGridTrigger(name = "eventGridEvent") EventSchema event,
            @EventGridOutput(name = "outputEvent", topicEndpointUri = "TOPIC_STORAGE_SETUP_STATUS", topicKeySetting = "TOPIC_KEY_SETTING") OutputBinding<EventSchema> outputEvent,
            final ExecutionContext context)
            throws InvalidKeyException, URISyntaxException, StorageException, IOException {
        context.getLogger().info("Java Event Grid trigger function executed.");
        
        final String AIRLINE_ONBOARDING_AUTOMATION_CONTAINER = System.getenv("AIRLINE_ONBOARDING_AUTOMATION_CONTAINER");
        String file = event.getSubject().replace("/blobServices/default/containers/" + AIRLINE_ONBOARDING_AUTOMATION_CONTAINER + "/blobs/", "");
        String airline = file.replace(".zip", "");
        context.getLogger().info("File dropped: " + file);

        CloudStorageAccount storageAccount = CloudStorageAccount.parse(System.getenv("STORAGE_CONNECTION_STRING"));
        CloudBlobClient blobClient = storageAccount.createCloudBlobClient();
        CloudBlobContainer container = blobClient.getContainerReference(AIRLINE_ONBOARDING_AUTOMATION_CONTAINER);
        CloudBlob blob = container.getBlockBlobReference(file);
        InputStream input = blob.openInputStream();
        Map<String, Object> data = new HashMap<>();
        OutputEventSchemaData dataContent = new OutputEventSchemaData();
        
        Boolean success = readMobileConfig(input, context, airline);
        final EventSchema eventGridOutputDocument = new EventSchema();

        eventGridOutputDocument.setId(UUID.randomUUID().toString());
        eventGridOutputDocument.setEventType("mobileConfigUploaded");
        eventGridOutputDocument.setEventTime(new Date(System.currentTimeMillis()));
        eventGridOutputDocument.setDataVersion("1.0");
        eventGridOutputDocument.setSubject("Mobile Configuration Status");

        if (success) {
            dataContent.setMessage("Mobile Configuration setup successfully");
            dataContent.setCode(200);
        }
        else{
            dataContent.setMessage("Mobile Configuration setup error");
            dataContent.setCode(500);
        }
        data.put("content", dataContent);
        eventGridOutputDocument.setData(data);

        outputEvent.setValue(eventGridOutputDocument);

    }

    public Boolean readMobileConfig(InputStream in, ExecutionContext context, String airline) throws IOException {

        // Create a BlobServiceClient object which will be used to create a container
        // client
        BlobServiceClient blobServiceClient = new BlobServiceClientBuilder()
                .connectionString(System.getenv("STORAGE_CONNECTION_STRING")).buildClient();

        // Create a unique name for the container
        String containerName = "config";

        // Create the container and return a container client object
        BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);
        String fileName = airline + ".mobileconfig";

        BlockBlobClient blobClient = containerClient.getBlobClient(fileName).getBlockBlobClient();

        ZipInputStream zipIn = new ZipInputStream(in);
        ZipEntry entry;
        try {
            while ((entry = zipIn.getNextEntry()) != null) {
                if (entry.getName().contains(airline + ".mobileconfig")) {
                    context.getLogger().info("found mobile config");
                    InputStreamReader isr = new InputStreamReader(zipIn, Charsets.UTF_8);
                    BufferedReader reader = new BufferedReader(isr);
                    String line;

                    StringBuilder sb = new StringBuilder();

                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                        sb.append(System.lineSeparator());
                    }

                    String output = sb.toString();
                    InputStream dataStream = new ByteArrayInputStream(output.getBytes(StandardCharsets.UTF_8));

                    blobClient.upload(dataStream, output.length(), true);

                }

                zipIn.closeEntry();
            }
        } catch (Exception e) {
            context.getLogger().info("Error: " + e);
            return false;
        }
        return true;
    }

}

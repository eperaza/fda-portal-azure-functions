package com.function;

import com.microsoft.azure.functions.annotation.*;
import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.CloudBlob;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.apache.commons.io.IOUtils;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.specialized.BlockBlobClient;
import com.function.pojos.AirlineSetupSettings;
import com.function.pojos.EventSchema;
import com.function.pojos.OutputEventSchemaData;
import com.google.common.base.Charsets;
import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.microsoft.azure.functions.*;

/**
 * Azure Functions with Event Grid trigger.
 */
public class SetupRegistrationInstructions {
    /**
     * This function will be invoked when an event is received from Event Grid.
     * 
     * @throws URISyntaxException
     * @throws InvalidKeyException
     * @throws StorageException
     * @throws IOException
     */
    @FunctionName("setupRegistrationInstructions")
    public void run(
            @EventGridTrigger(name = "eventGridEvent") EventSchema event,
            @EventGridOutput(name = "outputEvent", topicEndpointUri = "TOPIC_STORAGE_SETUP_STATUS", topicKeySetting = "TOPIC_KEY_SETTING") OutputBinding<EventSchema> outputEvent,
            final ExecutionContext context)
            throws InvalidKeyException, URISyntaxException, StorageException, IOException {
        context.getLogger().info("Java Event Grid trigger function executed.");

        final String AIRLINE_ONBOARDING_AUTOMATION_CONTAINER = System.getenv("AIRLINE_ONBOARDING_AUTOMATION_CONTAINER");
        String file = event.getSubject()
                .replace("/blobServices/default/containers/" + AIRLINE_ONBOARDING_AUTOMATION_CONTAINER + "/blobs/", "");
        String airline = file.replace(".zip", "");
        context.getLogger().info("File dropped: " + file);

        CloudStorageAccount storageAccount = CloudStorageAccount.parse(System.getenv("STORAGE_CONNECTION_STRING"));
        CloudBlobClient blobClient = storageAccount.createCloudBlobClient();
        CloudBlobContainer container = blobClient.getContainerReference(AIRLINE_ONBOARDING_AUTOMATION_CONTAINER);
        CloudBlob blob = container.getBlockBlobReference(file);
        InputStream input = blob.openInputStream();
        Map<String, Object> data = new HashMap<>();
        OutputEventSchemaData dataContent = new OutputEventSchemaData();

        Boolean success = readInstructions(input, context, airline);
        final EventSchema eventGridOutputDocument = new EventSchema();

        eventGridOutputDocument.setId(UUID.randomUUID().toString());
        eventGridOutputDocument.setEventType("registrationInstructionsUploaded");
        eventGridOutputDocument.setEventTime(new Date(System.currentTimeMillis()));
        eventGridOutputDocument.setDataVersion("1.0");
        eventGridOutputDocument.setSubject("Registration Instructions Status");

        if (success) {
            dataContent.setMessage("Registration Instructions setup successfully");
            dataContent.setCode(200);
        } else {
            dataContent.setMessage("Registration Instructions setup error");
            dataContent.setCode(500);
        }
        data.put("content", dataContent);
        eventGridOutputDocument.setData(data);

        outputEvent.setValue(eventGridOutputDocument);

    }

    public Boolean readInstructions(InputStream in, ExecutionContext context, String airline) throws IOException {

        // Create a BlobServiceClient object which will be used to create a container
        // client
        BlobServiceClient blobServiceClient = new BlobServiceClientBuilder()
                .connectionString(System.getenv("STORAGE_CONNECTION_STRING")).buildClient();
        ZipInputStream zipIn = new ZipInputStream(in);
        ZipEntry entry;

        byte[] bytes = null;
        Gson gson = new Gson();
        AirlineSetupSettings settings = null;

        while ((entry = zipIn.getNextEntry()) != null) {
            if (entry.getName().contains("settings")) {
                context.getLogger().info("found settings");
                InputStreamReader isr = new InputStreamReader(zipIn, Charsets.UTF_8);
                JsonReader reader = new JsonReader(isr);
                settings = gson.fromJson(reader, AirlineSetupSettings.class);
                context.getLogger().info("is .mp?:" + settings.getIsMPForRegistration());
            }
            if (entry.getName().contains("instructions")) {
                context.getLogger().info("found instructions");
                bytes = IOUtils.toByteArray(zipIn);
            }
            zipIn.closeEntry();
        }

        String containerNameOld = "email-instructions/" + airline + "/";
        String containerNameNew = "email-new-instructions/" + airline + "/";

        List<String> containers = new ArrayList<>();
        containers.add(containerNameOld);
        containers.add(containerNameNew);

        for (String container : containers) {
            // Create the container and return a container client object
            BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(container);
            String fileName = "FDA_registration_instructions.pdf";

            BlockBlobClient blobClient = containerClient.getBlobClient(fileName).getBlockBlobClient();
            InputStream dataStream = new ByteArrayInputStream(bytes);

            try {
                blobClient.upload(dataStream, bytes.length, true);
            } catch (Exception e) {
                return false;
            }
        }
        return true;
    }

}

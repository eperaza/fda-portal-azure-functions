package com.function;

import com.microsoft.azure.functions.annotation.*;
import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.CloudBlob;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.function.pojos.User;
import com.google.common.base.Charsets;
import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.microsoft.azure.functions.*;

/**
 * Azure Functions with Azure Blob trigger.
 */
public class PilotNoteParse {
    /**
     * This function will be invoked when a new or updated blob is detected at the
     * specified path. The blob contents are provided as input to this function.
     * 
     * @throws StorageException
     * @throws URISyntaxException
     * @throws InvalidKeyException
     * @throws IOException
     */
    @FunctionName("pilotNoteParse")
    @StorageAccount("AzureWebJobsStorage")
    public void run(
            @BlobTrigger(name = "content", path = "samples/{name}", dataType = "binary", connection = "AzureWebJobsStorage") byte[] content,
            @BindingName("name") String name,
            final ExecutionContext context)
            throws StorageException, InvalidKeyException, URISyntaxException, IOException {
        String conn = "DefaultEndpointsProtocol=https;AccountName=fdagroundstorage;AccountKey=Dyc4CYOc/rdAe4PcjwqUB2JjbA9unnqNSQiBcCIx2dYlUwK9ykqlMTg3o/mmRs+JZX1maaoIT7csFuXRpTCz9g==;EndpointSuffix=core.windows.net";
        CloudStorageAccount storageAccount = CloudStorageAccount.parse(conn);
        CloudBlobClient blobClient = storageAccount.createCloudBlobClient();
        CloudBlobContainer container = blobClient.getContainerReference("samples");
        CloudBlob blob = container.getBlockBlobReference(name);
        InputStream input = blob.openInputStream();
        readZipStream(input);
        /*
         * InputStreamReader inr = new InputStreamReader(input, "UTF-8");
         * String utf8str = IOUtils.toString(inr);
         * context.getLogger().info(utf8str);
         */
        context.getLogger().info("Java Blob trigger function processed a blob. Name: " + name + "\n  Size: "
                + content.length + " Bytes");
    }

    public void readZipStream(InputStream in) throws IOException {
        ZipInputStream zipIn = new ZipInputStream(in);
        ZipEntry entry;
        Gson gson = new Gson();

        while ((entry = zipIn.getNextEntry()) != null) {
            if (entry.getName().contains("user.json")) {
                InputStreamReader isr = new InputStreamReader(zipIn, Charsets.UTF_8);
                JsonReader reader = new JsonReader(isr);
                User manifestJson = gson.fromJson(reader, User.class);
                System.out.println(manifestJson.getState());
                System.out.println("manifest: " + manifestJson.toString());
            }
            // System.out.println(entry.getName());
            // readContents(zipIn);
            zipIn.closeEntry();
        }
    }

    private void readContents(InputStream contentsIn) throws IOException {
        byte contents[] = new byte[4096];
        int direct;
        while ((direct = contentsIn.read(contents, 0, contents.length)) >= 0) {
            System.out.println("Read " + direct + "bytes content.");
        }
    }
}

package com.function;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.microsoft.azure.functions.annotation.*;
import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.CloudBlob;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.function.pojos.CruiseData;
import com.function.pojos.TSP;
import com.google.common.base.Charsets;
import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.microsoft.azure.functions.*;

/**
 * Azure Functions with HTTP Trigger.
 */
public class GetOptimalCI {
    /**
     * This function listens at endpoint "/api/GetOptimalCI". Two ways to invoke it
     * using "curl" command in bash:
     * 1. curl -d "HTTP Body" {your host}/api/GetOptimalCI
     * 2. curl {your host}/api/GetOptimalCI?name=HTTP%20Query
     * 
     * @throws StorageException
     * @throws URISyntaxException
     * @throws InvalidKeyException
     * @throws IOException
     */
    @FunctionName("getOptimalCI")
    public HttpResponseMessage run(
            @HttpTrigger(name = "req", methods = { HttpMethod.GET,
                    HttpMethod.POST }, authLevel = AuthorizationLevel.FUNCTION) HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context)
            throws URISyntaxException, InvalidKeyException, IOException, StorageException {
        context.getLogger().info("Java HTTP trigger processed a request.");

        String data = request.getBody().orElse(null);
        CruiseData cruiseData = new Gson().fromJson(data, CruiseData.class);

        Double optimalCI = calculateOptimal(cruiseData, context);

        return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                .body(optimalCI).build();

    }

    private Double calculateOptimal(CruiseData data, ExecutionContext context) throws InvalidKeyException, URISyntaxException, StorageException, IOException{
        TSP tsp = getTSP(data.getAirline(), data.getTail(), context);
        Double etheta101 = null;

        //try to get etheta101 from TSP first if not exists get it from InfltDB
        if (tsp.getETHETA101() != null){
            etheta101 = tsp.getETHETA101();
            context.getLogger().info("ETHETA101 from TSP for tail " + data.getTail() + "is: " + tsp.getETHETA101());
        }
        else{
            etheta101 = getEtheta101(tsp.getInfltDB(), context);
        }
         
        Double missionQstarMRC = (tsp.getQstarMRC()  * Math.pow(10, 6) + data.getCostIndex() * tsp.getdQstar_dCI()) / Math.pow(10, 6);
        Double delta = calculateDeltaCelsius(data.getAltitude()*100);
        Double theta = calculateThetaCelsius(data.getStaticAirTemperature());

        context.getLogger().info("theta: " + theta); 
        context.getLogger().info("delta: " + delta);
        context.getLogger().info("missionQstarMRC: " + missionQstarMRC);


        return missionQstarMRC * (data.getGrossWeight()*1000 / delta) / Math.pow(theta, etheta101);

    }

    private TSP getTSP(String airline, String tail, ExecutionContext context)
            throws InvalidKeyException, URISyntaxException, StorageException, IOException {
        InputStream input = null;

        try {
            String containerName = System.getenv("TSP_CONFIG_ZIP_CONTAINER");
            String fileName = new StringBuilder(airline).append("/").append(airline).append("-config-pkg.zip")
                    .toString();
            String conn = System.getenv("STORAGE_URL");
            CloudStorageAccount storageAccount = CloudStorageAccount.parse(conn);
            CloudBlobClient blobClient = storageAccount.createCloudBlobClient();
            CloudBlobContainer container = blobClient.getContainerReference(containerName);
            CloudBlob blob = container.getBlockBlobReference(fileName);
            input = blob.openInputStream();

        } catch (InvalidKeyException ike) {
            ike.printStackTrace();
        } catch (URISyntaxException urise) {
            urise.printStackTrace();
        } catch (StorageException se) {
            se.printStackTrace();
        }

        return readZipStream(input, tail, context);

    }

    private Double getEtheta101(String infltdb, ExecutionContext context) throws URISyntaxException, StorageException, InvalidKeyException {
        BufferedReader br = null;
        String line = null;
        Double etheta101 = null;

        try {
            String conn = System.getenv("STORAGE_URL");
            CloudStorageAccount storageAccount = CloudStorageAccount.parse(conn);
            CloudBlobClient blobClient = storageAccount.createCloudBlobClient();
            CloudBlobContainer container = blobClient.getContainerReference("infltdb");
            CloudBlob blob = container.getBlockBlobReference(infltdb);
            InputStream input = blob.openInputStream();
            Reader reader = new InputStreamReader(input);
            br = new BufferedReader(reader);
            while ((line = br.readLine()) != null) {
                if (line.contains("ETHETA101")) {
                    line = line.replaceAll(" ", "");
                    int equalsIndex = line.lastIndexOf("=");
                    line = line.substring(equalsIndex + 1);
                    context.getLogger().info("ETHETA101 for InfltDB " + infltdb + ": " + line);
                    etheta101 = new Double(line);
                    return etheta101;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (br != null)
                    br.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return etheta101;
    }

    public TSP readZipStream(InputStream in, String tail, ExecutionContext context) throws IOException {
        ZipInputStream zipIn = new ZipInputStream(in);
        ZipEntry entry;
        Gson gson = new Gson();
        TSP tsp = null;

        while ((entry = zipIn.getNextEntry()) != null) {
            if (entry.getName().contains(tail + ".json")) {
                InputStreamReader isr = new InputStreamReader(zipIn, Charsets.UTF_8);
                JsonReader reader = new JsonReader(isr);
                tsp = gson.fromJson(reader, TSP.class);
                context.getLogger().info("InfltDB for tail " + tail + " is: " + tsp.getInfltDB());
                context.getLogger().info("QstarMRC for tail " + tail + " is: " + tsp.getQstarMRC());
                context.getLogger().info("dQstar_dCI for tail " + tail + " is: " + tsp.getdQstar_dCI());
            }

            zipIn.closeEntry();
        }
        return tsp;
    }

    private Double calculateDeltaCelsius(int altitude){
        Double limitAltitude = new Double(36089);
        if (altitude <= limitAltitude) {
            return Math.pow((288.15 - 0.0019812 * altitude) / 288.15, 5.25588);
        }
        return 0.22336 * Math.exp((limitAltitude - altitude) / 20805.7);
    }

    private Double calculateThetaCelsius(int staticAirTemperature){
        return (staticAirTemperature + 273.15) / 288.15;
    }
}

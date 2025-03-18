package solid;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import cartago.Artifact;
import cartago.OPERATION;
import cartago.OpFeedbackParam;

/**
 * A CArtAgO artifact that agent can use to interact with LDP containers in a Solid pod.
 */
public class Pod extends Artifact {

    private String podURL; // the location of the Solid pod 

  /**
   * Method called by CArtAgO to initialize the artifact. 
   *
   * @param podURL The location of a Solid pod
   */
    public void init(String podURL) {
        this.podURL = podURL;
        log("Pod artifact initialized for: " + this.podURL);
    }

  /**
   * CArtAgO operation for creating a Linked Data Platform container in the Solid pod
   *
   * @param containerName The name of the container to be created
   * 
   */
    @OPERATION
    public void createContainer(String containerName) {
        // Construct the container URL: ensure podURL ends with "/" then append containerName + "/"
        String base = podURL.endsWith("/") ? podURL : podURL + "/";
        String containerURL = base + containerName + "/";
        log("Creating container at: " + containerURL);

        try {
            HttpURLConnection con = (HttpURLConnection) new URL(containerURL).openConnection();
            con.setRequestMethod("HEAD");
            if (con.getResponseCode() == HttpURLConnection.HTTP_OK) {
                log("Container exists: " + containerURL);
                return;
            }
        } catch (Exception e) {
            log("Container not found; creating it now.");
        }
        

        // Create the container with a PUT request (using an empty turtle payload)
        try {
            HttpURLConnection putCon = (HttpURLConnection) new URL(containerURL).openConnection();
            putCon.setDoOutput(true);
            putCon.setRequestMethod("PUT");
            putCon.setRequestProperty("Content-Type", "text/turtle");
            // with a empty payload.
            try (OutputStream os = putCon.getOutputStream()) { }
            int code = putCon.getResponseCode();
            if (code == HttpURLConnection.HTTP_CREATED || code == HttpURLConnection.HTTP_OK) {
                log("Container created successfully at: " + containerURL);
            } else {
                log("Failed to create container. HTTP response code: " + code);
            }
        } catch (Exception e) {
            log("Error during container creation: " + e.getMessage());
        }
        
    }

  /**
   * CArtAgO operation for publishing data within a .txt file in a Linked Data Platform container of the Solid pod
   * 
   * @param containerName The name of the container where the .txt file resource will be created
   * @param fileName The name of the .txt file resource to be created in the container
   * @param data An array of Object data that will be stored in the .txt file
   */
    @OPERATION
    public void publishData(String containerName, String fileName, Object[] data) {
        // Construct the resource URL: podURL/containerName/fileName
        String base = podURL.endsWith("/") ? podURL : podURL + "/";
        String resourceURL = base + containerName + "/" + fileName;
        log("Publishing data to: " + resourceURL);
    
        // Convert the data array to a string
        String payload = createStringFromArray(data);
    
        try {
            HttpURLConnection con = (HttpURLConnection) new URL(resourceURL).openConnection();
            con.setDoOutput(true);
            con.setRequestMethod("PUT");
            con.setRequestProperty("Content-Type", "text/plain");
    
            // Write the payload to the output stream
            try (OutputStream os = con.getOutputStream()) {
                os.write(payload.getBytes("UTF-8"));
            }
    
            int responseCode = con.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_CREATED || responseCode == HttpURLConnection.HTTP_OK) {
                log("Data published successfully to: " + resourceURL);
            } else {
                log("Failed to publish data. HTTP response code: " + responseCode);
            }
        } catch (Exception e) {
            log("Error publishing data: " + e.getMessage());
        }
    }
  

  /**
   * CArtAgO operation for reading data of a .txt file in a Linked Data Platform container of the Solid pod
   * 
   * @param containerName The name of the container where the .txt file resource is located
   * @param fileName The name of the .txt file resource that holds the data to be read
   * @param data An array whose elements are the data read from the .txt file
   */
    @OPERATION
    public void readData(String containerName, String fileName, OpFeedbackParam<Object[]> data) {
        data.set(readData(containerName, fileName));
    }

  /**
   * Method for reading data of a .txt file in a Linked Data Platform container of the Solid pod
   * 
   * @param containerName The name of the container where the .txt file resource is located
   * @param fileName The name of the .txt file resource that holds the data to be read
   * @return An array whose elements are the data read from the .txt file
   */
    public Object[] readData(String containerName, String fileName) {
        // Construct the resource URL inline: podURL/containerName/fileName
        String base = podURL.endsWith("/") ? podURL : podURL + "/";
        String resourceURL = base + containerName + "/" + fileName;
        log("Reading data from: " + resourceURL);

        StringBuilder response = new StringBuilder();
        try {
            HttpURLConnection con = (HttpURLConnection) new URL(resourceURL).openConnection();
            con.setRequestMethod("GET");
            con.setRequestProperty("Accept", "text/plain");

            if (con.getResponseCode() == HttpURLConnection.HTTP_OK) {
                try (BufferedReader in = new BufferedReader(
                        new InputStreamReader(con.getInputStream(), "UTF-8"))) {
                    String line;
                    while ((line = in.readLine()) != null) {
                        response.append(line).append("\n");
                    }
                }
            } else {
                log("Failed to read data. HTTP response code: " + con.getResponseCode());
            }
        } catch (Exception e) {
            log("Error reading data: " + e.getMessage());
        }
        return createArrayFromString(response.toString());
    }

  /**
   * Method that converts an array of Object instances to a string, 
   * e.g. the array ["one", 2, true] is converted to the string "one\n2\ntrue\n"
   *
   * @param array The array to be converted to a string
   * @return A string consisting of the string values of the array elements separated by "\n"
   */
    public static String createStringFromArray(Object[] array) {
        StringBuilder sb = new StringBuilder();
        for (Object obj : array) {
            sb.append(obj.toString()).append("\n");
        }
        return sb.toString();
    }

  /**
   * Method that converts a string to an array of Object instances computed by splitting the given string with delimiter "\n"
   * e.g. the string "one\n2\ntrue\n" is converted to the array ["one", "2", "true"]
   *
   * @param str The string to be converted to an array
   * @return An array consisting of string values that occur by splitting the string around "\n"
   */
    public static Object[] createArrayFromString(String str) {
        return str.split("\n");
    }


  /**
   * CArtAgO operation for updating data of a .txt file in a Linked Data Platform container of the Solid pod
   * The method reads the data currently stored in the .txt file and publishes in the file the old data along with new data 
   * 
   * @param containerName The name of the container where the .txt file resource is located
   * @param fileName The name of the .txt file resource that holds the data to be updated
   * @param data An array whose elements are the new data to be added in the .txt file
   */
    @OPERATION
    public void updateData(String containerName, String fileName, Object[] data) {
        Object[] oldData = readData(containerName, fileName);
        Object[] allData = new Object[oldData.length + data.length];
        System.arraycopy(oldData, 0, allData, 0, oldData.length);
        System.arraycopy(data, 0, allData, oldData.length, data.length);
        publishData(containerName, fileName, allData);
    }
}

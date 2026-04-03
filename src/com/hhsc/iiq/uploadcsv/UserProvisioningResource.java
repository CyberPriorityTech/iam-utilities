package com.hhsc.iiq.uploadcsv;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.log4j.Logger;

import com.hhsc.iiq.uploadcsv.services.UserProvisioningService;

import sailpoint.rest.plugin.AllowAll;
import sailpoint.rest.plugin.BasePluginResource;
import sailpoint.tools.GeneralException;

@Path("ApplicationOnboarding")
@Produces("application/json")
@Consumes("application/json")
@AllowAll
public class UserProvisioningResource extends BasePluginResource {

    private static final String SETTING_SEARCH_OBJECT_COUNT = "maxSearchObjectCount";
    Logger logger = Logger.getLogger("sailpoint.task.ApplicationOnboarding");

    @Override
    public String getPluginName() {
        return "ApplicationOnboarding";
    }

    /**
     * New endpoint: returns list of application names
     */
    @GET
    @Path("search/applications")
    @Produces("application/json")
    public List<String> getApplications() throws GeneralException {
        logger.info("Fetching application list...");
        return getSearchService().getApplicationNames(); // must be List<String>
    }


    /**
     * Existing endpoint: CSV upload
     */
    @POST
    @Path("importCSV")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces("application/json")
    public List<String> uploadCSV(Map<String, String> body) throws GeneralException {
        logger.info("CSV Upload Started");

        List<String> results = new ArrayList<>();
        try {
            String csvData = body.get("csvData");
            String applicationName = body.get("applicationName");
            logger.debug("Selected application: " + applicationName);
            BufferedReader reader = new BufferedReader(new StringReader(csvData));
            String line;
                // Read header row
                String headerLine = reader.readLine();
                String[] headers = headerLine.split(",");

                // Map column names to their indexes
                Map<String, Integer> columnIndexMap = new HashMap<>();
                for (int i = 0; i < headers.length; i++) {
                    columnIndexMap.put(headers[i].trim(), i);
                }

                // Process data rows
                while ((line = reader.readLine()) != null) {
                    String[] values = line.split(",");

                    String firstName = values[columnIndexMap.get("First*")];
                    String lastName  = values[columnIndexMap.get("Last*")];
                    String email     = values[columnIndexMap.get("Email*")];
                    String name      = values[columnIndexMap.get("SSO ID")];
                    String tamRole   = values[columnIndexMap.get("TAM Role")];

                results.addAll(getSearchService().createIdentity(firstName, lastName, email, name,tamRole,applicationName));
            }
        } catch (Exception e) {
            logger.error("CSV Upload Error", e);
            results.add("CSV processing failed: " + e.getMessage());
        }
        return results;
    }

	/**
	 * Gets an instance of the SearchService class.
	 *
	 * @return The service.
	 */
	private UserProvisioningService getSearchService() {
		return new UserProvisioningService(this);
	}

}

package com.hhsc.iiq.uploadcsv.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.apache.log4j.Logger;

import sailpoint.api.SailPointContext;
import sailpoint.api.SailPointFactory;
import sailpoint.api.Workflower;
import sailpoint.object.Application;
import sailpoint.object.Filter;
import sailpoint.object.Identity;
import sailpoint.object.Link;
import sailpoint.object.ManagedAttribute;
import sailpoint.object.ProvisioningPlan;
import sailpoint.object.ProvisioningPlan.AccountRequest;
import sailpoint.object.ProvisioningPlan.AttributeRequest;
import sailpoint.object.QueryOptions;
import sailpoint.object.Workflow;
import sailpoint.object.WorkflowLaunch;
import sailpoint.plugin.PluginContext;
import sailpoint.tools.GeneralException;

public class UserProvisioningService {

	Logger logger = Logger.getLogger("sailpoint.task.ApplicationOnboarding");
	private PluginContext pluginContext;

	public UserProvisioningService(PluginContext pluginContext) {
		this.pluginContext = pluginContext;
	}

	/**
     * New method; fetch all application names
     */
    public List<String> getApplicationNames() throws GeneralException {
        List<String> appNames = new ArrayList<>();
        SailPointContext context = SailPointFactory.getCurrentContext();

        List<Application> apps = context.getObjects(Application.class);
        for (Application app : apps) {
            appNames.add(app.getName()); // only return names
        }
        logger.info("application list..."+appNames);
        return appNames;
    }

	/**
	 * Existing method; create identity from CSV row
	 * 
	 * @param tamRole
	 */
	public List<String> createIdentity(String firstName, String lastName, String email, String userName, String tamRole,
			String applicationName) throws GeneralException {
		List<String> results = new ArrayList();
		SailPointContext context = SailPointFactory.getCurrentContext();

		List accountReqList = new ArrayList();
		AccountRequest accountReq = new AccountRequest();
		accountReq.setApplication(applicationName);
		ProvisioningPlan plan = new ProvisioningPlan();
		Identity existingUser = context.getObjectByName(Identity.class, userName);
		if (existingUser != null) {
			String dn = "uid=" + userName + ",cn=users,O=TAASTP,ST=TX,C=US";
			accountReq.setOperation(AccountRequest.Operation.Modify);
			accountReq.setNativeIdentity(dn);
			logger.error("BatchRequest tamRole" + tamRole);
			if (tamRole != null && !tamRole.isEmpty()) {
				String[] entitlementValue = tamRole.split("#");
				for (String groupName : entitlementValue) {
					logger.error("BatchRequest groupName " + groupName);
					QueryOptions entQo = new QueryOptions();

					Application appObject = context.getObjectByName(Application.class, applicationName);

					entQo.add(Filter.eq("application", appObject));
					entQo.add(Filter.eq("displayName", groupName));

					Iterator<ManagedAttribute> itEnt = context.search(ManagedAttribute.class, entQo);

					while (itEnt.hasNext()) {
						ManagedAttribute ent = (ManagedAttribute) itEnt.next();
						if (ent.getValue() != null) {
							accountReq.add(new AttributeRequest("groups", ProvisioningPlan.Operation.Add, ent.getValue()));
							accountReqList.add(accountReq);
						}

					}
				}
			} else {
				logger.error("No Entitlements found to add");
			}
			results.add("User Provisioned Successfully " + userName);
			//return results;
		} else {

		if (lastName != null) {
			if (userName != null && !userName.trim().isEmpty()) {
				userName = (String) userName.trim();
			} else {
				userName = firstName != null ? firstName.substring(0, 2) + "" + lastName :lastName;
			}
		} else {
			results.add("User LastName is Empty " + userName);
			//return results;
		}
		String initialUserName = userName;
		while (context.getObjectByName(Identity.class, userName) != null) {
			Random r = new Random();
			int randomNumber = 100 + r.nextInt(900);
			userName = initialUserName + randomNumber;
		}
		if (email != null && !email.trim().isEmpty()) {
			email = (String) email.trim();
		} else {
			email = firstName + "." + lastName + "@hhs.texas.qov";
		}
		String dn = "uid=" + userName + ",cn=users,O=TAASTP,ST=TX,C=US";
		QueryOptions qo = new QueryOptions();
		qo.addFilter(Filter.eq("application.name", applicationName));
		qo.addFilter(Filter.eq("nativeIdentity", dn));
		Iterator itr = context.search(Link.class, qo);
		if (itr != null && itr.hasNext()) {
			results.add("User Application Link Already Exists " + dn);
			//return results;
		}
		Identity identity = new Identity();
		identity.setFirstname(firstName);
		identity.setEmail(email);
		identity.setName(userName);
		identity.setLastname(lastName);
		context.saveObject(identity);
		context.commitTransaction();
		

		accountReq.setOperation(AccountRequest.Operation.Create);
		accountReq.setNativeIdentity(dn);
		logger.error("BatchRequest tamRole" + tamRole);
		if (tamRole != null && !tamRole.isEmpty()) {
			String[] entitlementValue = tamRole.split("#");
			for (String groupName : entitlementValue) {
				logger.error("BatchRequest groupName;; " + groupName);
				QueryOptions entQo = new QueryOptions();

				Application appObject = context.getObjectByName(Application.class, applicationName);

				entQo.add(Filter.eq("application", appObject));
				entQo.add(Filter.eq("displayName", groupName));

				Iterator<ManagedAttribute> itEnt = context.search(ManagedAttribute.class, entQo);

				while (itEnt.hasNext()) {
					ManagedAttribute ent = (ManagedAttribute) itEnt.next();
					if (ent.getValue() != null) {
						accountReq.add(new AttributeRequest("groups", ProvisioningPlan.Operation.Add, ent.getValue()));
						accountReqList.add(accountReq);
					}

				}
			}
		} else {
			logger.error("No Entitlements found to add");
		}
		context.decache();

		existingUser = context.getObjectByName(Identity.class, userName);
		}
		if(existingUser != null) {
		accountReqList.add(accountReq) ;
		plan.setAccountRequests (accountReqList) ; 
		plan.setNativeIdentity(existingUser.getName()) ;
		plan.setIdentity(existingUser);
		
		launchWorkflow(plan,existingUser.getName());
		results.add("User Provisioned Successfully; " + existingUser.getName());
		} else results.add("User Provisioning failed due to user not found " + userName);
		
		return results;
	}

	private void launchWorkflow(ProvisioningPlan plan, String name) throws GeneralException {
		SailPointContext context = SailPointFactory.getCurrentContext();
		
		String LCM_WORKFLOW_NAME = "LCM Provisioning";


        HashMap launchArgsMap = new HashMap();
        Identity myIdentity = plan.getIdentity();
        //Add needed Workflow Launch Variables to map of name/value pairs
        launchArgsMap.put("approvalMode", "parallelPoll");
        launchArgsMap.put("approvalScheme", "manager");
        launchArgsMap.put("fallbackApprover", "spadmin");
        launchArgsMap.put("flow", "AccessRequest");
        launchArgsMap.put("foregroundProvisioning", "true");
        launchArgsMap.put("identityDisplayName", myIdentity.getDisplayableName());
        launchArgsMap.put("identityName", myIdentity.getName());
        launchArgsMap.put("launcher", "spadmin");
        launchArgsMap.put("notificationScheme", "user,requester");
        launchArgsMap.put("plan", plan);
        launchArgsMap.put("policyScheme", "continue");
        launchArgsMap.put("requireViolationReviewComments", "true");
        launchArgsMap.put("source", "LCM");
        launchArgsMap.put("trace", "true");

        //Create WorkflowLaunch and set values
        WorkflowLaunch wflaunch = new WorkflowLaunch();
        Workflow wf = (Workflow) context.getObjectByName(Workflow.class, LCM_WORKFLOW_NAME);
        wflaunch.setWorkflowName(wf.getName());
        wflaunch.setWorkflowRef(wf.getName());
        wflaunch.setCaseName("LCM Provisioning");
        wflaunch.setVariables(launchArgsMap);
        Workflower workflower = new Workflower(context);
        WorkflowLaunch launch = workflower.launch(wflaunch);

        String workFlowId = launch.getWorkflowCase().getId();
		
	}
}

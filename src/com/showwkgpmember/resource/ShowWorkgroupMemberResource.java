package com.showwkgpmember.resource;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.showwkgpmember.service.ShowWorkgroupMemberService;
import com.showwkgpmember.utility.ShowWorkgroupMemberConstants;

import sailpoint.api.SailPointContext;
import sailpoint.api.SailPointFactory;
import sailpoint.object.Identity;
import sailpoint.rest.plugin.AllowAll;
import sailpoint.rest.plugin.BasePluginResource;
import sailpoint.tools.GeneralException;

@Path(ShowWorkgroupMemberConstants.PLUGIN_NAME)
public class ShowWorkgroupMemberResource extends BasePluginResource {
	private static Log log = LogFactory.getLog(ShowWorkgroupMemberConstants.LOGGER_NAME);

	@Override
	public String getPluginName() {
		return ShowWorkgroupMemberConstants.PLUGIN_NAME;
	}

	@GET
	@Path(ShowWorkgroupMemberConstants.GET_WKGP_DETAILS)
	@AllowAll
	@Produces(MediaType.APPLICATION_JSON)
	public String getWorkgroupDetails(@QueryParam(ShowWorkgroupMemberConstants.WKGP_NAME) String workgroupName) {
		log.trace("Entry ShowWorkgroupMemberResource::getWorkgroupDetails");
		log.trace("workgroupName: " + workgroupName);
		StringBuilder data = new StringBuilder();
		try {
			SailPointContext context = SailPointFactory.getCurrentContext();
			
			ShowWorkgroupMemberService wkgpService = new ShowWorkgroupMemberService();
			
			List<String> memberColumns = getSettingMultiString(ShowWorkgroupMemberConstants.MEMBERS_PROP);
			boolean showOwner = getSettingBool(ShowWorkgroupMemberConstants.SHOW_OWNER);
			String defaultNoMemberMsg = getSettingString(ShowWorkgroupMemberConstants.DEFAULT_NO_MEMBER_MSG);
			
			List<String> attrNames = wkgpService.getAttributeNames(memberColumns);
			log.trace("memberColumns: " + memberColumns);
			log.trace("attrNames: " + attrNames);
			log.trace("showOwner: " + showOwner);
			Identity id = context.getObjectByName(Identity.class, workgroupName);
			if (id != null && id.isWorkgroup()) {

				if (showOwner) {
					data.append(wkgpService.getOwnerData(id.getOwner()));
				}

				String membersDetails = wkgpService.getMembersDetails(context, id, attrNames);
				if (membersDetails.length() > 0) {
					data = data.append("<h4>Members of the workgroup</h4><table><tr>");
					for (String memberProp : memberColumns) {
						data = data.append("<th>").append(memberProp).append("</th>");
					}
					data = data.append("</tr>");
					data = data.append(membersDetails);
					data = data.append("</table><br/>");
				} else {
					data = data.append("<br/> <div style=\"text-align:center\"><h4 style=\"color:crimson\"")
							.append(defaultNoMemberMsg).append("</h4></div>");
				}
				log.debug("Final data to be displayed: "+data);
			}
		} catch (GeneralException genExp) {
			log.error("GeneralException in ShowWorkgroupMemberResource::getWorkgroupDetails: " + genExp);
		}
		log.trace("Exit ShowWorkgroupMemberResource::getWorkgroupDetails");
		return data.toString();
	}

}

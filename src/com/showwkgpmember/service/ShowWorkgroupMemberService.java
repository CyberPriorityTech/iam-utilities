package com.showwkgpmember.service;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.showwkgpmember.utility.ShowWorkgroupMemberConstants;

import sailpoint.api.ObjectUtil;
import sailpoint.api.SailPointContext;
import sailpoint.object.Identity;
import sailpoint.object.ObjectAttribute;
import sailpoint.tools.GeneralException;
import sailpoint.tools.Util;

public class ShowWorkgroupMemberService {

	private static Log log = LogFactory.getLog(ShowWorkgroupMemberConstants.LOGGER_NAME);

	public List<String> getAttributeNames(List<String> memberColumns) {
		log.trace("Entry ShowWorkgroupMemberService::getAttributeNames");
		List<String> attrNames = new ArrayList<>();
		log.trace("Creating the localized list of Identity filter based on Locale of the logged In User.");
		List<ObjectAttribute> identityProperties = Identity.getObjectConfig().getSearchableAttributes();
		if (!Util.isEmpty(identityProperties)) {
			memberColumns.forEach(attrs -> {
				log.trace("Getting name for localized attribute: " + attrs);
				for (ObjectAttribute identityProp : identityProperties) {
					if (attrs.equalsIgnoreCase(identityProp.getDisplayableName(Locale.US))) {
						String attrName = identityProp.getName();
						log.trace("Name for localized attribute: " + attrName);
						attrNames.add(attrName);
					}
				}
			});
		}
		log.trace("Attributes name from object config to be displayed: " + attrNames);
		log.trace("Exit ShowWorkgroupMemberService::getAttributeNames");
		return attrNames;
	}

	public String getOwnerData(Identity ownerId) {
		log.trace("Entry ShowWorkgroupMemberService::getOwnerData");
		StringBuilder ownerStringBuilder = new StringBuilder();
		if (ownerId != null) {
			log.debug("owner of the workgroup: " + ownerId.getName());
			ownerStringBuilder.append(
					"<h4>Owner of the workgroup</h4><table><tr><th>Name</th><th>Display Name</th><th>Email</th></tr>");
			ownerStringBuilder.append("<tr><td>").append(ownerId.getName()).append("</td><td>")
					.append(ownerId.getDisplayableName() != null ? ownerId.getDisplayableName() : "")
					.append("</td><td>").append(ownerId.getEmail() != null ? ownerId.getEmail() : "")
					.append("</td></tr>");
			ownerStringBuilder.append("</table><br/>");
		}
		String ownerData = ownerStringBuilder.toString();
		log.trace("Exit ShowWorkgroupMemberService::getOwnerData ownerData: " + ownerData);
		return ownerData;
	}

	public String getMembersDetails(SailPointContext context, Identity wkgp, List<String> attrNames) {
		log.trace("Entry ShowWorkgroupMemberService::getMembersDetails");
		StringBuilder memberStringBuilder = new StringBuilder();
		try {
			Iterator<Object[]> workgroupMembers = ObjectUtil.getWorkgroupMembers(context, wkgp, attrNames);
			while (workgroupMembers.hasNext()) {
				Object[] pro = workgroupMembers.next();
				memberStringBuilder.append("<tr>");
				for (int i = 0; i < attrNames.size(); i++) {
					memberStringBuilder.append("<td>").append(pro[i] != null ? (String) pro[i] : "").append("</td>");
				}
				memberStringBuilder.append("</tr>");
			}
		} catch (GeneralException genExp) {
			log.error("GeneralException in ShowWorkgroupMemberService::getMembersDetails: " + genExp);
		}
		String memberData = memberStringBuilder.toString();
		log.trace("Exit ShowWorkgroupMemberService::getMembersDetails memberData: " + memberData);
		return memberData;
	}

}

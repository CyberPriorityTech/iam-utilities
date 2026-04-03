const WGP_BASE_PATH = "showWorkgroupMember";

jQuery(document).ready(function() {
	var MutationObserver = window.MutationObserver || window.WebKitMutationObserver || window.MozMutationObserver;

	var mutationObserver = new MutationObserver(function(mutations) {
		if ($("[id*='moreLessListItem0']").length) {
			const approvalItems = $("sp-identity-request-item-approval-item[sp-approval-item='moreLessItem']>div");
			approvalItems.each(function(index, element) {
				if (element.children[1] !== undefined && $(element).find(".fa-info-circle").length === 0) {
					const wkgpName = element.childNodes[0].nodeValue.split("\n")[1].trim();
					const url = PluginHelper.getPluginRestUrl(`${WGP_BASE_PATH}/getWorkgroupDetails?workgroupName=${wkgpName}`);
					jQuery.ajax({
						type: "GET",
						beforeSend: function(request) {
							request.setRequestHeader("X-XSRF-TOKEN", PluginHelper.getCsrfToken());
						},
						url: url,
						success: function(data, status) {
							if (data && data.length > 0 && status === "success") {
								const button = document.createElement("button");
								button.className = "btn btn-xs btn-white";
								button.style.marginLeft = "5px";
								button.setAttribute("title", "Click to view members of the workgroup");
								const icon = document.createElement("i");
								icon.className = "fa fa-info-circle";
								button.appendChild(icon);

								if ($(element).find(".fa-info-circle").length === 0) {
									element.children[0].after(button);
									jQuery(button).click(function() {
										const popupWindow = window.open("", `workgroupDetailsWindow`, "width=600, height=400, scrollbars=yes, menubar=no, status=no, location=no");
										const $popupWin = $(popupWindow.document.body);
										$popupWin.html(`
											<head>
											<style>
											<title>Workgroup: ${wkgpName}</title>
											<style>
												* {
													font-family: "Open Sans", "Helvetica Neue", Helvetica, Arial, sans-serif;
												}
												table {
													border-collapse: collapse;
													width: 100%;
												}
												td, th {
													border: 1px solid #dddddd; text-align: left; padding: 8px;
												}
												th {
													background-color: #e9f5f9;
												}
												tr:nth-child(odd) {
													background-color: #f9fafc;
												}
												h4 {
													color: #10A2CE;
												}
											</style>
											</head> <body>
											<img src="${window.location.origin}/identityiq/ui/images/TopLogo1.png" style="margin:10px;"></img> 
											${data}
											</body>`);
										popupWindow.document.close();
									});
								}
							}
						}
					});
				}
			});
		}
	});
	mutationObserver.observe(document.documentElement, { childList: true, subtree: true, attributes: false, characterData: true });
});

/*
 ****************************************************************************
 *
 * Copyright (c)2024 The Vanguard Group of Investment Companies (VGI)
 * All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS
 * IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 ****************************************************************************
 */
package com.vanguard.screenshot.gen.core.workflow;

import com.adobe.granite.workflow.WorkflowException;
import com.adobe.granite.workflow.WorkflowSession;
import com.adobe.granite.workflow.exec.WorkItem;
import com.adobe.granite.workflow.exec.WorkflowProcess;
import com.adobe.granite.workflow.metadata.MetaDataMap;
import com.day.cq.commons.jcr.JcrConstants;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.osgi.services.HttpClientBuilderFactory;
import org.apache.http.util.EntityUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.json.JSONObject;
import org.osgi.service.component.annotations.Component;

import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.UserManager;

import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import java.util.ArrayList;
import java.util.List;

@Component(
    service = WorkflowProcess.class,
    property = {"process.label= Generate Screenshot"}
)
public class GenerateScreenshot implements WorkflowProcess {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public static final String SCREENSHOT_FILE_PATH = "screenshotFilePath";
    public static final String PDF_FILE_PATH = "pdfFilePath";

    public static final String AEM_USER_EMAIL_PROPERTY = "profile/email";

    public static final String START_TIME = "startTime";

    public static final String PAYLOAD = "payload";

    @Reference
    private HttpClientBuilderFactory httpFactory;

    @Reference
    private SendEmailService sendEmailService;

    @Override
    public void execute(WorkItem workItem, WorkflowSession workflowSession, MetaDataMap metaDataMap) throws WorkflowException {

        String screenshotFilePath = null;
        String pdfFilePath = null;
        String emailTemplatePath = "/etc/notification/email/screenshot-gen/review-screenshot.txt";
        String pdfFileName = StringUtils.EMPTY;
        String isScreenshotGenerated = "no";

        String payloadPath = workItem.getWorkflowData().getPayload().toString();


        try {
            ResourceResolver resourceResolver = workflowSession.adaptTo(ResourceResolver.class);
            Resource resource = resourceResolver.getResource(payloadPath + "/" + JcrConstants.JCR_CONTENT);
            String pageTitle = resource.getValueMap().get(JcrConstants.JCR_TITLE, String.class);

            String previewURL = "http://localhost:4505" +  payloadPath + ".html";
            Map<String, String> screenshotsMap = fetchPageScreenshot(previewURL);
            MetaDataMap workflowMetadataMap = workItem.getWorkflow().getWorkflowData().getMetaDataMap();

            if(screenshotsMap.containsKey(SCREENSHOT_FILE_PATH))
                screenshotFilePath = screenshotsMap.get(SCREENSHOT_FILE_PATH);
            if(screenshotsMap.containsKey(PDF_FILE_PATH))
                pdfFilePath = screenshotsMap.get(PDF_FILE_PATH);



            if (StringUtils.isNotEmpty(pdfFilePath)) {
                pdfFileName = new StringBuilder(pageTitle.replace(" ", "_")).append(".pdf").toString();
                LOGGER.info("screenshotFilePath and pdfFilePath: {} , {}", screenshotFilePath, pdfFilePath);

                //setting file path in metadata map
                workflowMetadataMap.put(SCREENSHOT_FILE_PATH, screenshotFilePath);
                workflowMetadataMap.put(PDF_FILE_PATH, pdfFilePath);

                Boolean isFileSentSuccess = sendScreenshotToAuthor(workItem, emailTemplatePath, pdfFilePath, workflowSession, resourceResolver, pdfFileName);
                LOGGER.debug("Email Sent successfully ? :: {} ", isFileSentSuccess);
                isScreenshotGenerated = "yes";
            } else{
                LOGGER.debug("Screenshot not generated, hence moving to next steps");
                isScreenshotGenerated = "no";
            }
            workflowMetadataMap.put("isScreenshotGenerated", isScreenshotGenerated);
        } catch (Exception ex) {
            LOGGER.error("Error Sending document to author :: {}", ex);
        }
    }

    public Map<String, String> fetchPageScreenshot(String pageURL) throws IOException {

        CloseableHttpClient httpClient = httpFactory.newBuilder().build();

        try {
            if (StringUtils.isNotEmpty(pageURL)) {
                LOGGER.debug("Upload Document :: Current Page URL :: {}", pageURL);
                HttpGet getRequest = new HttpGet("http://localhost:3000/webscreenshot");
                List<NameValuePair> nameValuePairs = new ArrayList<>();
                nameValuePairs.add(new BasicNameValuePair("url", pageURL));
                URI uri = new URIBuilder(getRequest.getURI())
                        .addParameters(nameValuePairs)
                        .build();
                getRequest.setURI(uri);
                LOGGER.debug("Get Request url :: {}", uri.toString());
                HttpResponse serviceResponse = httpClient.execute(getRequest);
                HttpEntity entity = serviceResponse.getEntity();
                String content = EntityUtils.toString(entity);
                LOGGER.debug("Fetch Page Screenshot:: {}", content);
                JSONObject jsonObject = new JSONObject(content);
                HashMap<String, String> screenshotsMap = new HashMap<>();
                if (jsonObject.get("status").toString().equalsIgnoreCase("success")) {
                    if (jsonObject.has("screenshotFilePath") && !jsonObject.isNull("screenshotFilePath")) {
                        screenshotsMap.put("screenshotFilePath", jsonObject.get("screenshotFilePath").toString());
                        LOGGER.debug("Screenshot PNG Path :: {}", jsonObject.get("screenshotFilePath").toString());
                    }
                    if (jsonObject.has("pdfFilePath") && !jsonObject.isNull("pdfFilePath")) {
                        screenshotsMap.put("pdfFilePath", jsonObject.get("pdfFilePath").toString());
                        LOGGER.debug("Screenshot PDF Path :: {}", jsonObject.get("pdfFilePath").toString());
                    }
                } else {
                    LOGGER.debug("Error creating screenshot:: {}", pageURL);
                }
                return screenshotsMap;
            }
        } catch (Exception Ex) {
            LOGGER.error("Error fetching the screenshot: {}", Ex.getMessage());
        } finally {
            httpClient.close();
        }
        return null;
    }

    private Boolean sendScreenshotToAuthor(WorkItem workItem, String emailTemplatePath, String attachmentPath, WorkflowSession wfSession, ResourceResolver resourceResolver, String attachmentName){
        try{

            String initiatorEmailId = getEmail(workItem.getWorkflow().getInitiator(), resourceResolver);
            LOGGER.debug("Initiator email ID :: "+initiatorEmailId);
            String[] emailRecipient = {initiatorEmailId};

            Map<String, String> emailParameters = new HashMap<>();
            emailParameters.put(START_TIME, workItem.getWorkflow().getTimeStarted().toString());
            emailParameters.put(PAYLOAD, workItem.getWorkflowData().getPayload().toString());

            sendEmailService.sendAttachmentFromAWS(emailRecipient, emailTemplatePath, emailParameters, wfSession.adaptTo(Session.class), resourceResolver, attachmentName, attachmentPath, "application/pdf");
            return true;
        } catch(Exception ex){
            LOGGER.error("Error while sending email to author with screenshot :: {}", ex);
            return false;
        }
    }

    public String getEmail(String userId, ResourceResolver resourceResolver){
        if(StringUtils.isEmpty(userId)){
            LOGGER.error("Empty userId passed, returning null. userId : {}", userId);
            return null;
        }

        if(null == resourceResolver){
            LOGGER.error("ResourceResolver value passed is null, returning null. userId : {}", userId);
            return null;
        }

        UserManager userManager = resourceResolver.adaptTo(UserManager.class);
        Authorizable userAuthorization;
        String emailId = null;
        try {
            userAuthorization = userManager.getAuthorizable(userId);
            if(null != userAuthorization && userAuthorization.hasProperty(AEM_USER_EMAIL_PROPERTY)) {
                emailId = userAuthorization.getProperty(AEM_USER_EMAIL_PROPERTY)[0].toString();
            }
        } catch (RepositoryException ex) {
            LOGGER.error("Exception while getting email ID for user : {} \\n And the exception is :: {}", userId, ex);
        }

        return emailId;
    }
}

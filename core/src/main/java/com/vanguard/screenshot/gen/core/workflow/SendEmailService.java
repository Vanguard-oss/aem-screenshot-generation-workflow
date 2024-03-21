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

import com.day.cq.commons.Externalizer;
import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.commons.mail.MailTemplate;
import com.day.cq.mailer.MessageGateway;
import com.day.cq.mailer.MessageGatewayService;
import org.apache.commons.lang.text.StrLookup;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.mail.HtmlEmail;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.Session;
import javax.mail.util.ByteArrayDataSource;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Designate(ocd = SendEmailService.Config.class)
@Component(service = SendEmailService.class)
public class SendEmailService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private Config config;

    public static final String AUTHOR_DOMAIN_PROPERTY = "authorDomain";

    public static final String EMAIL_PROPERTY_AUTHOR_URL = "authorUrl";

    public static final String START_TIME = "startTime";

    public static final String PAYLOAD = "payload";


    @Reference
    private MessageGatewayService messageGatewayService;


    //<------------------------------------------ OSGI Internal ------------------------------------------>

    @Activate
    protected void start(SendEmailService.Config config) {
        this.config = config;
        String emailTemplateTootPath = this.config.emailTemplateRootPath();
        LOGGER.debug("Email Template Root Path : {}", emailTemplateTootPath);
    }

    public void sendAttachmentFromAWS(String[] toAddress, String emailTemplate, final Map<String, String> parameters, Session session, ResourceResolver resourceResolver, String attachmentName, String attachmentPath, String fileType) {
        try {
            if (null == session) {
                LOGGER.error("Failed sending Email : session is null, Method Arguments -> emailTemplate : {}", emailTemplate);
                return;
            }
            if (null == toAddress || toAddress.length == 0) {
                LOGGER.error("Failed sending Email : toAddress field is empty");
                return;
            }
            if (StringUtils.isEmpty(emailTemplate)) {
                LOGGER.debug("Setting default Email template as email template passed is Empty, Method Arguments -> emailTemplate : {}", emailTemplate);
                emailTemplate = "/etc/notification/email/screenshot-gen/review-screenshot.txt";
            }

            String authorDomain = "http://localhost:4502";

            parameters.put(AUTHOR_DOMAIN_PROPERTY, authorDomain);
            String payload = null;
            if (parameters.containsKey(PAYLOAD)) {
                payload = parameters.get(PAYLOAD);
                LOGGER.debug("Email parameters contains payload key, value is : {}", payload);
            }
            if (StringUtils.isNotEmpty(payload)) {

                parameters.put(EMAIL_PROPERTY_AUTHOR_URL, authorDomain + payload);

            }

            final MailTemplate mailTemplate = MailTemplate.create(emailTemplate, session);

            HtmlEmail email = mailTemplate.getEmail(StrLookup.mapLookup(parameters), HtmlEmail.class);
            email.addTo(toAddress);
            MessageGateway<HtmlEmail> messageGateway = messageGatewayService.getGateway(HtmlEmail.class);
            if (StringUtils.isNotEmpty(attachmentPath)) {
                LOGGER.debug("File Attachment path on AWS :: {}", attachmentPath);
                File pdfToAttach = new File(attachmentPath);
                InputStream fileStream = new FileInputStream(pdfToAttach);
                ByteArrayDataSource imageDS = new ByteArrayDataSource(fileStream, fileType);
                email.attach(imageDS, attachmentName, "Page Screenshot");
            }
            messageGateway.send(email);
            LOGGER.debug("Email with Attachment Sent using email template : {}", emailTemplate);
        } catch (Exception ex) {
            LOGGER.error("Error while sending email with attachment for template path : [{}], Email parameters : [{}]", emailTemplate, parameters, ex);
        }
    }

    @ObjectClassDefinition(
            name = "Send Email Service",
            description = "SendEmail Service"
    )
    @interface Config {

        @AttributeDefinition(name = "Email Template Root Path")
        String emailTemplateRootPath() default "/etc/notification/email/infra-core";

    }
}

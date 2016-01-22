package com.ceilfors.jenkins.plugins.jiratrigger.webhook

import groovy.json.JsonSlurper
import groovy.util.logging.Log
import hudson.Extension
import hudson.model.UnprotectedRootAction
import org.codehaus.jettison.json.JSONObject
import org.kohsuke.stapler.StaplerRequest
import org.kohsuke.stapler.interceptor.RequirePOST

import javax.inject.Inject

/**
 * The HTTP endpoint that receives JIRA Webhook.
 *
 * @author ceilfors
 */
@Log
@Extension
class JiraWebhook implements UnprotectedRootAction {

    public static final URL_NAME = "jira-trigger"
    public static final WEBHOOK_EVENT = "jira:issue_updated"
    private JiraWebhookListener jiraWebhookListener

    JiraWebhook() {
    }

    @Inject
    void setJiraWebhookListener(JiraWebhookListener jiraWebhookListener) {
        this.jiraWebhookListener = jiraWebhookListener
    }

    @Override
    String getIconFileName() {
        return null
    }

    @Override
    String getDisplayName() {
        return "JIRA Trigger"
    }

    @Override
    String getUrlName() {
        return URL_NAME
    }

    @SuppressWarnings("GroovyUnusedDeclaration")
    @RequirePOST
    public void doIndex(StaplerRequest request) {
        processEvent(request, getRequestBody(request))
    }

    public void processEvent(StaplerRequest request, String webhookEvent) {
        Map webhookEventMap = new JsonSlurper().parseText(webhookEvent) as Map
        String eventType = webhookEventMap['webhookEvent']
        if (isChangelogEvent(webhookEventMap)) {
            log.fine("Received Webhook callback from changelog. Event type: ${eventType}")
            WebhookChangelogEvent changelogEvent = new WebhookChangelogEventJsonParser().parse(new JSONObject(webhookEvent))
            changelogEvent.userId = request.getParameter("user_id")
            changelogEvent.userKey = request.getParameter("user_key")
            jiraWebhookListener.changelogCreated(changelogEvent)
        } else if (isCommentEvent(webhookEventMap)) {
            log.fine("Received Webhook callback from comment. Event type: ${eventType}")
            WebhookCommentEvent commentEvent = new WebhookCommentEventJsonParser().parse(new JSONObject(webhookEvent))
            commentEvent.userId = request.getParameter("user_id")
            commentEvent.userKey = request.getParameter("user_key")
            jiraWebhookListener.commentCreated(commentEvent)
        } else {
            log.warning("Received Webhook callback with an invalid event type or a body without comment/changelog. " +
                    "Event type: ${eventType}. Event body contains: ${webhookEventMap.keySet()}.")
        }
    }

    private boolean isChangelogEvent(Map webhookEventMap) {
        String eventType = webhookEventMap["webhookEvent"]
        return eventType == WEBHOOK_EVENT && webhookEventMap["changelog"]
    }

    private boolean isCommentEvent(Map webhookEventMap) {
        String eventType = webhookEventMap["webhookEvent"]
        return eventType == WEBHOOK_EVENT && webhookEventMap["comment"]
    }

    private String getRequestBody(StaplerRequest req) {
        return req.inputStream.text
    }
}
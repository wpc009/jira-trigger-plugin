package com.ceilfors.jenkins.plugins.jirabuilder

import hudson.Plugin
import hudson.util.PluginServletFilter

/**
 * @author ceilfors
 */
class JiraBuilderPlugin extends Plugin {

    @Override
    void start() throws Exception {
        PluginServletFilter.addFilter(new ExceptionLoggingFilter())
    }
}
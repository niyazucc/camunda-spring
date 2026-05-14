package com.example.camunda_spring;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.util.logging.Logger;

@Component("slaNotifyDelegate")
public class SlaNotifyDelegate implements JavaDelegate {

    private final Logger LOGGER = Logger.getLogger(SlaNotifyDelegate.class.getName());

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String technician = (String) execution.getVariable("technicianId");
        String issue = (String) execution.getVariable("complaint_title");
        String businessKey = execution.getBusinessKey();

        // This is where you would call an Email Service or SMS API
        LOGGER.warning("!!! SLA ALERT !!!");
        LOGGER.warning("Task for " + technician + " regarding '" + issue + "' has exceeded 2 minutes.");
        LOGGER.warning("Process Business Key: " + businessKey);

        // Example: Adding a flag so the supervisor knows it was escalated
        execution.setVariable("slaEscalated", true);
    }
}
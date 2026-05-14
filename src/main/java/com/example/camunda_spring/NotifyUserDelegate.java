
package com.example.camunda_spring;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component("notifyUserDelegate") // This name must match the BPMN Delegate Expression
public class NotifyUserDelegate implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String initiator = (String) execution.getVariable("initiator");

        // In a real app, you'd trigger an email or SMS here
        System.out.println(">>> NOTIFICATION: Dear " + initiator +
                ", your complaint has been resolved and approved!");
    }
}
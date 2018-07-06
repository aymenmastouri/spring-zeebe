package io.zeebe.spring.client.config.processor;

import java.util.function.Consumer;
import java.util.stream.Collectors;

import io.zeebe.client.ZeebeClient;
import io.zeebe.client.api.events.DeploymentEvent;
import io.zeebe.spring.client.annotation.ZeebeDeployment;
import io.zeebe.spring.client.bean.ClassInfo;
import io.zeebe.spring.client.bean.value.ZeebeDeploymentValue;
import io.zeebe.spring.client.bean.value.factory.ReadZeebeDeploymentValue;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DeploymentPostProcessor extends BeanInfoPostProcessor
{
    private final ReadZeebeDeploymentValue reader;

    public DeploymentPostProcessor(final ReadZeebeDeploymentValue reader)
    {
        this.reader = reader;
    }

    @Override
    public boolean test(final ClassInfo beanInfo)
    {
        return beanInfo.hasClassAnnotation(ZeebeDeployment.class);
    }

    @Override
    public Consumer<ZeebeClient> apply(final ClassInfo beanInfo)
    {
        final ZeebeDeploymentValue value = reader.applyOrThrow(beanInfo);

        log.info("deployment: {}", value);

        return client -> {
            final DeploymentEvent deploymentResult = client.topicClient(value.getTopicName())
                                                           .workflowClient()
                                                           .newDeployCommand()
                                                           .addResourceFromClasspath(value.getClassPathResource())
                                                           .send()
                                                           .join();

            log.info("Deployed: {}", deploymentResult.getDeployedWorkflows()
                                                     .stream()
                                                     .map(wf -> String.format("<%s:%d>", wf.getBpmnProcessId(), wf.getVersion()))
                                                     .collect(Collectors.joining(",")));

        };
    }

}

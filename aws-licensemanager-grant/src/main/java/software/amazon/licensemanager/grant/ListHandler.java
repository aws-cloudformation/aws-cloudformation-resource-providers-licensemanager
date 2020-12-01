package software.amazon.licensemanager.grant;

import software.amazon.awssdk.services.licensemanager.LicenseManagerClient;
import software.amazon.awssdk.services.licensemanager.model.AccessDeniedException;
import software.amazon.awssdk.services.licensemanager.model.ListDistributedGrantsRequest;
import software.amazon.awssdk.services.licensemanager.model.ListDistributedGrantsResponse;
import software.amazon.awssdk.services.licensemanager.model.ValidationException;
import software.amazon.cloudformation.exceptions.CfnAccessDeniedException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.ResourceNotFoundException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ListHandler extends BaseHandlerStd {

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<LicenseManagerClient> proxyClient,
            final Logger logger) {

        final ResourceModel model = request.getDesiredResourceState();
        try {
            final List<ResourceModel> models = new ArrayList<>();

            // STEP 1 [TODO: construct a body of a request]
            final ListDistributedGrantsRequest listDistributedGrantsRequest = Translator.translateToListRequest(model, request.getNextToken());

            ListDistributedGrantsResponse listDistributedGrantsResponse = null;
            try {
                // STEP 2 [TODO: make an api call]
                listDistributedGrantsResponse = proxy.injectCredentialsAndInvokeV2(
                        listDistributedGrantsRequest, proxyClient.client()::listDistributedGrants);
            } catch (AccessDeniedException e) {
                throw new CfnAccessDeniedException("List:License", e);
            } catch (ValidationException e) {
                throw new CfnInvalidRequestException(e);
            }

            // STEP 3 [TODO: get a token for the next page]
            String nextToken = listDistributedGrantsResponse.nextToken();

            // STEP 4 [TODO: construct resource models]
            // e.g. https://github.com/aws-cloudformation/aws-cloudformation-resource-providers-logs/blob/master/aws-logs-loggroup/src/main/java/software/amazon/logs/loggroup/ListHandler.java#L19-L21

            models.addAll(Translator.translateFromListRequest(listDistributedGrantsResponse).stream().collect(Collectors.toList()));
            return ProgressEvent.<ResourceModel, CallbackContext>builder()
                    .resourceModels(models)
                    .nextToken(nextToken)
                    .status(OperationStatus.SUCCESS)
                    .build();
        } catch (final software.amazon.awssdk.services.licensemanager.model.ResourceNotFoundException e) {
            throw new ResourceNotFoundException(ResourceModel.TYPE_NAME, e.getMessage(), e);
        }

    }
}

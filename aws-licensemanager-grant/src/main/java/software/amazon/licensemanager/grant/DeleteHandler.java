package software.amazon.licensemanager.grant;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.licensemanager.LicenseManagerClient;
import software.amazon.awssdk.services.licensemanager.model.DeleteGrantRequest;
import software.amazon.awssdk.services.licensemanager.model.DeleteGrantResponse;
import software.amazon.awssdk.services.licensemanager.model.InvalidParameterValueException;
import software.amazon.awssdk.services.licensemanager.model.ValidationException;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.exceptions.ResourceNotFoundException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class DeleteHandler extends BaseHandlerStd {
    private Logger logger;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<LicenseManagerClient> proxyClient,
            final Logger logger) {

        this.logger = logger;

        // TODO: Adjust Progress Chain according to your implementation
        // https://github.com/aws-cloudformation/cloudformation-cli-java-plugin/blob/master/src/main/java/software/amazon/cloudformation/proxy/CallChain.java

        return ProgressEvent.progress(request.getDesiredResourceState(), callbackContext)

                // STEP 1 [check if resource already exists]
                // for more information -> https://docs.aws.amazon.com/cloudformation-cli/latest/userguide/resource-type-test-contract.html
                // if target API does not support 'ResourceNotFoundException' then following check is required
                .then(progress -> {
                            try {
                                ProgressEvent readHandlerResponse = new ReadHandler().handleRequest(proxy, request, callbackContext, proxyClient, logger);
                                request.getDesiredResourceState()
                                        .setVersion(((ResourceModel) readHandlerResponse.getResourceModel()).getVersion());
                                return ProgressEvent.progress(request.getDesiredResourceState(), callbackContext);
                            } catch (final CfnNotFoundException e) {
                                logger.log(request.getDesiredResourceState().getPrimaryIdentifier()
                                        + " does not exist; Therefore, can not be deleted.");
                                return ProgressEvent.defaultFailureHandler(e, HandlerErrorCode.NotFound);
                            }
                        }
                )

                // STEP 2.0 [delete/stabilize progress chain - required for resource deletion]
                .then(progress ->
                        // If your service API throws 'ResourceNotFoundException' for delete requests then DeleteHandler can return just proxy.initiate construction
                        // STEP 2.0 [initialize a proxy context]
                        // Implement client invocation of the delete request through the proxyClient, which is already initialised with
                        // caller credentials, correct region and retry settings
                        proxy.initiate("AWS-LicenseManager-Grant::Delete", proxyClient, progress.getResourceModel(), progress.getCallbackContext())

                                // STEP 2.1 [TODO: construct a body of a request]
                                .translateToServiceRequest(Translator::translateToDeleteRequest)

                                // STEP 2.2 [TODO: make an api call]
                                .makeServiceCall((awsRequest, client) -> deleteGrant(progress.getResourceModel(), awsRequest, client))
                                .done(awsResponse -> ProgressEvent.<ResourceModel, CallbackContext>builder()
                                        .status(OperationStatus.SUCCESS)
                                        .build()));
    }

    private DeleteGrantResponse deleteGrant(
            final ResourceModel model,
            final DeleteGrantRequest deleteGrantRequest,
            final ProxyClient<LicenseManagerClient> proxyClient) {
        DeleteGrantResponse deleteGrantResponse = null;
        try {

            deleteGrantResponse = proxyClient.injectCredentialsAndInvokeV2(deleteGrantRequest,
                    proxyClient.client()::deleteGrant);
        } catch (InvalidParameterValueException | ValidationException e) {
            logger.log("Resource deletion failed");
            throw new CfnNotFoundException(model.getGrantArn(), ResourceModel.TYPE_NAME);
        } catch (final AwsServiceException e) {
            logger.log("Resource deletion failed");
            throw new CfnGeneralServiceException(ResourceModel.TYPE_NAME + e.getMessage(), e);
        }

        logger.log(String.format("%s successfully deleted.", ResourceModel.TYPE_NAME));
        return deleteGrantResponse;
    }
}

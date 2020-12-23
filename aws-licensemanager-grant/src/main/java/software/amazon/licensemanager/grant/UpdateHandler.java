package software.amazon.licensemanager.grant;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.licensemanager.LicenseManagerClient;
import software.amazon.awssdk.services.licensemanager.model.CreateGrantVersionRequest;
import software.amazon.awssdk.services.licensemanager.model.CreateGrantVersionResponse;
import software.amazon.awssdk.services.licensemanager.model.InvalidParameterValueException;
import software.amazon.awssdk.services.licensemanager.model.ValidationException;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class UpdateHandler extends BaseHandlerStd {
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

                // STEP 2 [first update/stabilize progress chain - required for resource update]
                .then(progress ->
                        // STEP 2.0 [initialize a proxy context]
                        // Implement client invocation of the update request through the proxyClient, which is already initialised with
                        // caller credentials, correct region and retry settings
                        proxy.initiate("AWS-LicenseManager-Grant::Update", proxyClient, progress.getResourceModel(), progress.getCallbackContext())

                                // STEP 2.1 [TODO: construct a body of a request]
                                .translateToServiceRequest(model -> Translator.translateToUpdateRequest(model, request.getClientRequestToken()))

                                // STEP 2.2 [TODO: make an api call]
                                .makeServiceCall((awsRequest, client) -> createGrantVersion(progress.getResourceModel(), awsRequest, client))

                                // STEP 2.3 [TODO: stabilize step is not necessarily required but typically involves describing the resource until it is in a certain status, though it can take many forms]
                                // stabilization step may or may not be needed after each API call
                                // for more information -> https://docs.aws.amazon.com/cloudformation-cli/latest/userguide/resource-type-test-contract.html
                                .stabilize((awsRequest, awsResponse, client, model, context) -> {
                                    // TODO: put your stabilization code here

                                    final boolean stabilized = true;

                                    logger.log(String.format("%s [%s] update has stabilized: %s", ResourceModel.TYPE_NAME, model.getPrimaryIdentifier(), stabilized));
                                    return stabilized;
                                })
                                .progress())

                // STEP 4 [TODO: describe call/chain to return the resource model]
                .then(progress -> new ReadHandler().handleRequest(proxy, request, callbackContext, proxyClient, logger));
    }


    private CreateGrantVersionResponse createGrantVersion(
            final ResourceModel model,
            final CreateGrantVersionRequest createGrantVersionRequest,
            final ProxyClient<LicenseManagerClient> proxyClient) {
        CreateGrantVersionResponse createGrantVersionResponse = null;
        try {
            createGrantVersionResponse = proxyClient.injectCredentialsAndInvokeV2(createGrantVersionRequest,
                    proxyClient.client()::createGrantVersion);
        }
        catch (InvalidParameterValueException | ValidationException e) {
            logger.log("Resource update failed");
            throw new CfnNotFoundException(model.getGrantArn(), ResourceModel.TYPE_NAME);
        }
        catch (final AwsServiceException e) {
            logger.log(String.format("Resource updation failed"));
            throw new CfnGeneralServiceException(ResourceModel.TYPE_NAME + e.getMessage(), e);
        }

        logger.log(String.format("%s has successfully been updated.", ResourceModel.TYPE_NAME));
        return createGrantVersionResponse;
    }
}

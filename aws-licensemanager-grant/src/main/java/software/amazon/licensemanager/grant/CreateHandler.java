package software.amazon.licensemanager.grant;

import software.amazon.awssdk.services.licensemanager.LicenseManagerClient;
import software.amazon.awssdk.services.licensemanager.model.AccessDeniedException;
import software.amazon.awssdk.services.licensemanager.model.CreateGrantRequest;
import software.amazon.awssdk.services.licensemanager.model.CreateGrantResponse;
import software.amazon.awssdk.services.licensemanager.model.GetGrantResponse;
import software.amazon.awssdk.services.licensemanager.model.InvalidParameterValueException;
import software.amazon.awssdk.services.licensemanager.model.InvalidResourceStateException;
import software.amazon.awssdk.services.licensemanager.model.ResourceLimitExceededException;
import software.amazon.awssdk.services.licensemanager.model.ValidationException;
import software.amazon.cloudformation.exceptions.CfnAccessDeniedException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnResourceConflictException;
import software.amazon.cloudformation.exceptions.CfnServiceLimitExceededException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;


public class CreateHandler extends BaseHandlerStd {
    private Logger logger;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<LicenseManagerClient> proxyClient,
            final Logger logger) {

        this.logger = logger;
        final ResourceModel resourceModel = request.getDesiredResourceState();

        // TODO: Adjust Progress Chain according to your implementation
        // https://github.com/aws-cloudformation/cloudformation-cli-java-plugin/blob/master/src/main/java/software/amazon/cloudformation/proxy/CallChain.java

        return ProgressEvent.progress(resourceModel, callbackContext)
                // STEP 2 [create/stabilize progress chain - required for resource creation]
                .then(progress ->
                        // If your service API throws 'ResourceAlreadyExistsException' for create requests then CreateHandler can return just proxy.initiate construction
                        // STEP 2.0 [initialize a proxy context]
                        // Implement client invocation of the create request through the proxyClient, which is already initialised with
                        // caller credentials, correct region and retry settings
                        proxy.initiate("AWS-LicenseManager-Grant::Create", proxyClient, resourceModel, callbackContext)

                                // STEP 2.1 [TODO: construct a body of a request]
                                .translateToServiceRequest(Translator::translateToCreateRequest)

                                // STEP 2.2 [TODO: make an api call]
                                .makeServiceCall((awsRequest, client) -> createGrant(progress, client, awsRequest, resourceModel, request))

                                // STEP 2.3 [TODO: stabilize step is not necessarily required but typically involves describing the resource until it is in a certain status, though it can take many forms]
                                // for more information -> https://docs.aws.amazon.com/cloudformation-cli/latest/userguide/resource-type-test-contract.html
                                // If your resource requires some form of stabilization (e.g. service does not provide strong consistency), you will need to ensure that your code
                                // accounts for any potential issues, so that a subsequent read/update requests will not cause any conflicts (e.g. NotFoundException/InvalidRequestException)
                                .stabilize((awsRequest, awsResponse, client, model, context) -> stabilizedOnCreate(model, awsResponse, client))
                                .progress()
                )

                // STEP 3 [TODO: describe call/chain to return the resource model]
                .then(progress -> new ReadHandler().handleRequest(proxy, request, callbackContext, proxyClient, logger));
    }


    private CreateGrantResponse createGrant(
            ProgressEvent<ResourceModel, CallbackContext> progress,
            ProxyClient<LicenseManagerClient> client,
            CreateGrantRequest awsRequest,
            ResourceModel model,
            ResourceHandlerRequest<ResourceModel> request) {

        CreateGrantResponse createGrantResponse;
        try {
            createGrantResponse = client.injectCredentialsAndInvokeV2(awsRequest, client.client()::createGrant);
        } catch (final ValidationException | InvalidParameterValueException e) {
            logger.log(e.getMessage());
            throw new CfnInvalidRequestException(String.format("Couldn't create %s due to error: %s", ResourceModel.TYPE_NAME, e.getMessage()), e);
        } catch (final AccessDeniedException e) {
            logger.log(e.getMessage());
            throw new CfnAccessDeniedException(ResourceModel.TYPE_NAME, e);
        } catch (final ResourceLimitExceededException e) {
            logger.log(e.getMessage());
            throw new CfnServiceLimitExceededException(e);
        } catch (final InvalidResourceStateException e) {
            logger.log(e.getMessage());
            throw new CfnResourceConflictException(e);
        }
        logger.log(
                String.format("%s [%s] successfully created.",
                        ResourceModel.TYPE_NAME,
                        createGrantResponse.grantArn()
                )
        );
        return createGrantResponse;
    }

    private boolean stabilizedOnCreate(
            final ResourceModel model,
            final CreateGrantResponse awsResponse,
            final ProxyClient<LicenseManagerClient> proxyClient) {
        try
        {
            model.setGrantArn(awsResponse.grantArn());
            final GetGrantResponse response = proxyClient.injectCredentialsAndInvokeV2(
                    Translator.getGrantRequest(model), proxyClient.client()::getGrant);
            if(response.grant() != null)
            {
                return true;
            }
            return false;
        } catch (final AccessDeniedException e) {
            logger.log(e.getMessage());
            throw new CfnAccessDeniedException(ResourceModel.TYPE_NAME, e);
        } catch (final InvalidParameterValueException e) {
            logger.log(e.getMessage());
            throw new CfnInvalidRequestException(ResourceModel.TYPE_NAME, e);
        }

    }

}

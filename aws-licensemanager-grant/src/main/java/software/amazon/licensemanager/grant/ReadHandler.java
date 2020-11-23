package software.amazon.licensemanager.grant;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.licensemanager.LicenseManagerClient;
import software.amazon.awssdk.services.licensemanager.model.GetGrantRequest;
import software.amazon.awssdk.services.licensemanager.model.GetGrantResponse;
import software.amazon.awssdk.services.licensemanager.model.InvalidParameterValueException;
import software.amazon.awssdk.services.licensemanager.model.ValidationException;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import static software.amazon.awssdk.services.licensemanager.model.GrantStatus.DELETED;
import static software.amazon.awssdk.services.licensemanager.model.GrantStatus.PENDING_DELETE;

public class ReadHandler extends BaseHandlerStd {
    private Logger logger;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<LicenseManagerClient> proxyClient,
            final Logger logger) {

        this.logger = logger;
        final ResourceModel model = request.getDesiredResourceState();

        // TODO: Adjust Progress Chain according to your implementation
        // https://github.com/aws-cloudformation/cloudformation-cli-java-plugin/blob/master/src/main/java/software/amazon/cloudformation/proxy/CallChain.java

        // STEP 1 [initialize a proxy context]
        return proxy.initiate("AWS-LicenseManager-Grant::Read", proxyClient, request.getDesiredResourceState(), callbackContext)

                // STEP 2 [TODO: construct a body of a request]
                .translateToServiceRequest(Translator::getGrantRequest)

                .makeServiceCall((awsRequest, sdkProxyClient) -> readResource(awsRequest, sdkProxyClient , model))

                // STEP 4 [TODO: gather all properties of the resource]
                // Implement client invocation of the read request through the proxyClient, which is already initialised with
                // caller credentials, correct region and retry settings
                .done(awsResponse -> ProgressEvent.defaultSuccessHandler(Translator.translateFromReadResponse(awsResponse)));
    }

    private GetGrantResponse readResource(
            final GetGrantRequest getGrantRequest,
            final ProxyClient<LicenseManagerClient> proxyClient,
            final ResourceModel model) {
        GetGrantResponse getGrantResponse = null;
        try {
            getGrantResponse = proxyClient.injectCredentialsAndInvokeV2(getGrantRequest, proxyClient.client()::getGrant);
            model.setGrantArn(getGrantResponse.grant().grantArn());
        } catch (ValidationException | InvalidParameterValueException e) {
            logger.log("Resource reading failed");
            throw new CfnInvalidRequestException(ResourceModel.TYPE_NAME, e);
        } catch (final AwsServiceException e) {
            logger.log("Resource reading failed");
            throw new CfnGeneralServiceException(ResourceModel.TYPE_NAME + e.getMessage(), e);
        }

        if (DELETED.equals(getGrantResponse.grant().grantStatus().toString())
                || PENDING_DELETE.equals(getGrantResponse.grant().grantStatus().toString())) {
            throw new CfnNotFoundException(model.getGrantArn(), ResourceModel.TYPE_NAME);
        }

        logger.log(String.format("%s has successfully been read.", ResourceModel.TYPE_NAME));
        return getGrantResponse;
    }

}

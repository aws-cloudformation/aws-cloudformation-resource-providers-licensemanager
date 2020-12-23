package software.amazon.licensemanager.grant;

import java.time.Duration;
import java.util.Arrays;

import software.amazon.awssdk.core.SdkClient;
import software.amazon.awssdk.services.licensemanager.LicenseManagerClient;
import software.amazon.awssdk.services.licensemanager.model.AllowedOperation;
import software.amazon.awssdk.services.licensemanager.model.CreateGrantVersionRequest;
import software.amazon.awssdk.services.licensemanager.model.CreateGrantVersionResponse;
import software.amazon.awssdk.services.licensemanager.model.GetGrantRequest;
import software.amazon.awssdk.services.licensemanager.model.GetGrantResponse;
import software.amazon.awssdk.services.licensemanager.model.Grant;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UpdateHandlerTest extends AbstractTestBase {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private ProxyClient<LicenseManagerClient> proxyClient;

    @Mock
    LicenseManagerClient sdkClient;

    @BeforeEach
    public void setup() {
        proxy = new AmazonWebServicesClientProxy(logger, MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());
        sdkClient = mock(LicenseManagerClient.class);
        proxyClient = MOCK_PROXY(proxy, sdkClient);
    }

    @Test
    public void handleRequest_SimpleSuccess() {
        Grant grant = Grant.builder()
                .grantArn("grantArn")
                .grantStatus("ACTIVE")
                .grantName("grantName")
                .parentArn("parentArn")
                .granteePrincipalArn("granteePrincipalArn")
                .homeRegion("us-east-1")
                .statusReason("statusReason")
                .grantedOperations(Arrays.asList(AllowedOperation.CREATE_GRANT))
                .version("2")
                .build();
        CreateGrantVersionResponse createGrantVersionResponse = CreateGrantVersionResponse.builder()
                .grantArn("grantArn").version("2").status("ACTIVE").build();

        GetGrantResponse getGrantResponse = GetGrantResponse.builder().grant(grant).build();

        when(proxyClient.client().createGrantVersion(any(CreateGrantVersionRequest.class)))
                .thenReturn(createGrantVersionResponse);
        when(proxyClient.client().getGrant(any(GetGrantRequest.class)))
                .thenReturn(getGrantResponse);

        final UpdateHandler handler = new UpdateHandler();

        final ResourceModel model = ResourceModel.builder()
                .grantArn("grantArn")
                .grantName("grantName")
                .parentArn("parentArn")
                .granteePrincipalArn("granteePrincipalArn")
                .homeRegion("us-east-1")
                .grantedOperations(Arrays.asList("CreateGrant"))
                .version("2")
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
            .desiredResourceState(model)
            .build();

        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
        verify(proxyClient.client()).createGrantVersion(any(CreateGrantVersionRequest.class));
    }
}

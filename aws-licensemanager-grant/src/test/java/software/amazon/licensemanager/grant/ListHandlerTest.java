package software.amazon.licensemanager.grant;

import software.amazon.awssdk.services.licensemanager.LicenseManagerClient;
import software.amazon.awssdk.services.licensemanager.model.AllowedOperation;
import software.amazon.awssdk.services.licensemanager.model.Grant;
import software.amazon.awssdk.services.licensemanager.model.ListDistributedGrantsRequest;
import software.amazon.awssdk.services.licensemanager.model.ListDistributedGrantsResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ListHandlerTest extends AbstractTestBase {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private ProxyClient<LicenseManagerClient> proxyClient;

    @Mock
    LicenseManagerClient licenseManagerClient;


    @BeforeEach
    public void setup() {
        proxy = new AmazonWebServicesClientProxy(logger, MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());
        licenseManagerClient = mock(LicenseManagerClient.class);
        proxyClient = MOCK_PROXY(proxy, licenseManagerClient);
    }

    @Test
    public void handleRequest_SimpleSuccess() {
        List<Grant> grants = new ArrayList<>();
        grants.add(Grant.builder()
                .grantArn("grantArn")
                .grantStatus("ACTIVE")
                .grantName("grantName")
                .parentArn("parentArn")
                .granteePrincipalArn("granteePrincipalArn")
                .homeRegion("us-east-1")
                .statusReason("statusReason")
                .grantedOperations(Arrays.asList(AllowedOperation.CREATE_GRANT))
                .version("1")
                .build());

        final ListDistributedGrantsResponse listDistributedGrantsResponse = ListDistributedGrantsResponse.builder()
                .grants(grants).build();
        when(proxyClient.client().listDistributedGrants(any(ListDistributedGrantsRequest.class)))
                .thenReturn(listDistributedGrantsResponse);

        final ListHandler handler = new ListHandler();

        final ResourceModel model = ResourceModel.builder().build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
            .desiredResourceState(model)
            .build();

        final ProgressEvent<ResourceModel, CallbackContext> response =
            handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isNull();
        assertThat(response.getResourceModels()).isNotNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
        assertThat(response.getResourceModels().size() > 0);
        assertThat(response.getResourceModels().get(0).getGrantArn().equals("grantArn"));
    }
}

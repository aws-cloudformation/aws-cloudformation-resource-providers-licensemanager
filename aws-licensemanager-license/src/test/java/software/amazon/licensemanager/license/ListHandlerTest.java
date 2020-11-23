package software.amazon.licensemanager.license;

import software.amazon.awssdk.services.licensemanager.LicenseManagerClient;
import software.amazon.awssdk.services.licensemanager.model.ConsumptionConfiguration;
import software.amazon.awssdk.services.licensemanager.model.DatetimeRange;
import software.amazon.awssdk.services.licensemanager.model.Entitlement;
import software.amazon.awssdk.services.licensemanager.model.IssuerDetails;
import software.amazon.awssdk.services.licensemanager.model.License;
import software.amazon.awssdk.services.licensemanager.model.ListLicensesRequest;
import software.amazon.awssdk.services.licensemanager.model.ListLicensesResponse;
import software.amazon.awssdk.services.licensemanager.model.Metadata;
import software.amazon.awssdk.services.licensemanager.model.ProvisionalConfiguration;
import software.amazon.awssdk.services.licensemanager.model.RenewType;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
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
public class ListHandlerTest extends AbstractTestBase{

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
        List<License> licenses = new ArrayList<>();
        IssuerDetails issuer = IssuerDetails.builder().name("test").signKey("test").build();
        DatetimeRange validity = DatetimeRange.builder().begin("begin").end("end").build();
        software.amazon.awssdk.services.licensemanager.model.Entitlement entitlement = Entitlement.builder().name("e1").maxCount(100L).allowCheckIn(true)
                .unit("Count").overage(false).build();
        software.amazon.awssdk.services.licensemanager.model.ProvisionalConfiguration provisionalConfiguration =
                ProvisionalConfiguration.builder().maxTimeToLiveInMinutes(60).build();
        software.amazon.awssdk.services.licensemanager.model.ConsumptionConfiguration consumptionConfiguration = ConsumptionConfiguration.builder()
                .provisionalConfiguration(provisionalConfiguration).renewType(RenewType.WEEKLY).build();
        software.amazon.awssdk.services.licensemanager.model.Metadata metadata = Metadata.builder().name("test").value("test").build();
        licenses.add(License.builder()
                .licenseArn("licenseArn")
                .productSKU("test")
                .issuer(issuer)
                .status("AVAILABLE")
                .validity(validity)
                .beneficiary("beneficiary")
                .entitlements(Arrays.asList(entitlement))
                .consumptionConfiguration(consumptionConfiguration)
                .homeRegion("us-east-1")
                .licenseMetadata(Arrays.asList(metadata))
                .version("1")
                .build());

        final ListLicensesResponse listLicensesResponse = ListLicensesResponse.builder()
                .licenses(licenses).build();
        when(proxyClient.client().listLicenses(any(ListLicensesRequest.class)))
                .thenReturn(listLicensesResponse);

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
        assertThat(response.getResourceModels().get(0).getLicenseArn().equals("licenseArn"));
    }
}

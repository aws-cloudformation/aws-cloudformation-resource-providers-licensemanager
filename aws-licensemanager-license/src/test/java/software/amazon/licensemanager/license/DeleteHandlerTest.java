package software.amazon.licensemanager.license;

import java.time.Duration;
import java.util.Arrays;

import software.amazon.awssdk.core.SdkClient;
import software.amazon.awssdk.services.licensemanager.LicenseManagerClient;
import software.amazon.awssdk.services.licensemanager.model.ConsumptionConfiguration;
import software.amazon.awssdk.services.licensemanager.model.DatetimeRange;
import software.amazon.awssdk.services.licensemanager.model.DeleteLicenseRequest;
import software.amazon.awssdk.services.licensemanager.model.DeleteLicenseResponse;
import software.amazon.awssdk.services.licensemanager.model.Entitlement;
import software.amazon.awssdk.services.licensemanager.model.GetLicenseRequest;
import software.amazon.awssdk.services.licensemanager.model.GetLicenseResponse;
import software.amazon.awssdk.services.licensemanager.model.IssuerDetails;
import software.amazon.awssdk.services.licensemanager.model.License;
import software.amazon.awssdk.services.licensemanager.model.Metadata;
import software.amazon.awssdk.services.licensemanager.model.ProvisionalConfiguration;
import software.amazon.awssdk.services.licensemanager.model.RenewType;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DeleteHandlerTest extends AbstractTestBase {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private ProxyClient<LicenseManagerClient> proxyClient;

    @Mock
    LicenseManagerClient sdkClient;

    private License license;

    @BeforeEach
    public void setup() {
        proxy = new AmazonWebServicesClientProxy(logger, MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());
        sdkClient = mock(LicenseManagerClient.class);
        proxyClient = MOCK_PROXY(proxy, sdkClient);

        IssuerDetails issuer = IssuerDetails.builder().name("test").signKey("test").build();
        DatetimeRange validity = DatetimeRange.builder().begin("begin").end("end").build();
        software.amazon.awssdk.services.licensemanager.model.Entitlement entitlement = Entitlement.builder().name("e1").maxCount(100L).allowCheckIn(true)
                .unit("Count").overage(false).build();
        software.amazon.awssdk.services.licensemanager.model.ProvisionalConfiguration provisionalConfiguration =
                ProvisionalConfiguration.builder().maxTimeToLiveInMinutes(60).build();
        software.amazon.awssdk.services.licensemanager.model.ConsumptionConfiguration consumptionConfiguration = ConsumptionConfiguration.builder()
                .provisionalConfiguration(provisionalConfiguration).renewType(RenewType.WEEKLY).build();
        software.amazon.awssdk.services.licensemanager.model.Metadata metadata = Metadata.builder().name("test").value("test").build();
        license = License.builder()
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
                .build();
    }

    @Test
    public void handleRequest_SimpleSuccess() {
        final GetLicenseResponse getLicenseResponse = GetLicenseResponse.builder().license(license).build();

        final DeleteLicenseResponse deleteLicenseResponse = DeleteLicenseResponse.builder()
                .status("Deleted").deletionDate("testDate").build();

        when(proxyClient.client().getLicense(any(GetLicenseRequest.class)))
                .thenReturn(getLicenseResponse);

        when(proxyClient.client().deleteLicense(any(DeleteLicenseRequest.class))).thenReturn(deleteLicenseResponse);

        final DeleteHandler handler = new DeleteHandler();

        final ResourceModel model = ResourceModel.builder().build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
            .desiredResourceState(model)
            .build();

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isNull();
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }
}

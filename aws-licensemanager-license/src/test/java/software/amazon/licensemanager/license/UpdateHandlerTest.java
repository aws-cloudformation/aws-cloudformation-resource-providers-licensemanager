package software.amazon.licensemanager.license;

import java.time.Duration;
import java.util.Arrays;

import software.amazon.awssdk.services.licensemanager.LicenseManagerClient;
import software.amazon.awssdk.services.licensemanager.model.ConsumptionConfiguration;
import software.amazon.awssdk.services.licensemanager.model.CreateLicenseVersionRequest;
import software.amazon.awssdk.services.licensemanager.model.CreateLicenseVersionResponse;
import software.amazon.awssdk.services.licensemanager.model.DatetimeRange;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
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
        IssuerDetails issuer = IssuerDetails.builder().name("test").signKey("test").build();
        DatetimeRange validity = DatetimeRange.builder().begin("begin").end("end").build();
        software.amazon.awssdk.services.licensemanager.model.Entitlement entitlement = Entitlement.builder().name("e1").maxCount(100L).allowCheckIn(true)
                .unit("Count").overage(false).build();
        software.amazon.awssdk.services.licensemanager.model.ProvisionalConfiguration provisionalConfiguration =
                ProvisionalConfiguration.builder().maxTimeToLiveInMinutes(60).build();
        software.amazon.awssdk.services.licensemanager.model.ConsumptionConfiguration consumptionConfiguration = ConsumptionConfiguration.builder()
                .provisionalConfiguration(provisionalConfiguration).renewType(RenewType.WEEKLY).build();
        software.amazon.awssdk.services.licensemanager.model.Metadata metadata = Metadata.builder().name("test").value("test").build();
        License license = License.builder()
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

        CreateLicenseVersionResponse createLicenseVersionResponse = CreateLicenseVersionResponse.builder()
                .licenseArn("licenseArn").version("2").status("AVAILABLE").build();

        GetLicenseResponse getLicenseResponse = GetLicenseResponse.builder().license(license).build();

        when(proxyClient.client().createLicenseVersion(any(CreateLicenseVersionRequest.class)))
                .thenReturn(createLicenseVersionResponse);
        when(proxyClient.client().getLicense(any(GetLicenseRequest.class)))
                .thenReturn(getLicenseResponse);

        final UpdateHandler handler = new UpdateHandler();

        IssuerData issuerData = IssuerData.builder().name("test").signKey("test").build();
        ValidityDateFormat validityDateFormat = ValidityDateFormat.builder().begin("begin").end("end").build();
        software.amazon.licensemanager.license.Entitlement entitlement1 =
                software.amazon.licensemanager.license.Entitlement.builder().name("e1").maxCount(100).allowCheckIn(true)
                        .unit("Count").overage(false).build();
        software.amazon.licensemanager.license.ProvisionalConfiguration provisionalConfiguration1 =
                software.amazon.licensemanager.license.ProvisionalConfiguration.builder()
                        .maxTimeToLiveInMinutes(60).build();
        software.amazon.licensemanager.license.ConsumptionConfiguration consumptionConfiguration1 =
                software.amazon.licensemanager.license.ConsumptionConfiguration.builder()
                        .provisionalConfiguration(provisionalConfiguration1).renewType("Weekly").build();
        software.amazon.licensemanager.license.Metadata metadata1 = software.amazon.licensemanager.license.Metadata
                .builder().name("test").value("test").build();
        final ResourceModel model = ResourceModel.builder()
                .licenseArn("licenseArn")
                .productSKU("test")
                .issuer(issuerData)
                .status("AVAILABLE")
                .validity(validityDateFormat)
                .beneficiary("beneficiary")
                .entitlements(Arrays.asList(entitlement1))
                .consumptionConfiguration(consumptionConfiguration1)
                .homeRegion("us-east-1")
                .licenseMetadata(Arrays.asList(metadata1))
                .version("1")
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
        verify(proxyClient.client()).createLicenseVersion(any(CreateLicenseVersionRequest.class));
    }
}

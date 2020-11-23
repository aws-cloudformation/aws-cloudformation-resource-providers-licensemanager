package software.amazon.licensemanager.license;

import software.amazon.awssdk.services.licensemanager.LicenseManagerClient;
import software.amazon.cloudformation.LambdaWrapper;

public class ClientBuilder {
  public static LicenseManagerClient getClient() {
    return LicenseManagerClient.builder()
            .httpClient(LambdaWrapper.HTTP_CLIENT)
            .build();
  }
}

package software.amazon.licensemanager.license;

import software.amazon.awssdk.services.licensemanager.model.CreateLicenseRequest;
import software.amazon.awssdk.services.licensemanager.model.CreateLicenseVersionRequest;
import software.amazon.awssdk.services.licensemanager.model.DatetimeRange;
import software.amazon.awssdk.services.licensemanager.model.DeleteLicenseRequest;
import software.amazon.awssdk.services.licensemanager.model.Filter;
import software.amazon.awssdk.services.licensemanager.model.GetLicenseRequest;
import software.amazon.awssdk.services.licensemanager.model.GetLicenseResponse;
import software.amazon.awssdk.services.licensemanager.model.Issuer;
import software.amazon.awssdk.services.licensemanager.model.License;
import software.amazon.awssdk.services.licensemanager.model.ListLicensesRequest;
import software.amazon.awssdk.services.licensemanager.model.ListLicensesResponse;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static software.amazon.awssdk.services.licensemanager.model.LicenseStatus.DELETED;
import static software.amazon.awssdk.services.licensemanager.model.LicenseStatus.PENDING_DELETE;

/**
 * This class is a centralized placeholder for
 *  - api request construction
 *  - object translation to/from aws sdk
 *  - resource model construction for read/list handlers
 */

public class Translator {

  /**
   * Request to create a resource
   * @param model resource model
   * @return awsRequest the aws service request to create a resource
   */
  static CreateLicenseRequest translateToCreateRequest(final ResourceModel model) {
    Issuer issuer = Issuer.builder()
            .name(model.getIssuer().getName())
            .signKey(model.getIssuer().getSignKey()).build();
    DatetimeRange validity = DatetimeRange.builder()
            .begin(model.getValidity().getBegin())
            .end(model.getValidity().getEnd()).build();
    List<software.amazon.awssdk.services.licensemanager.model.Entitlement> entitlements = new ArrayList<>();
    for (Entitlement entitlement : model.getEntitlements()) {
      entitlements.add(software.amazon.awssdk.services.licensemanager.model.Entitlement.builder().name(entitlement.getName())
              .unit(entitlement.getUnit())
              .allowCheckIn(entitlement.getAllowCheckIn())
              .maxCount(Long.valueOf(entitlement.getMaxCount()))
              .overage(entitlement.getOverage()).build());
    }
    software.amazon.awssdk.services.licensemanager.model.ProvisionalConfiguration provisionalConfiguration =
            software.amazon.awssdk.services.licensemanager.model.ProvisionalConfiguration.builder()
            .maxTimeToLiveInMinutes(model.getConsumptionConfiguration().getProvisionalConfiguration()
                    .getMaxTimeToLiveInMinutes()).build();

    software.amazon.awssdk.services.licensemanager.model.ConsumptionConfiguration consumptionConfiguration =
            software.amazon.awssdk.services.licensemanager.model.ConsumptionConfiguration.builder()
            .renewType(model.getConsumptionConfiguration().getRenewType())
            .provisionalConfiguration(provisionalConfiguration)
            .build();

    List<software.amazon.awssdk.services.licensemanager.model.Metadata> licenseMetadata = new ArrayList<>();
    for (Metadata metadata : model.getLicenseMetadata()) {
      licenseMetadata.add(software.amazon.awssdk.services.licensemanager.model.Metadata.builder()
              .name(metadata.getName())
              .value(metadata.getValue()).build());
    }

    final CreateLicenseRequest createLicenseRequest = CreateLicenseRequest.builder()
            .productSKU(model.getProductSKU())
            .licenseName(model.getLicenseName())
            .productName(model.getProductName())
            .beneficiary(model.getBeneficiary())
            .clientToken(model.getClientToken())
            .homeRegion(model.getHomeRegion())
            .issuer(issuer)
            .validity(validity)
            .entitlements(entitlements)
            .consumptionConfiguration(consumptionConfiguration)
            .licenseMetadata(licenseMetadata)
            .build();

    return createLicenseRequest;
  }

  /**
   * Request to read a resource
   * @param model resource model
   * @return awsRequest the aws service request to describe a resource
   */
  static ListLicensesRequest listLicensesRequest(final ResourceModel model) {
    final ListLicensesRequest listLicensesRequest = ListLicensesRequest
            .builder().licenseArns(model.getLicenseArn()).build();
    return listLicensesRequest;
  }

  static GetLicenseRequest getLicenseRequest(final ResourceModel model) {
    final GetLicenseRequest getLicenseRequest = GetLicenseRequest.builder()
            .licenseArn(model.getLicenseArn())
            .version(model.getVersion())
            .build();
    return getLicenseRequest;
  }

  /**
   * Translates resource object from sdk into a resource model
   * @param getLicenseResponse the aws service describe resource response
   * @return model resource model
   */
  static ResourceModel translateFromReadResponse(final GetLicenseResponse getLicenseResponse) {
    // e.g. https://github.com/aws-cloudformation/aws-cloudformation-resource-providers-logs/blob/master/aws-logs-loggroup/src/main/java/software/amazon/logs/loggroup/Translator.java#L58
    License license = getLicenseResponse.license();
    IssuerData issuerData = IssuerData.builder().name(license.issuer().name())
            .signKey(license.issuer().signKey()).build();
    ValidityDateFormat validityDateFormat = ValidityDateFormat.builder().begin(license.validity().begin())
            .end(license.validity().end()).build();
    List<Entitlement> entitlements = new ArrayList<>();
    for (software.amazon.awssdk.services.licensemanager.model.Entitlement entitlement : license.entitlements()) {
      entitlements.add(Entitlement.builder().name(entitlement.name())
              .unit(entitlement.unitAsString())
              .allowCheckIn(entitlement.allowCheckIn())
              .maxCount(entitlement.maxCount().intValue())
              .overage(entitlement.overage()).build());
    }

    ProvisionalConfiguration provisionalConfiguration = ProvisionalConfiguration.builder()
                    .maxTimeToLiveInMinutes(license.consumptionConfiguration().provisionalConfiguration()
                            .maxTimeToLiveInMinutes()).build();

    ConsumptionConfiguration consumptionConfiguration = ConsumptionConfiguration.builder()
            .renewType(license.consumptionConfiguration().renewType().toString())
            .provisionalConfiguration(provisionalConfiguration)
            .build();

    List<Metadata> metadataList = new ArrayList<>();
    for (software.amazon.awssdk.services.licensemanager.model.Metadata metadata : license.licenseMetadata()) {
      metadataList.add(Metadata.builder().name(metadata.name())
              .value(metadata.value()).build());
    }

    return ResourceModel.builder()
            .licenseArn(license.licenseArn())
            .licenseName(license.licenseName())
            .productName(license.productName())
            .productSKU(license.productSKU())
            .issuer(issuerData)
            .status(license.status().toString())
            .validity(validityDateFormat)
            .homeRegion(license.homeRegion())
            .beneficiary(license.beneficiary())
            .entitlements(entitlements)
            .consumptionConfiguration(consumptionConfiguration)
            .version(license.version())
            .licenseMetadata(metadataList)
            .build();
  }

  /**
   * Request to delete a resource
   * @param model resource model
   * @return awsRequest the aws service request to delete a resource
   */
  static DeleteLicenseRequest translateToDeleteRequest(final ResourceModel model) {
    final DeleteLicenseRequest deleteListenerRequest  = DeleteLicenseRequest.builder()
            .licenseArn(model.getLicenseArn())
            .sourceVersion(model.getSourceVersion()).build();
    return deleteListenerRequest;
  }

  /**
   * Request to update properties of a previously created resource
   * @param model resource model
   * @return awsRequest the aws service request to modify a resource
   */
  static CreateLicenseVersionRequest translateToUpdateRequest(final ResourceModel model) {
    Issuer issuer = Issuer.builder()
            .name(model.getIssuer().getName())
            .signKey(model.getIssuer().getSignKey()).build();
    DatetimeRange validity = DatetimeRange.builder()
            .begin(model.getValidity().getBegin())
            .end(model.getValidity().getEnd()).build();
    List<software.amazon.awssdk.services.licensemanager.model.Entitlement> entitlements = new ArrayList<>();
    for (Entitlement entitlement : model.getEntitlements()) {
      entitlements.add(software.amazon.awssdk.services.licensemanager.model.Entitlement.builder().name(entitlement.getName())
              .unit(entitlement.getUnit())
              .allowCheckIn(entitlement.getAllowCheckIn())
              .maxCount(Long.valueOf(entitlement.getMaxCount()))
              .overage(entitlement.getOverage()).build());
    }
    software.amazon.awssdk.services.licensemanager.model.ProvisionalConfiguration provisionalConfiguration =
            software.amazon.awssdk.services.licensemanager.model.ProvisionalConfiguration.builder()
                    .maxTimeToLiveInMinutes(model.getConsumptionConfiguration().getProvisionalConfiguration()
                            .getMaxTimeToLiveInMinutes()).build();

    software.amazon.awssdk.services.licensemanager.model.ConsumptionConfiguration consumptionConfiguration =
            software.amazon.awssdk.services.licensemanager.model.ConsumptionConfiguration.builder()
            .renewType(model.getConsumptionConfiguration().getRenewType())
            .provisionalConfiguration(provisionalConfiguration)
            .build();

    List<software.amazon.awssdk.services.licensemanager.model.Metadata> licenseMetadata = new ArrayList<>();
    for (Metadata metadata : model.getLicenseMetadata()) {
      licenseMetadata.add(software.amazon.awssdk.services.licensemanager.model.Metadata.builder()
              .name(metadata.getName())
              .value(metadata.getValue()).build());
    }

    final CreateLicenseVersionRequest createLicenseVersionRequest = CreateLicenseVersionRequest.builder()
            .licenseArn(model.getLicenseArn())
            .licenseName(model.getLicenseName())
            .productName(model.getProductName())
            .sourceVersion(model.getSourceVersion())
            .issuer(issuer)
            .homeRegion(model.getHomeRegion())
            .validity(validity)
            .licenseMetadata(licenseMetadata)
            .entitlements(entitlements)
            .consumptionConfiguration(consumptionConfiguration)
            .status(model.getStatus())
            .clientToken(model.getClientToken())
            .build();
    return createLicenseVersionRequest;
  }

  /**
   * Request to update properties of a previously created resource
   * @return awsRequest the aws service request to describe resources within aws account
   */
  static ListLicensesRequest translateToListRequest(final ResourceModel model) {
    List<Filter> filters = new ArrayList<>();
    if (model.getFilters() != null) {
      filters = model.getFilters().stream().map(filter -> convertFilter(filter)).collect(Collectors.toList());
    }
    final ListLicensesRequest listLicensesRequest = ListLicensesRequest.builder()
            .licenseArns(model.getLicenseArns())
            .nextToken(model.getNextToken())
            .filters(filters)
            .maxResults(model.getMaxResults()).build();
    return listLicensesRequest;
  }

  public static Filter convertFilter(final software.amazon.licensemanager.license.Filter inputFilter) {
    if (inputFilter == null) {
      return null;
    }
    return  Filter.builder().name(inputFilter.getName())
            .values(inputFilter.getValues()).build();
  }

  /**
   * Translates resource objects from sdk into a resource model (primary identifier only)
   * @param listLicensesResponse list license response
   * @return list of resource models
   */
  static List<ResourceModel> translateFromListRequest(final ListLicensesResponse listLicensesResponse) {
    // e.g. e.g. https://github.com/aws-cloudformation/aws-cloudformation-resource-providers-logs/blob/master/aws-logs-loggroup/src/main/java/software/amazon/logs/loggroup/Translator.java#L81
    return streamOfOrEmpty(listLicensesResponse.licenses())
            .filter(license -> !DELETED.equals(license.status().toString())
                    && !PENDING_DELETE.equals(license.status().toString()))
            .map(license -> ResourceModel.builder()
                    .licenseArn(license.licenseArn())
                    .build())
            .collect(Collectors.toList());
  }

  private static <T> Stream<T> streamOfOrEmpty(final Collection<T> collection) {
    return Optional.ofNullable(collection)
            .map(Collection::stream)
            .orElseGet(Stream::empty);
  }
}
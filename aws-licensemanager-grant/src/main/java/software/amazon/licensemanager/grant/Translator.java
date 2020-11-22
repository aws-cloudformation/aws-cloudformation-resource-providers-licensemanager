package software.amazon.licensemanager.grant;

import software.amazon.awssdk.services.licensemanager.model.CreateGrantRequest;
import software.amazon.awssdk.services.licensemanager.model.CreateGrantResponse;
import software.amazon.awssdk.services.licensemanager.model.CreateGrantVersionRequest;
import software.amazon.awssdk.services.licensemanager.model.DeleteGrantRequest;
import software.amazon.awssdk.services.licensemanager.model.Filter;
import software.amazon.awssdk.services.licensemanager.model.GetGrantRequest;
import software.amazon.awssdk.services.licensemanager.model.GetGrantResponse;
import software.amazon.awssdk.services.licensemanager.model.Grant;
import software.amazon.awssdk.services.licensemanager.model.ListDistributedGrantsRequest;
import software.amazon.awssdk.services.licensemanager.model.ListDistributedGrantsResponse;
import software.amazon.awssdk.services.licensemanager.model.ListLicensesRequest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static software.amazon.awssdk.services.licensemanager.model.GrantStatus.DELETED;
import static software.amazon.awssdk.services.licensemanager.model.GrantStatus.PENDING_DELETE;

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
  static CreateGrantRequest translateToCreateRequest(final ResourceModel model) {
    final CreateGrantRequest createGrantRequest = CreateGrantRequest.builder()
            .clientToken(model.getClientToken())
            .homeRegion(model.getHomeRegion())
            .grantName(model.getGrantName())
            .licenseArn(model.getLicenseArn())
            .principals(model.getPrincipals())
            .allowedOperationsWithStrings(model.getAllowedOperations())
            .build();

    return createGrantRequest;
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

  static GetGrantRequest getGrantRequest(final ResourceModel model) {
    final GetGrantRequest getGrantRequest = GetGrantRequest.builder()
            .grantArn(model.getGrantArn())
            .version(model.getVersion())
            .build();
    return getGrantRequest;
  }

  /**
   * Translates resource object from sdk into a resource model
   * @param createGrantResponse the aws service create resource response
   * @return model resource model
   */
  static ResourceModel translateFromCreateResponse(final CreateGrantResponse createGrantResponse) {
    // e.g. https://github.com/aws-cloudformation/aws-cloudformation-resource-providers-logs/blob/master/aws-logs-loggroup/src/main/java/software/amazon/logs/loggroup/Translator.java#L58
    return ResourceModel.builder()
            .grantArn(createGrantResponse.grantArn())
            .status(createGrantResponse.status().toString())
            .version(createGrantResponse.version())
            .build();
  }

  /**
   * Translates resource object from sdk into a resource model
   * @param getGrantResponse the aws service describe resource response
   * @return model resource model
   */
  static ResourceModel translateFromReadResponse(final GetGrantResponse getGrantResponse) {
    // e.g. https://github.com/aws-cloudformation/aws-cloudformation-resource-providers-logs/blob/master/aws-logs-loggroup/src/main/java/software/amazon/logs/loggroup/Translator.java#L58
    Grant grant = getGrantResponse.grant();
    return ResourceModel.builder()
            .grantArn(grant.grantArn())
            .grantName(grant.grantName())
            .parentArn(grant.parentArn())
            .licenseArn(grant.licenseArn())
            .granteePrincipalArn(grant.granteePrincipalArn())
            .homeRegion(grant.homeRegion())
            .grantStatus(grant.grantStatus().toString())
            .statusReason(grant.statusReason())
            .version(grant.version())
            .grantedOperations(grant.grantedOperationsAsStrings())
            .build();
  }

  /**
   * Request to delete a resource
   * @param model resource model
   * @return awsRequest the aws service request to delete a resource
   */
  static DeleteGrantRequest translateToDeleteRequest(final ResourceModel model) {
    final DeleteGrantRequest deleteGrantRequest  = DeleteGrantRequest.builder()
            .grantArn(model.getGrantArn())
            .version(model.getVersion()).build();
    return deleteGrantRequest;
  }

  /**
   * Request to update properties of a previously created resource
   * @param model resource model
   * @return awsRequest the aws service request to modify a resource
   */
  static CreateGrantVersionRequest translateToUpdateRequest(final ResourceModel model) {
    final CreateGrantVersionRequest createLicenseVersionRequest = CreateGrantVersionRequest.builder()
            .clientToken(model.getClientToken())
            .grantArn(model.getGrantArn())
            .grantName(model.getGrantName())
            .allowedOperationsWithStrings(model.getGrantedOperations())
            .status(model.getStatus())
            .sourceVersion(model.getSourceVersion())
            .build();
    return createLicenseVersionRequest;
  }

  /**
   * Request to update properties of a previously created resource
   * @return awsRequest the aws service request to describe resources within aws account
   */
  static ListDistributedGrantsRequest translateToListRequest(final ResourceModel model) {
    List<Filter> filters = new ArrayList<>();
    if (model.getFilters() != null) {
      filters = model.getFilters().stream().map(filter -> convertFilter(filter)).collect(Collectors.toList());
    }
    final ListDistributedGrantsRequest listDistributedGrantsRequest = ListDistributedGrantsRequest.builder()
            .grantArns(model.getGrantArns())
            .nextToken(model.getNextToken())
            .filters(filters)
            .maxResults(model.getMaxResults()).build();
    return listDistributedGrantsRequest;
  }

  public static Filter convertFilter(final software.amazon.licensemanager.grant.Filter inputFilter) {
    if (inputFilter == null) {
      return null;
    }
    return  Filter.builder().name(inputFilter.getName())
            .values(inputFilter.getValues()).build();
  }

  /**
   * Translates resource objects from sdk into a resource model (primary identifier only)
   * @param listDistributedGrantsResponse list distributed grants response
   * @return list of resource models
   */
  static List<ResourceModel> translateFromListRequest(final ListDistributedGrantsResponse listDistributedGrantsResponse) {
    // e.g. e.g. https://github.com/aws-cloudformation/aws-cloudformation-resource-providers-logs/blob/master/aws-logs-loggroup/src/main/java/software/amazon/logs/loggroup/Translator.java#L81
    return streamOfOrEmpty(listDistributedGrantsResponse.grants())
            .filter(grant -> !DELETED.equals(grant.grantStatus().toString())
                    && !PENDING_DELETE.equals(grant.grantStatus().toString()))
            .map(resource -> ResourceModel.builder()
                    .grantArn(resource.grantArn())
                    .build())
            .collect(Collectors.toList());
  }

  private static <T> Stream<T> streamOfOrEmpty(final Collection<T> collection) {
    return Optional.ofNullable(collection)
            .map(Collection::stream)
            .orElseGet(Stream::empty);
  }
}
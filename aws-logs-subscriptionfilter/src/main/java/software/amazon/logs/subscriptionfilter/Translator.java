package software.amazon.logs.subscriptionfilter;

import com.amazonaws.util.StringUtils;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.awscore.AwsResponse;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import software.amazon.awssdk.services.cloudwatchlogs.model.DeleteSubscriptionFilterRequest;
import software.amazon.awssdk.services.cloudwatchlogs.model.DescribeSubscriptionFiltersRequest;
import software.amazon.awssdk.services.cloudwatchlogs.model.DescribeSubscriptionFiltersResponse;
import software.amazon.awssdk.services.cloudwatchlogs.model.PutSubscriptionFilterRequest;
import software.amazon.awssdk.services.cloudwatchlogs.model.SubscriptionFilter;

/**
 * This class is a centralized placeholder for
 *  - api request construction
 *  - object translation to/from aws sdk
 *  - resource model construction for read/list handlers
 */

public class Translator {

  public static final String DEFAULT_DISTRIBUTION = "ByLogStream";

  /**
   * Request to create a resource
   * @param model resource model
   * @return awsRequest the aws service request to create a resource
   */
  static PutSubscriptionFilterRequest translateToCreateRequest(final ResourceModel model) {
    if (StringUtils.isNullOrEmpty(model.getDistribution())) {
      model.setDistribution(DEFAULT_DISTRIBUTION);
    }
    return PutSubscriptionFilterRequest.builder()
        .logGroupName(model.getLogGroupName())
        .filterName(model.getFilterName())
        .filterPattern(model.getFilterPattern())
        .destinationArn(model.getDestinationArn())
        .roleArn(model.getRoleArn())
        .distribution(model.getDistribution())
        .build();
  }
  /**
   * Request to read a resource
   * @param model resource model
   * @return awsRequest the aws service request to describe a resource
   */
  static DescribeSubscriptionFiltersRequest translateToReadRequest(final ResourceModel model) {
      return DescribeSubscriptionFiltersRequest.builder()
          .filterNamePrefix(model.getFilterName())
          .logGroupName(model.getLogGroupName())
          .limit(1)
          .build();
  }

  /**
   * Translates resource object from sdk into a resource model
   * @param awsResponse the aws service describe resource response
   * @return model resource model
   */
  static ResourceModel translateFromReadResponse(final AwsResponse awsResponse) {
    DescribeSubscriptionFiltersResponse response = (DescribeSubscriptionFiltersResponse) awsResponse;
    List<SubscriptionFilter> subscriptionFilters = response.subscriptionFilters();
    SubscriptionFilter subscriptionFilter = subscriptionFilters.size() > 0 ? subscriptionFilters.get(0) : null;
    if (subscriptionFilter == null) {
      return ResourceModel.builder()
          .build();
    } else {
      return ResourceModel.builder()
          .destinationArn(subscriptionFilter.destinationArn())
          .distribution(subscriptionFilter.distributionAsString())
          .filterName(subscriptionFilter.filterName())
          .filterPattern(subscriptionFilter.filterPattern())
          .logGroupName(subscriptionFilter.logGroupName())
          .roleArn(subscriptionFilter.roleArn())
          .build();
    }
  }

  /**
   * Request to delete a resource
   * @param model resource model
   * @return awsRequest the aws service request to delete a resource
   */
  static DeleteSubscriptionFilterRequest translateToDeleteRequest(final ResourceModel model) {
    return DeleteSubscriptionFilterRequest.builder()
        .filterName(model.getFilterName())
        .logGroupName(model.getLogGroupName())
        .build();
  }

  /**
   * Request to update properties of a previously created resource
   * @param model resource model
   * @return awsRequest the aws service request to modify a resource
   */
  static PutSubscriptionFilterRequest translateToFirstUpdateRequest(final ResourceModel model) {
    return translateToCreateRequest(model);
  }

  /**
   * Request to update some other properties that could not be provisioned through first update request
   * @param model resource model
   * @return awsRequest the aws service request to modify a resource
   */
  static PutSubscriptionFilterRequest translateToSecondUpdateRequest(final ResourceModel model) {
    return translateToCreateRequest(model);
  }

  /**
   * Request to list resources
   * @param nextToken token passed to the aws service list resources request
   * @return awsRequest the aws service request to list resources within aws account
   */
  static DescribeSubscriptionFiltersRequest translateToListRequest(final String nextToken) {
    return DescribeSubscriptionFiltersRequest.builder()
        .nextToken(nextToken)
        .limit(50)
        .build();
  }

  /**
   * Translates resource objects from sdk into a resource model (primary identifier only)
   * @param awsResponse the aws service describe resource response
   * @return list of resource models
   */
  static List<ResourceModel> translateFromListRequest(final AwsResponse awsResponse) {
    DescribeSubscriptionFiltersResponse response = (DescribeSubscriptionFiltersResponse) awsResponse;
    return streamOfOrEmpty(response.subscriptionFilters())
        .map(resource -> ResourceModel.builder()
            .destinationArn(resource.destinationArn())
            .distribution(resource.distributionAsString())
            .filterName(resource.filterName())
            .filterPattern(resource.filterPattern())
            .logGroupName(resource.logGroupName())
            .roleArn(resource.roleArn())
            .build())
        .collect(Collectors.toList());
  }

  private static <T> Stream<T> streamOfOrEmpty(final Collection<T> collection) {
    return Optional.ofNullable(collection)
        .map(Collection::stream)
        .orElseGet(Stream::empty);
  }

  static List<SubscriptionFilter> translateToSDK
      (final ResourceModel model) {
    SubscriptionFilter subscriptionFilter = SubscriptionFilter.builder()
        .destinationArn(model.getDestinationArn())
        .distribution(model.getDistribution())
        .filterName(model.getFilterName())
        .filterPattern(model.getFilterPattern())
        .logGroupName(model.getLogGroupName())
        .roleArn(model.getRoleArn())
        .build();
    List<SubscriptionFilter> subscriptionFilters = new ArrayList<>();
    subscriptionFilters.add(subscriptionFilter);
    return subscriptionFilters;
  }
}

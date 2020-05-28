package software.amazon.logs.subscriptionfilter;

import java.util.Objects;
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsClient;

import software.amazon.awssdk.awscore.AwsResponse;
import software.amazon.awssdk.services.cloudwatchlogs.model.DescribeSubscriptionFiltersRequest;
import software.amazon.awssdk.services.cloudwatchlogs.model.DescribeSubscriptionFiltersResponse;
import software.amazon.awssdk.services.cloudwatchlogs.model.InvalidParameterException;
import software.amazon.awssdk.services.cloudwatchlogs.model.ResourceNotFoundException;
import software.amazon.awssdk.services.cloudwatchlogs.model.ServiceUnavailableException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.exceptions.CfnServiceInternalErrorException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class ReadHandler extends BaseHandlerStd {
    private Logger logger;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final ProxyClient<CloudWatchLogsClient> proxyClient,
        final Logger logger) {

        this.logger = logger;

        final ResourceModel model = request.getDesiredResourceState();

        return proxy.initiate("AWS-Logs-SubscriptionFilter::Read", proxyClient, model, callbackContext)

            .translateToServiceRequest(Translator::translateToReadRequest)

            .makeServiceCall((awsRequest, sdkProxyClient) -> readResource(awsRequest, sdkProxyClient , model))

            .done(this::constructResourceModelFromResponse);
    }

    /**
     * Implement client invocation of the read request through the proxyClient, which is already initialised with
     * caller credentials, correct region and retry settings
     * @param awsRequest the aws service request to describe a resource
     * @param proxyClient the aws service client to make the call
     * @return describe resource response
     */
    private AwsResponse readResource(
        final DescribeSubscriptionFiltersRequest awsRequest,
        final ProxyClient<CloudWatchLogsClient> proxyClient,
        final ResourceModel model) {
        DescribeSubscriptionFiltersResponse awsResponse = null;
        try {

            awsResponse = proxyClient.injectCredentialsAndInvokeV2(awsRequest, proxyClient.client()::describeSubscriptionFilters);

        } catch (InvalidParameterException e) {
            throw new CfnInvalidRequestException(e);
        } catch (ResourceNotFoundException e) {
            throw new CfnNotFoundException(e);
        } catch (ServiceUnavailableException e) {
            throw new CfnServiceInternalErrorException(e);
        } catch (Exception ex) {
            logger.log("unhandled exception " + ex.getMessage());
            throw ex;
        }

        if (awsResponse.subscriptionFilters().isEmpty()) {
            logger.log("Resource does not exist.");
            throw new CfnNotFoundException(ResourceModel.TYPE_NAME,
                Objects.toString(model.getPrimaryIdentifier()));
        }

        logger.log(String.format("%s has successfully been read.", ResourceModel.TYPE_NAME));
        return awsResponse;
    }

    /**
     * Implement client invocation of the read request through the proxyClient, which is already initialised with
     * caller credentials, correct region and retry settings
     * @param awsResponse the aws service describe resource response
     * @return progressEvent indicating success, in progress with delay callback or failed state
     */
    private ProgressEvent<ResourceModel, CallbackContext> constructResourceModelFromResponse(
        final AwsResponse awsResponse) {
        return ProgressEvent.<ResourceModel, CallbackContext>builder()
            .status(OperationStatus.SUCCESS)
            .resourceModel(Translator.translateFromReadResponse(awsResponse))
            .build();
    }
}

package software.amazon.logs.subscriptionfilter;

import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsClient;

import java.util.Objects;
import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.awscore.AwsResponse;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.cloudwatchlogs.model.DescribeSubscriptionFiltersResponse;
import software.amazon.awssdk.services.cloudwatchlogs.model.InvalidParameterException;
import software.amazon.awssdk.services.cloudwatchlogs.model.LimitExceededException;
import software.amazon.awssdk.services.cloudwatchlogs.model.OperationAbortedException;
import software.amazon.awssdk.services.cloudwatchlogs.model.PutSubscriptionFilterRequest;
import software.amazon.awssdk.services.cloudwatchlogs.model.PutSubscriptionFilterResponse;
import software.amazon.awssdk.services.cloudwatchlogs.model.ServiceUnavailableException;
import software.amazon.cloudformation.exceptions.CfnAlreadyExistsException;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.exceptions.CfnResourceConflictException;
import software.amazon.cloudformation.exceptions.CfnServiceInternalErrorException;
import software.amazon.cloudformation.exceptions.CfnServiceLimitExceededException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class CreateHandler extends BaseHandlerStd {
    private Logger logger;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final ProxyClient<CloudWatchLogsClient> proxyClient,
        final Logger logger) {

        this.logger = logger;

        final ResourceModel model = request.getDesiredResourceState();

        return ProgressEvent.progress(model, callbackContext)

            .then(progress -> {
                    logger.log("Checking pre-existence of the resource...");
                    return preExistenceCheck(proxy, proxyClient, progress, logger)
                        .done((response) -> checkBeforeCreate(response, model, callbackContext));
                }
            )

            .then(progress -> {
                logger.log("creating a proxy chain for service calls...");
                return proxy.initiate("AWS-Logs-SubscriptionFilter::Create", proxyClient, model, callbackContext)

                    .translateToServiceRequest(Translator::translateToCreateRequest)

                    .makeServiceCall(this::createResource)

                    .progress();
                }
            )

            .then(progress -> {
                logger.log("Calling read handler to confirm the resource has been created...");
                return new ReadHandler().handleRequest(proxy, request, callbackContext, proxyClient, logger);
            });
    }

    private ProgressEvent<ResourceModel, CallbackContext> checkBeforeCreate(
        final DescribeSubscriptionFiltersResponse describeSubscriptionFiltersResponse,
        final ResourceModel model,
        final CallbackContext callbackContext
        ) {

        if (describeSubscriptionFiltersResponse.subscriptionFilters().isEmpty()) {
            logger.log("Empty result set.");
            return ProgressEvent.progress(model, callbackContext);
        }
        logger.log("Resource already exists.");
        return ProgressEvent.defaultFailureHandler(new CfnAlreadyExistsException(null), HandlerErrorCode.AlreadyExists);
    }

    /**
     * Implement client invocation of the create request through the proxyClient, which is already initialised with
     * caller credentials, correct region and retry settings
     * @param awsRequest the aws service request to create a resource
     * @param proxyClient the aws service client to make the call
     * @return awsResponse create resource response
     */
    private PutSubscriptionFilterResponse createResource(
        final PutSubscriptionFilterRequest awsRequest,
        final ProxyClient<CloudWatchLogsClient> proxyClient) {
        PutSubscriptionFilterResponse awsResponse;
        try {
            awsResponse = proxyClient.injectCredentialsAndInvokeV2(awsRequest, proxyClient.client()::putSubscriptionFilter);
        } catch (final InvalidParameterException e) {
            throw new CfnInvalidRequestException(ResourceModel.TYPE_NAME, e);
        } catch (final LimitExceededException e) {
            throw new CfnServiceLimitExceededException(e);
        } catch (final OperationAbortedException e) {
            throw new CfnResourceConflictException(e);
        } catch (final ServiceUnavailableException e) {
            throw new CfnServiceInternalErrorException(e);
        }

        logger.log(String.format("%s successfully created.", ResourceModel.TYPE_NAME));
        return awsResponse;
    }

}

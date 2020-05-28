package software.amazon.logs.subscriptionfilter;

import com.amazonaws.util.StringUtils;
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
import software.amazon.awssdk.services.cloudwatchlogs.model.ResourceNotFoundException;
import software.amazon.awssdk.services.cloudwatchlogs.model.ServiceUnavailableException;
import software.amazon.cloudformation.exceptions.CfnAlreadyExistsException;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnInternalFailureException;
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
import software.amazon.cloudformation.resource.IdentifierUtils;

public class CreateHandler extends BaseHandlerStd {
    private static final int MAX_LENGTH_METRIC_FILTER_NAME = 512;

    private Logger logger;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final ProxyClient<CloudWatchLogsClient> proxyClient,
        final Logger logger) {

        this.logger = logger;

        final ResourceModel model = request.getDesiredResourceState();
        // resource can auto-generate a name if not supplied by caller.
        // this logic should move up into the CloudFormation engine, but
        // currently exists here for backwards-compatibility with existing models
        if (StringUtils.isNullOrEmpty(model.getFilterName())) {
            model.setFilterName(
                IdentifierUtils.generateResourceIdentifier(
                    request.getLogicalResourceIdentifier(),
                    request.getClientRequestToken(),
                    MAX_LENGTH_METRIC_FILTER_NAME
                )
            );
        }

        logger.log("ID logs: log group name = " + model.getLogGroupName());
        logger.log("ID logs: filter name = " + model.getFilterName());

        return ProgressEvent.progress(model, callbackContext)

            .then(progress -> checkForPreCreateResourceExistence(proxy, request, proxyClient, progress))

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

    /**
     * If your service API is not idempotent, meaning it does not distinguish duplicate create requests against some identifier (e.g; resource Name)
     * and instead returns a 200 even though a resource already exists, you must first check if the resource exists here
     * NOTE: If your service API throws 'ResourceAlreadyExistsException' for create requests this method is not necessary
     * @param proxy Amazon webservice proxy to inject credentials correctly.
     * @param request incoming resource handler request
     * @param progressEvent event of the previous state indicating success, in progress with delay callback or failed state
     * @return progressEvent indicating success, in progress with delay callback or failed state
     */
    private ProgressEvent<ResourceModel, CallbackContext> checkForPreCreateResourceExistence(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final ProxyClient<CloudWatchLogsClient> proxyClient,
        final ProgressEvent<ResourceModel, CallbackContext> progressEvent) {
        final ResourceModel model = progressEvent.getResourceModel();
        final CallbackContext callbackContext = progressEvent.getCallbackContext();
        try {
            new ReadHandler().handleRequest(proxy, request, callbackContext, proxyClient, logger);
            logger.log("resource already exist. throwing exception...");
            return ProgressEvent.defaultFailureHandler(new CfnAlreadyExistsException(null), HandlerErrorCode.AlreadyExists);
        } catch (CfnNotFoundException e) {
            logger.log(model.getPrimaryIdentifier() + " does not exist; creating the resource.");
            return ProgressEvent.progress(model, callbackContext);
        } catch (InvalidParameterException e) {
            return ProgressEvent.defaultFailureHandler(new CfnInvalidRequestException(e), HandlerErrorCode.InvalidRequest);
        } catch (ServiceUnavailableException e) {
            return ProgressEvent.defaultFailureHandler(new CfnServiceInternalErrorException(e), HandlerErrorCode.ServiceInternalError);
        } catch (Exception ex) {
            logger.log("unhandled exception " + ex.getMessage());
            return ProgressEvent.defaultFailureHandler(new CfnInternalFailureException(ex), HandlerErrorCode.ServiceInternalError);
        }
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

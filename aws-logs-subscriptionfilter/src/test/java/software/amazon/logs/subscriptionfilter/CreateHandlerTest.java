package software.amazon.logs.subscriptionfilter;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import org.mockito.ArgumentMatchers;
import software.amazon.awssdk.core.SdkClient;
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsClient;
import software.amazon.awssdk.services.cloudwatchlogs.model.DescribeSubscriptionFiltersRequest;
import software.amazon.awssdk.services.cloudwatchlogs.model.DescribeSubscriptionFiltersResponse;
import software.amazon.awssdk.services.cloudwatchlogs.model.OperationAbortedException;
import software.amazon.awssdk.services.cloudwatchlogs.model.PutSubscriptionFilterRequest;
import software.amazon.awssdk.services.cloudwatchlogs.model.PutSubscriptionFilterResponse;
import software.amazon.awssdk.services.cloudwatchlogs.model.ResourceNotFoundException;
import software.amazon.awssdk.services.cloudwatchlogs.model.ServiceUnavailableException;
import software.amazon.cloudformation.exceptions.CfnResourceConflictException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CreateHandlerTest extends AbstractTestBase {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private ProxyClient<CloudWatchLogsClient> proxyClient;

    @Mock
    CloudWatchLogsClient sdkClient;

    @BeforeEach
    public void setup() {
        proxy = new AmazonWebServicesClientProxy(logger, MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());
        sdkClient = mock(CloudWatchLogsClient.class);
        proxyClient = MOCK_PROXY(proxy, sdkClient);
    }

    @Test
    public void handleRequest_SimpleSuccess() {
        final CreateHandler handler = new CreateHandler();

        final ResourceModel model = buildDefaultModel();

        final DescribeSubscriptionFiltersResponse describeResponse = DescribeSubscriptionFiltersResponse.builder()
            .subscriptionFilters(Translator.translateToSDK(model))
            .build();

        final PutSubscriptionFilterResponse createResponse = PutSubscriptionFilterResponse.builder().build();

        when(proxyClient.client().describeSubscriptionFilters(ArgumentMatchers.any(DescribeSubscriptionFiltersRequest.class)))
            .thenThrow(ResourceNotFoundException.class)
            .thenReturn(describeResponse);

        when(proxyClient.client().putSubscriptionFilter(any(PutSubscriptionFilterRequest.class)))
            .thenReturn(createResponse);

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
            .desiredResourceState(model)
            .logicalResourceIdentifier("logicalResourceIdentifier")
            .clientRequestToken("requestToken")
            .build();

        CallbackContext callbackContext = new CallbackContext();
        ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, callbackContext, proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

    }

    @Test
    public void handleRequest_Success2() {
        final CreateHandler handler = new CreateHandler();
        final ResourceModel model = buildDefaultModel();

        final DescribeSubscriptionFiltersResponse preCreateResponse = DescribeSubscriptionFiltersResponse.builder()
            .subscriptionFilters(Collections.emptyList())
            .build();

        final DescribeSubscriptionFiltersResponse postCreateResponse = DescribeSubscriptionFiltersResponse.builder()
            .subscriptionFilters(Translator.translateToSDK(model))
            .build();

        final PutSubscriptionFilterResponse createResponse = PutSubscriptionFilterResponse.builder()
            .build();

        // return no existing metrics for pre-create and then success response for create
        when(proxyClient.client().describeSubscriptionFilters(any(DescribeSubscriptionFiltersRequest.class)))
            .thenReturn(preCreateResponse)
            .thenReturn(postCreateResponse);

        when(proxyClient.client().putSubscriptionFilter(any(PutSubscriptionFilterRequest.class)))
            .thenReturn(createResponse);

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
            .desiredResourceState(model)
            .build();

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
        verify(proxyClient.client(), times(2)).describeSubscriptionFilters(any(DescribeSubscriptionFiltersRequest.class));
        verify(proxyClient.client(), times(1)).putSubscriptionFilter(any(PutSubscriptionFilterRequest.class));
    }

    @Test
    public void handleRequest_FailedCreate_InternalReadThrowsException() {
        final CreateHandler handler = new CreateHandler();
        final ResourceModel model = buildDefaultModel();

        // throw arbitrary error which should propagate to be handled by wrapper
        when(proxyClient.client().describeSubscriptionFilters(any(DescribeSubscriptionFiltersRequest.class)))
            .thenThrow(ServiceUnavailableException.class);

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
            .desiredResourceState(model)
            .build();

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.ServiceInternalError);
    }

    @Test
    public void handleRequest_FailedCreate_AlreadyExists() {
        final CreateHandler handler = new CreateHandler();
        final ResourceModel model = buildDefaultModel();

        final DescribeSubscriptionFiltersResponse describeResponse = DescribeSubscriptionFiltersResponse.builder()
            .subscriptionFilters(Translator.translateToSDK(model))
            .build();

        when(proxyClient.client().describeSubscriptionFilters(any(DescribeSubscriptionFiltersRequest.class)))
            .thenReturn(describeResponse);

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
            .desiredResourceState(model)
            .build();

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.AlreadyExists);
    }

    @Test
    public void handleRequest_FailedCreate_PutFailed() {
        final CreateHandler handler = new CreateHandler();
        final ResourceModel model = buildDefaultModel();

        final DescribeSubscriptionFiltersResponse describeResponse = DescribeSubscriptionFiltersResponse.builder()
            .subscriptionFilters(Translator.translateToSDK(model))
            .build();

        // return no existing metrics for pre-create and then success response for create
        when(proxyClient.client().describeSubscriptionFilters(any(DescribeSubscriptionFiltersRequest.class)))
            .thenThrow(ResourceNotFoundException.class)
            .thenReturn(describeResponse);

        when(proxyClient.client().putSubscriptionFilter(any(PutSubscriptionFilterRequest.class)))
            .thenThrow(OperationAbortedException.class);

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
            .desiredResourceState(model)
            .build();

        assertThatThrownBy(() -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger))
            .isInstanceOf(CfnResourceConflictException.class);
    }

    @Test
    public void handleRequest_Success_WithGeneratedName() {
        final CreateHandler handler = new CreateHandler();
        // no filter name supplied; should be generated
        final ResourceModel model = ResourceModel.builder()
            .logGroupName("log-group-name")
            .filterPattern("[pattern]")
            .roleArn("someRoleArn")
            .destinationArn("destinationArn")
            .distribution("ByLogStream")
            .build();

        // return no existing metrics for pre-create and then success response for create
        when(proxyClient.client().describeSubscriptionFilters(any(DescribeSubscriptionFiltersRequest.class)))
            .thenThrow(ResourceNotFoundException.class)
            .thenReturn(DescribeSubscriptionFiltersResponse.builder()
                .subscriptionFilters(Translator.translateToSDK(model))
                .build());

        when(proxyClient.client().putSubscriptionFilter(any(PutSubscriptionFilterRequest.class)))
            .thenReturn(PutSubscriptionFilterResponse.builder().build());

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
            .desiredResourceState(model)
            .logicalResourceIdentifier("logicalResourceIdentifier")
            .clientRequestToken("requestToken")
            .build();

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isNotNull();
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }
}

package software.amazon.logs.subscriptionfilter;

import java.time.Duration;

import java.util.Collections;
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsClient;
import software.amazon.awssdk.services.cloudwatchlogs.model.DescribeSubscriptionFiltersRequest;
import software.amazon.awssdk.services.cloudwatchlogs.model.DescribeSubscriptionFiltersResponse;
import software.amazon.awssdk.services.cloudwatchlogs.model.InvalidParameterException;
import software.amazon.awssdk.services.cloudwatchlogs.model.PutSubscriptionFilterRequest;
import software.amazon.awssdk.services.cloudwatchlogs.model.PutSubscriptionFilterResponse;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
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
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UpdateHandlerTest extends AbstractTestBase {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private ProxyClient<CloudWatchLogsClient> proxyClient;

    @Mock
    CloudWatchLogsClient cloudWatchLogsClient;

    @BeforeEach
    public void setup() {
        proxy = new AmazonWebServicesClientProxy(logger, MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());
        cloudWatchLogsClient = mock(CloudWatchLogsClient.class);
        proxyClient = MOCK_PROXY(proxy, cloudWatchLogsClient);
    }

    @Test
    public void handleRequest_SimpleSuccess() {
        final UpdateHandler handler = new UpdateHandler();

        final ResourceModel model = buildDefaultModel();

        final PutSubscriptionFilterResponse updateResponse = PutSubscriptionFilterResponse.builder()
            .build();

        final DescribeSubscriptionFiltersResponse describeResponse = DescribeSubscriptionFiltersResponse.builder()
            .subscriptionFilters(Translator.translateToSDK(model))
            .build();

        when(proxyClient.client().putSubscriptionFilter(any(PutSubscriptionFilterRequest.class)))
            .thenReturn(updateResponse);
        when(proxyClient.client().describeSubscriptionFilters(any(DescribeSubscriptionFiltersRequest.class)))
            .thenReturn(describeResponse);

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
    }

    @Test
    public void handleRequest_FilterNameDoesNotMatch_NotUpdatable() {
        final UpdateHandler handler = new UpdateHandler();
        final ResourceModel model = buildDefaultModel();
        final ResourceModel previousModel = buildDefaultModel();
        previousModel.setFilterName(previousModel.getFilterName() + "a");

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
            .desiredResourceState(model)
            .previousResourceState(previousModel)
            .build();

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.NotUpdatable);
    }

    @Test
    public void handleRequest_LogGroupNameDoesNotMatch_NotUpdatable() {
        final UpdateHandler handler = new UpdateHandler();
        final ResourceModel model = buildDefaultModel();
        final ResourceModel previousModel = buildDefaultModel();
        previousModel.setLogGroupName(previousModel.getLogGroupName() + "a");

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
            .desiredResourceState(model)
            .previousResourceState(previousModel)
            .build();

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.NotUpdatable);
    }

    @Test
    public void handleRequest_ResourceNotFound() {
        final UpdateHandler handler = new UpdateHandler();
        final ResourceModel model = buildDefaultModel();

        final DescribeSubscriptionFiltersResponse describeResponse = DescribeSubscriptionFiltersResponse.builder()
            .subscriptionFilters(Collections.emptyList())
            .build();

        when(proxyClient.client().describeSubscriptionFilters(any(DescribeSubscriptionFiltersRequest.class)))
            .thenReturn(describeResponse);

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
            .desiredResourceState(model)
            .build();

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.NotFound);
    }

    @Test
    public void handleRequest_InternalException() {
        final UpdateHandler handler = new UpdateHandler();
        final ResourceModel model = buildDefaultModel();

        final DescribeSubscriptionFiltersResponse describeResponse = DescribeSubscriptionFiltersResponse.builder()
            .subscriptionFilters(Translator.translateToSDK(model))
            .build();

        when(proxyClient.client().describeSubscriptionFilters(any(DescribeSubscriptionFiltersRequest.class)))
            .thenReturn(describeResponse);

        when(proxyClient.client().putSubscriptionFilter(any(PutSubscriptionFilterRequest.class)))
            .thenThrow(InvalidParameterException.class);

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
            .desiredResourceState(model)
            .build();

        assertThatThrownBy(() -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger))
            .isInstanceOf(CfnInvalidRequestException.class);
        verify(proxyClient.client(), times(1)).describeSubscriptionFilters(any(DescribeSubscriptionFiltersRequest.class));
        verify(proxyClient.client(), times(1)).putSubscriptionFilter(any(PutSubscriptionFilterRequest.class));
        verify(cloudWatchLogsClient, atLeastOnce()).serviceName();
        verifyNoMoreInteractions(cloudWatchLogsClient);
    }
}

package br.com.on.fiap.hackathonprocessadorvideo.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.net.URI;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.SnsClientBuilder;

@DisplayName("AwsSnsConfig - configuração do bean SnsClient")
class AwsSnsConfigTest {

    @Test
    @DisplayName("Dado properties válidas quando snsClient então builder é configurado corretamente")
    void givenProperties_whenSnsClient_thenBuilderConfiguredProperly() {
        AwsSnsConfig config = new AwsSnsConfig();
        ReflectionTestUtils.setField(config, "region", "us-east-1");
        ReflectionTestUtils.setField(config, "accessKey", "test-ak");
        ReflectionTestUtils.setField(config, "secretKey", "test-sk");
        ReflectionTestUtils.setField(config, "endpoint", "http://localhost:4566");

        SnsClientBuilder builder = mock(SnsClientBuilder.class, RETURNS_SELF);
        SnsClient clientMock = mock(SnsClient.class);

        try (MockedStatic<SnsClient> snsStatic = mockStatic(SnsClient.class);
                MockedStatic<AwsBasicCredentials> credsStatic = mockStatic(AwsBasicCredentials.class);
                MockedStatic<StaticCredentialsProvider> providerStatic = mockStatic(StaticCredentialsProvider.class)) {

            snsStatic.when(SnsClient::builder).thenReturn(builder);
            when(builder.build()).thenReturn(clientMock);

            AwsBasicCredentials credsMock = mock(AwsBasicCredentials.class);
            credsStatic
                    .when(() -> AwsBasicCredentials.create("test-ak", "test-sk"))
                    .thenReturn(credsMock);

            StaticCredentialsProvider providerMock = mock(StaticCredentialsProvider.class);
            providerStatic
                    .when(() -> StaticCredentialsProvider.create(credsMock))
                    .thenReturn(providerMock);

            SnsClient result = config.snsClient();

            assertThat(result).isSameAs(clientMock);

            ArgumentCaptor<Region> regionCaptor = ArgumentCaptor.forClass(Region.class);
            verify(builder).region(regionCaptor.capture());
            assertThat(regionCaptor.getValue().id()).isEqualTo("us-east-1");

            ArgumentCaptor<StaticCredentialsProvider> providerCaptor =
                    ArgumentCaptor.forClass(StaticCredentialsProvider.class);
            verify(builder).credentialsProvider(providerCaptor.capture());
            assertThat(providerCaptor.getValue()).isSameAs(providerMock);

            ArgumentCaptor<URI> uriCaptor = ArgumentCaptor.forClass(URI.class);
            verify(builder).endpointOverride(uriCaptor.capture());
            assertThat(uriCaptor.getValue()).hasToString("http://localhost:4566");

            verify(builder).build();
            verifyNoMoreInteractions(builder);
        }
    }
}

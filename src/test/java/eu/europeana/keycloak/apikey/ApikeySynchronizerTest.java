package eu.europeana.keycloak.apikey;

import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.exceptions.misusing.InvalidUseOfMatchersException;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;

import static org.junit.Assert.fail;

@RunWith(SpringRunner.class)
public class ApikeySynchronizerTest {

    private static final String CLIENT_ID = "clientId";

    private static final String KEYCLOAK_CLIENT_IDENTIFIER = "keycloakId";

    @Mock
    private CloseableHttpClient httpClient;

    @InjectMocks
    private ApikeySynchronizer synchronizer = new ApikeySynchronizer();

    @Test
    public void synchronizeClientWhenDisabled() throws Exception {
        prepareForSynchronize(false);

        synchronizer.synchronizeClient(CLIENT_ID, KEYCLOAK_CLIENT_IDENTIFIER, false);
    }

    @Test
    public void synchronizeClientWhenEnabled() throws Exception {
        prepareForSynchronize(false);

        synchronizer.synchronizeClient(CLIENT_ID, KEYCLOAK_CLIENT_IDENTIFIER, true);
    }

    @Test(expected = ApikeyNotFoundException.class)
    public void synchronizeClientWhenNotFound() throws IOException, ApikeyNotFoundException {
        prepareForSynchronize(true);

        synchronizer.synchronizeClient(CLIENT_ID, KEYCLOAK_CLIENT_IDENTIFIER, true);
        fail();
    }

    private void prepareForSynchronize(boolean notFound) throws IOException {
        CloseableHttpResponse response = Mockito.mock(CloseableHttpResponse.class);
        StatusLine statusLine = Mockito.mock(StatusLine.class);
        Mockito.when(response.getStatusLine()).thenReturn(statusLine);
        Mockito.when(statusLine.getStatusCode()).thenReturn(notFound ? 404 : 204);

        CloseableHttpResponse reenableResponse = Mockito.mock(CloseableHttpResponse.class);
        StatusLine reenableStatusLine = Mockito.mock(StatusLine.class);
        Mockito.when(reenableResponse.getStatusLine()).thenReturn(reenableStatusLine);
        Mockito.when(reenableStatusLine.getStatusCode()).thenReturn(200);

        CloseableHttpResponse deleteResponse = Mockito.mock(CloseableHttpResponse.class);
        StatusLine deleteStatusLine = Mockito.mock(StatusLine.class);
        Mockito.when(deleteResponse.getStatusLine()).thenReturn(deleteStatusLine);
        Mockito.when(deleteStatusLine.getStatusCode()).thenReturn(204);

        Mockito.when(httpClient.execute(Mockito.anyObject())).thenAnswer(
                invocation -> {
                    Object argument = invocation.getArguments()[0];
                    if (argument instanceof HttpPost) {
                        return response;
                    }
                    throw new InvalidUseOfMatchersException(
                            String.format("Argument %s does not match", argument)
                    );
                }).thenAnswer(
                invocation -> {
                    Object argument = invocation.getArguments()[0];
                    if (argument instanceof HttpPost) {
                        return reenableResponse;
                    } else if (argument instanceof HttpDelete) {
                        return deleteResponse;
                    }
                    throw new InvalidUseOfMatchersException(
                            String.format("Argument %s does not match", argument)
                    );
                });
    }

    @Test
    public void deleteClient() throws IOException {
        prepareForDelete();

        synchronizer.deleteClient(KEYCLOAK_CLIENT_IDENTIFIER);
    }

    private void prepareForDelete() throws IOException {
        CloseableHttpResponse response = Mockito.mock(CloseableHttpResponse.class);
        StatusLine statusLine = Mockito.mock(StatusLine.class);
        Mockito.when(response.getStatusLine()).thenReturn(statusLine);
        Mockito.when(statusLine.getStatusCode()).thenReturn(204);

        Mockito.when(httpClient.execute(Mockito.anyObject())).thenAnswer(
                invocation -> {
                    Object argument = invocation.getArguments()[0];
                    if (argument instanceof HttpDelete) {
                        return response;
                    }
                    throw new InvalidUseOfMatchersException(
                            String.format("Argument %s does not match", argument)
                    );
                });
    }

    @Test
    public void updateAccessDate() throws IOException, ApikeyNotFoundException {
        prepareForUpdate(false);

        synchronizer.updateAccessDate(CLIENT_ID);
    }

    @Test(expected = ApikeyNotFoundException.class)
    public void updateAccessDateWhenNotFound() throws IOException, ApikeyNotFoundException {
        prepareForUpdate(true);

        synchronizer.updateAccessDate(CLIENT_ID);
        fail();
    }

    private void prepareForUpdate(boolean notFound) throws IOException {
        CloseableHttpResponse response = Mockito.mock(CloseableHttpResponse.class);
        StatusLine statusLine = Mockito.mock(StatusLine.class);
        Mockito.when(response.getStatusLine()).thenReturn(statusLine);
        Mockito.when(statusLine.getStatusCode()).thenReturn(notFound ? 404 : 204);

        Mockito.when(httpClient.execute(Mockito.anyObject())).thenAnswer(
                invocation -> {
                    Object argument = invocation.getArguments()[0];
                    if (argument instanceof HttpPost) {
                        return response;
                    }
                    throw new InvalidUseOfMatchersException(
                            String.format("Argument %s does not match", argument)
                    );
                });
    }
}
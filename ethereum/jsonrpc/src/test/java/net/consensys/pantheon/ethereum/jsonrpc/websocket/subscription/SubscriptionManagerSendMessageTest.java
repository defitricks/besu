package net.consensys.pantheon.ethereum.jsonrpc.websocket.subscription;

import static junit.framework.TestCase.fail;
import static org.mockito.Mockito.mock;

import net.consensys.pantheon.ethereum.jsonrpc.internal.results.JsonRpcResult;
import net.consensys.pantheon.ethereum.jsonrpc.websocket.subscription.request.SubscribeRequest;
import net.consensys.pantheon.ethereum.jsonrpc.websocket.subscription.request.SubscriptionType;
import net.consensys.pantheon.ethereum.jsonrpc.websocket.subscription.response.SubscriptionResponse;

import java.util.UUID;

import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
public class SubscriptionManagerSendMessageTest {

  private static final int VERTX_AWAIT_TIMEOUT_MILLIS = 10000;

  private Vertx vertx;
  private SubscriptionManager subscriptionManager;

  @Before
  public void before(final TestContext context) {
    vertx = Vertx.vertx();
    subscriptionManager = new SubscriptionManager();
    vertx.deployVerticle(subscriptionManager, context.asyncAssertSuccess());
  }

  @Test
  public void shouldSendMessageOnTheConnectionIdEventBusAddressForExistingSubscription(
      final TestContext context) {
    final String connectionId = UUID.randomUUID().toString();
    final SubscribeRequest subscribeRequest =
        new SubscribeRequest(SubscriptionType.SYNCING, null, null, connectionId);

    final JsonRpcResult expectedResult = mock(JsonRpcResult.class);
    final SubscriptionResponse expectedResponse = new SubscriptionResponse(1L, expectedResult);

    final Long subscriptionId = subscriptionManager.subscribe(subscribeRequest);

    final Async async = context.async();

    vertx
        .eventBus()
        .consumer(connectionId)
        .handler(
            msg -> {
              context.assertEquals(Json.encode(expectedResponse), msg.body());
              async.complete();
            })
        .completionHandler(v -> subscriptionManager.sendMessage(subscriptionId, expectedResult));

    async.awaitSuccess(VERTX_AWAIT_TIMEOUT_MILLIS);
  }

  @Test
  public void shouldNotSendMessageOnTheConnectionIdEventBusAddressForAbsentSubscription(
      final TestContext context) {
    final String connectionId = UUID.randomUUID().toString();

    final Async async = context.async();

    vertx
        .eventBus()
        .consumer(connectionId)
        .handler(
            msg -> {
              fail("Shouldn't receive message");
              async.complete();
            })
        .completionHandler(v -> subscriptionManager.sendMessage(1L, mock(JsonRpcResult.class)));

    // if it doesn't receive the message in 5 seconds we assume it won't receive anymore
    vertx.setPeriodic(5000, v -> async.complete());

    async.awaitSuccess(VERTX_AWAIT_TIMEOUT_MILLIS);
  }
}

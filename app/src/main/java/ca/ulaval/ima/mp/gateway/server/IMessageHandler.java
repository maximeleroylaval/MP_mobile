package ca.ulaval.ima.mp.gateway.server;

import ca.ulaval.ima.mp.sdk.models.Message;

public interface IMessageHandler {
    void onMessageReceived(Message message);
}

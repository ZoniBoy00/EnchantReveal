package fi.zoniboy.enchantreveal.client;

import fi.zoniboy.enchantreveal.wire.Protocol;

public interface PreviewReceiver {
    void enchantreveal$receive(Protocol.Preview preview);
}

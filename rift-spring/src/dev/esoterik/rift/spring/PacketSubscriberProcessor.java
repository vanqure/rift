package dev.esoterik.rift.spring;

import dev.esoterik.rift.RiftClient;
import dev.esoterik.rift.packet.PacketSubscriber;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

@Component
public final class PacketSubscriberProcessor implements BeanPostProcessor {

  private final RiftClient<?, ?> riftClient;

  @Autowired
  public PacketSubscriberProcessor(final RiftClient<?, ?> riftClient) {
    this.riftClient = riftClient;
  }

  @Override
  public Object postProcessAfterInitialization(
      final @NotNull Object bean, final @NotNull String beanName) throws BeansException {
    if (bean instanceof final PacketSubscriber packetSubscriber)
      riftClient.subscribe(packetSubscriber);

    return bean;
  }
}

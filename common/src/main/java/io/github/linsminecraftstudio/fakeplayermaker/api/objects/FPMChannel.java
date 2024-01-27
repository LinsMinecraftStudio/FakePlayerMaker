package io.github.linsminecraftstudio.fakeplayermaker.api.objects;

import io.netty.channel.*;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.SocketUtils;
import org.jetbrains.annotations.NotNull;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class FPMChannel extends AbstractChannel {
    private final static EventLoop EVENT_LOOP = new DefaultEventLoop();
    private final DefaultChannelConfig config = new DefaultChannelConfig(this);
    private final ChannelMetadata metadata;
    private final Queue<Object> outbound;
    private final Queue<Object> inbound;
    private final FPMChannelPipeline pipeline;
    private State state;

    enum State {
        ACTIVE,
        CLOSED
    }

    public FPMChannel() {
        super(null);
        setup();
        state = State.ACTIVE;
        outbound = new ArrayDeque<>();
        inbound = new ArrayDeque<>();
        metadata = new ChannelMetadata(false);
        pipeline = new FPMChannelPipeline(this);
    }

    private void setup(final ChannelHandler... handlers) {
        ObjectUtil.checkNotNull(handlers, "handlers");
        ChannelPipeline p = this.pipeline();
        p.addLast(new ChannelInitializer<>() {
            protected void initChannel(Channel ch) {
                ChannelPipeline pipeline = ch.pipeline();
                for (ChannelHandler h : handlers) {
                    if (h == null) {
                        break;
                    }
                    pipeline.addLast(h);
                }
            }
        });
        ChannelFuture future = EVENT_LOOP.register(this);
        assert future.isDone();
    }

    @Override
    public ChannelConfig config() {
        config.setAutoRead(true);
        return config;
    }

    @Override
    protected void doBeginRead() {
    }

    @Override
    protected void doBind(@NotNull SocketAddress arg0) throws Exception {
        SocketUtils.bind(java.nio.channels.SocketChannel.open(), arg0);
    }

    @Override
    protected void doClose() throws ExecutionException, InterruptedException {
        this.state = State.CLOSED;
        ChannelFuture future = super.close(voidPromise());
        future.get();
    }

    @Override
    protected void doDisconnect() throws ExecutionException, InterruptedException {
        if (!metadata().hasDisconnect()) {
            this.doClose();
        }
    }

    protected boolean checkActive() {
        return state == State.ACTIVE;
    }

    @Override
    protected void doWrite(ChannelOutboundBuffer in) throws ExecutionException, InterruptedException {
        if (!checkActive()) return;

        CompletableFuture.runAsync(() -> {
            while (in.current() != null) {
                Object msg = in.current();
                ReferenceCountUtil.retain(msg);
                this.handleOutboundMessage(msg);
                in.remove();
            }
        }).get();
    }

    protected void handleOutboundMessage(Object msg) {
        this.outbound.add(msg);
    }

    protected void handleInboundMessage(Object msg) {
        this.inbound.add(msg);
    }

    @Override
    public boolean isActive() {
        return checkActive();
    }

    @Override
    protected boolean isCompatible(EventLoop arg0) {
        return true;
    }

    @Override
    public boolean isOpen() {
        return true;
    }

    @Override
    protected SocketAddress localAddress0() {
        return new InetSocketAddress(60000);
    }

    @Override
    public ChannelMetadata metadata() {
        return this.metadata;
    }

    @Override
    protected AbstractUnsafe newUnsafe() {
        return new AbstractUnsafe() {
            @Override
            public void connect(SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise) {
                safeSetSuccess(promise);
            }
        };
    }

    @Override
    protected SocketAddress remoteAddress0() {
        return new InetSocketAddress(60000);
    }

    @Override
    public ChannelPipeline pipeline() {
        return this.pipeline;
    }

    @Override
    public EventLoop eventLoop() {
        return EVENT_LOOP;
    }

    private final class FPMChannelPipeline extends DefaultChannelPipeline {
        FPMChannelPipeline(FPMChannel channel) {
            super(channel);
        }

        protected void onUnhandledInboundException(Throwable cause) {
        }

        protected void onUnhandledInboundMessage(ChannelHandlerContext ctx, Object msg) {
            FPMChannel.this.handleInboundMessage(msg);
        }
    }
}
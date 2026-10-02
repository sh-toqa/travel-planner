package dev.toqash.travelplannerbackend.support;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

// Stands in for Gemini: tests queue the replies the "model" will give, in order. No network, no cost, deterministic.
public class FakeChatModel implements ChatModel {
    private final BlockingQueue<Object> replies = new LinkedBlockingQueue<>();
    private final List<Prompt> prompts = new CopyOnWriteArrayList<>();
    private volatile CountDownLatch gate = new CountDownLatch(0);

    public void reply(String content) {
        replies.add(content);
    }

    public void fail(RuntimeException error) {
        replies.add(error);
    }

    // Makes the next calls wait until release() is called, to test what happens while a job is running.
    public void hold() {
        gate = new CountDownLatch(1);
    }

    public void release() {
        gate.countDown();
    }

    public List<Prompt> prompts() {
        return prompts;
    }

    public void reset() {
        replies.clear();
        prompts.clear();
        release();
    }

    @Override
    public ChatResponse call(Prompt prompt) {
        prompts.add(prompt);
        try {
            if (!gate.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("FakeChatModel was never released");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
        Object reply = replies.poll();
        if (reply == null) {
            throw new IllegalStateException("FakeChatModel has no reply queued");
        }
        if (reply instanceof RuntimeException error) {
            throw error;
        }
        return new ChatResponse(List.of(new Generation(new AssistantMessage((String) reply))));
    }
}
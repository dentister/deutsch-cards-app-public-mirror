package com.kniazev.cards.word.services;

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;

import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.springframework.stereotype.Service;

import com.kniazev.cards.word.ui.configuration.ChatMessage;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class ChatStorage  {
    private final Map<String, Queue<ChatMessage>> archive = new HashMap<>();
    
    public Queue<ChatMessage> getMessages(String username) {
        Queue<ChatMessage> list = archive.get(username);
        
        if (list == null) {
            list = new CircularFifoQueue<>(50);
            archive.put(username, list);
        }
        
        return list;
    }
    
    public void addMessages(String username, ChatMessage...msgs) {
        if (msgs == null) {
            return;
        }
        
        Queue<ChatMessage> messages = getMessages(username);
        
        for (ChatMessage m : msgs) {
            messages.offer(m);
        }
    }
}

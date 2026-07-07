// SPDX-FileCopyrightText: 2025 owlmaddie LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © owlmaddie LLC - unauthorized use prohibited
package com.owlmaddie.json;

import java.util.List;


public class ChatGPTResponse {
    public List<ChatGPTChoice> choices;
    public ChatGPTUsage usage;
    
    public static class ChatGPTChoice {
        public int index;
        public ChatGPTMessage message;
        public String finish_reason;
    }

    public static class ChatGPTMessage {
        public String role;
        public String content;
        public String refusal;
    }

    public static class ChatGPTUsage {
        public Integer prompt_tokens;
        public Integer completion_tokens;
        public Integer total_tokens;
    }
}

package com.billanalysis.agent;

import com.billanalysis.agent.tools.AnomalyTool;
import com.billanalysis.agent.tools.StatsTool;
import com.billanalysis.agent.tools.TrendTool;
import com.billanalysis.parser.BillRecord;
import com.billanalysis.prompt.PromptBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BillAnalysisAgent {

    private final ChatClient.Builder chatClientBuilder;
    private final StatsTool statsTool;
    private final TrendTool trendTool;
    private final AnomalyTool anomalyTool;
    private final BillSessionContext sessionContext;
    private final PromptBuilder promptBuilder;

    /**
     * Loads bills into the request-scoped context, then asks the LLM to answer
     * the user's question using whichever tools it deems appropriate.
     */
    public String analyze(String userQuestion, List<BillRecord> bills) {
        sessionContext.setBills(bills);

        return chatClientBuilder.build()
                .prompt()
                .system(promptBuilder.buildSystemPrompt(bills.size()))
                .user(userQuestion)
                .tools(statsTool, trendTool, anomalyTool)
                .call()
                .content();
    }
}

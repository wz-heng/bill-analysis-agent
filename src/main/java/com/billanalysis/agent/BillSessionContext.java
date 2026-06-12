package com.billanalysis.agent;

import com.billanalysis.parser.BillRecord;
import lombok.Data;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import java.util.ArrayList;
import java.util.List;

/**
 * Request-scoped holder for the parsed bill data.
 * Singleton tool beans access this via a CGLIB proxy so each HTTP request
 * gets its own isolated bill list and tool-invocation log.
 */
@Data
@Component
@RequestScope
public class BillSessionContext {
    private List<BillRecord> bills;
    private final List<String> toolsInvoked = new ArrayList<>();

    public void recordTool(String name) {
        toolsInvoked.add(name);
    }
}

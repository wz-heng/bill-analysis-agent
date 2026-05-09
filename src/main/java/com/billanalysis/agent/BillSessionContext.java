package com.billanalysis.agent;

import com.billanalysis.parser.BillRecord;
import lombok.Data;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import java.util.List;

/**
 * Request-scoped holder for the parsed bill data.
 * Singleton tool beans access this via a CGLIB proxy so each HTTP request
 * gets its own isolated bill list.
 */
@Data
@Component
@RequestScope
public class BillSessionContext {
    private List<BillRecord> bills;
}

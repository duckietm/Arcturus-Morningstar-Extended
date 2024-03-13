package com.eu.habbo.messages.outgoing.modtool;

import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

public class ModToolReportReceivedAlertComposer extends MessageComposer {
    public static final int REPORT_RECEIVED = 0;
    public static final int REPORT_WINDOW = 1;
    public static final int REPORT_ABUSIVE = 2;

    private final int errorCode;
    private final String message;

    public ModToolReportReceivedAlertComposer(int errorCode, String message) {
        this.errorCode = errorCode;
        this.message = message;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.ModToolReportReceivedAlertComposer);
        this.response.appendInt(this.errorCode);
        this.response.appendString(this.message);
        return this.response;
    }
}

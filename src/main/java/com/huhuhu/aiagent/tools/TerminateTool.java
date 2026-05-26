package com.huhuhu.aiagent.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

/**
 * 终止工具（作用是让自主规划智能体能够合理地中断）
 */
public class TerminateTool {

    @Tool(description = """
            Terminate the interaction when the request is met OR if the assistant cannot proceed further with the task.
            "When you have finished all the tasks, call this tool to end the work.
            """)
    public String doTerminate(@ToolParam(description = "Final summary or answer to provide to the user before terminating", required = false) String finalResponse) {
        if (finalResponse != null && !finalResponse.isEmpty()) {
            return "任务完成。最终回答：\n" + finalResponse;
        }
        return "任务结束";
    }
}

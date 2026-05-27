package com.huhuhu.aiagent.chatmemory;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import org.objenesis.strategy.StdInstantiatorStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 基于文件持久化的对话记忆
 * <p>支持相对路径（自动转为项目根目录下的绝对路径）和摘要文件分离存储
 */
@Slf4j
public class FileBasedChatMemory implements ChatMemory {

    private final Path baseDir;
    private static final String SUMMARY_SUFFIX = ".summary";
    private static final Kryo kryo = new Kryo();

    static {
        kryo.setRegistrationRequired(false);
        kryo.setInstantiatorStrategy(new StdInstantiatorStrategy());
    }

    /**
     * @param dir 文件存储目录（相对路径会转换为项目根目录下的绝对路径）
     */
    public FileBasedChatMemory(String dir) {
        Path path = Path.of(dir);
        if (path.isAbsolute()) {
            this.baseDir = path;
        } else {
            // 相对路径：从项目根目录（user.dir）解析
            this.baseDir = Path.of(System.getProperty("user.dir"), dir);
        }
        File baseDirFile = this.baseDir.toFile();
        if (!baseDirFile.exists()) {
            baseDirFile.mkdirs();
        }
    }

    /**
     * 获取存储根目录的绝对路径
     */
    public String getBaseDir() {
        return baseDir.toAbsolutePath().toString();
    }

    /**
     * 获取会话文件路径（调试用）
     */
    public File getConversationFilePath(String conversationId) {
        return getConversationFile(conversationId);
    }

    @Override
    public void add(String conversationId, List<Message> messages) {
        log.debug("FileBasedChatMemory.add() 被调用，conversationId={}，消息数={}", conversationId, messages.size());
        List<Message> conversationMessages = getOrCreateConversation(conversationId);
        conversationMessages.addAll(messages);
        saveConversation(conversationId, conversationMessages);
        log.debug("FileBasedChatMemory.add() 完成，当前会话共有 {} 条消息", conversationMessages.size());
    }

    @Override
    public List<Message> get(String conversationId) {
        return getOrCreateConversation(conversationId);
    }

    @Override
    public void clear(String conversationId) {
        deleteConversationFile(conversationId);
        deleteSummaryFile(conversationId);
    }

    /**
     * 只清除会话文件，保留摘要
     */
    public void clearConversation(String conversationId) {
        deleteConversationFile(conversationId);
    }

    /**
     * 保存摘要（独立于原始消息文件）
     */
    public void saveSummary(String conversationId, String summary) {
        File summaryFile = getSummaryFile(conversationId);
        try (Output output = new Output(new FileOutputStream(summaryFile))) {
            kryo.writeObject(output, summary);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取摘要
     */
    public String getSummary(String conversationId) {
        File summaryFile = getSummaryFile(conversationId);
        if (!summaryFile.exists()) {
            return null;
        }
        try (Input input = new Input(new FileInputStream(summaryFile))) {
            return kryo.readObject(input, String.class);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 清除摘要
     */
    public void clearSummary(String conversationId) {
        deleteSummaryFile(conversationId);
    }

    private List<Message> getOrCreateConversation(String conversationId) {
        File file = getConversationFile(conversationId);
        List<Message> messages = new ArrayList<>();
        if (file.exists()) {
            try (Input input = new Input(new FileInputStream(file))) {
                messages = kryo.readObject(input, ArrayList.class);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return messages;
    }

    private void saveConversation(String conversationId, List<Message> messages) {
        File file = getConversationFile(conversationId);
        log.debug("保存会话到文件: {}，消息数={}", file.getAbsolutePath(), messages.size());
        try (Output output = new Output(new FileOutputStream(file))) {
            kryo.writeObject(output, messages);
            log.debug("文件保存成功，大小={} bytes", file.length());
        } catch (IOException e) {
            log.error("保存会话失败: {}", e.getMessage());
            e.printStackTrace();
        }
    }

    private File getConversationFile(String conversationId) {
        return baseDir.resolve(conversationId + ".kryo").toFile();
    }

    private File getSummaryFile(String conversationId) {
        return baseDir.resolve(conversationId + SUMMARY_SUFFIX + ".kryo").toFile();
    }

    private void deleteConversationFile(String conversationId) {
        File file = getConversationFile(conversationId);
        if (file.exists()) {
            file.delete();
        }
    }

    private void deleteSummaryFile(String conversationId) {
        File file = getSummaryFile(conversationId);
        if (file.exists()) {
            file.delete();
        }
    }
}
package org.filepile.censorplugin;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CensorPlugin extends JavaPlugin implements Listener {

    private List<String> bannedWords;
    private Pattern bannedPattern;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadConfig();
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("CensorPlugin has been enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("CensorPlugin has been disabled!");
    }

    private void loadConfig() {
        reloadConfig();
        FileConfiguration config = getConfig();
        bannedWords = config.getStringList("banned-words");
        String patternString = createFlexiblePattern(bannedWords);
        bannedPattern = Pattern.compile(patternString, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    }

    private String createFlexiblePattern(List<String> words) {
        StringBuilder pattern = new StringBuilder();
        for (String word : words) {
            if (pattern.length() > 0) {
                pattern.append("|");
            }
            pattern.append("(");
            for (char c : word.toCharArray()) {
                pattern.append(c).append("[\\s\\W\\d]*");
            }
            pattern.append(")");
        }
        return pattern.toString();
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        String message = event.getMessage();
        String censoredMessage = censorMessage(message);
        event.setMessage(censoredMessage);
    }

    private String censorMessage(String message) {
        Matcher matcher = bannedPattern.matcher(message);
        StringBuffer sb = new StringBuffer();
        boolean censoredAny = false;

        while (matcher.find()) {
            String matchedWord = matcher.group();
            String censored = createCensoredReplacement(matchedWord);
            matcher.appendReplacement(sb, Matcher.quoteReplacement(censored));
            censoredAny = true;
        }
        matcher.appendTail(sb);

        if (!censoredAny) {
            // 유사도 검사, %바꿀수 있음
            for (String word : bannedWords) {
                String lowercaseMessage = sb.toString().toLowerCase();
                String lowercaseWord = word.toLowerCase();
                if (calculateSimilarity(lowercaseMessage, lowercaseWord) > 0.8) {
                    return censorEntireWord(message, word);
                }
            }
        }

        return sb.toString();
    }

    private String createCensoredReplacement(String word) {
        return "*".repeat(word.replaceAll("[\\s\\W\\d]", "*").length());
    }

    private String censorEntireWord(String message, String word) {
        String lowercaseMessage = message.toLowerCase();
        String lowercaseWord = word.toLowerCase();
        StringBuilder result = new StringBuilder(message);
        int startIndex = 0;

        while ((startIndex = lowercaseMessage.indexOf(lowercaseWord, startIndex)) != -1) {
            int endIndex = startIndex + word.length();
            for (int i = startIndex; i < endIndex; i++) {
                if (!Character.isWhitespace(message.charAt(i))) {
                    result.setCharAt(i, '*');
                }
            }
            startIndex = endIndex;
        }

        return result.toString();
    }

    private double calculateSimilarity(String s1, String s2) {
        int longer = Math.max(s1.length(), s2.length());
        int shorter = Math.min(s1.length(), s2.length());
        int matchCount = 0;
        for (int i = 0; i < shorter; i++) {
            if (s1.charAt(i) == s2.charAt(i)) {
                matchCount++;
            }
        }
        return (double) matchCount / longer;
    }
}
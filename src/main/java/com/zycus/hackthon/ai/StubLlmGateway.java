package com.zycus.hackthon.ai;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "llm.provider", havingValue = "stub", matchIfMissing = true)
public class StubLlmGateway implements LlmGateway {

    private static final Pattern AGENT_ID = Pattern.compile("AGT-[0-9]{3}");
    private static final Pattern AGENT_ENTRY = Pattern.compile(
            "\\{agentId=(AGT-[0-9]{3}),name=.*?,status=(AVAILABLE|BUSY),activeOrderCount=(\\d+)\\}");

    private record Candidate(String id, int activeOrderCount) {
    }

    @Override
    public String call(String prompt) {
        int rosterStart = prompt.indexOf("Available agents:");
        String roster = rosterStart >= 0 ? prompt.substring(rosterStart) : prompt;
        List<Candidate> candidates = new ArrayList<>();
        Matcher entries = AGENT_ENTRY.matcher(roster);
        while (entries.find()) {
            candidates.add(new Candidate(entries.group(1), Integer.parseInt(entries.group(3))));
        }

        List<Candidate> rankedCandidates = candidates.stream()
            .sorted(Comparator.comparingInt(Candidate::activeOrderCount).thenComparing(Candidate::id))
            .toList();
        Candidate selected = rankedCandidates.stream()
                .min(Comparator.comparingInt(Candidate::activeOrderCount).thenComparing(Candidate::id))
                .orElseGet(() -> {
                    Matcher matcher = AGENT_ID.matcher(roster);
                    return new Candidate(matcher.find() ? matcher.group() : "AGT-002", 0);
                });
        int highestLoad = candidates.stream()
                .mapToInt(Candidate::activeOrderCount)
                .max()
                .orElse(selected.activeOrderCount());
        int runnerUpLoad = rankedCandidates.size() > 1
            ? rankedCandidates.get(1).activeOrderCount()
            : selected.activeOrderCount();
        int loadMargin = runnerUpLoad - selected.activeOrderCount();
        double loadQuality = 1.0 - ((double) selected.activeOrderCount() / (highestLoad + 1));
        double separation = (double) loadMargin / (highestLoad + 1);
        double confidence = 0.65 + (0.10 * loadQuality) + (0.25 * separation);
        return "{\"agentId\":\"" + selected.id()
                + "\",\"confidence\":" + String.format(java.util.Locale.ROOT, "%.2f", confidence)
                + ",\"reasoning\":\"Stub advisor selected " + selected.id()
            + " because it has the lowest active load of " + selected.activeOrderCount()
            + " with a load margin of " + loadMargin + " over the next candidate"
                + ".\"}";
    }
}
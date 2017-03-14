package com.android.inputmethod.keyboard.emojifast;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by sepehr on 2/2/17.
 */
public class EmojiParser {

    private final EmojiTree emojiTree;

    public EmojiParser(EmojiTree emojiTree) {
        this.emojiTree = emojiTree;
    }

    public @NonNull
    List<Candidate> findCandidates(@Nullable CharSequence text) {
        List<Candidate> results = new LinkedList<>();

        if (text == null) return results;

        for (int i = 0; i < text.length(); i++) {
            int emojiEnd = getEmojiEndPos(text, i);

            if (emojiEnd != -1) {
                EmojiDrawInfo drawInfo = emojiTree.getEmoji(text, i, emojiEnd);

                if (emojiEnd + 2 <= text.length()) {
                    if (Fitzpatrick.fitzpatrickFromUnicode(text, emojiEnd) != null) {
                        emojiEnd += 2;
                    }
                }

                results.add(new Candidate(i, emojiEnd, drawInfo));

                i = emojiEnd - 1;
            }
        }

        return results;
    }

    private int getEmojiEndPos(CharSequence text, int startPos) {
        int best = -1;

        for (int j = startPos + 1; j <= text.length(); j++) {
            EmojiTree.Matches status = emojiTree.isEmoji(text, startPos, j);

            if (status.exactMatch()) {
                best = j;
            } else if (status.impossibleMatch()) {
                return best;
            }
        }

        return best;
    }

    public static class Candidate {

        private final int           startIndex;
        private final int           endIndex;
        private final EmojiDrawInfo drawInfo;

        Candidate(int startIndex, int endIndex, EmojiDrawInfo drawInfo) {
            this.startIndex = startIndex;
            this.endIndex = endIndex;
            this.drawInfo = drawInfo;
        }

        public EmojiDrawInfo getDrawInfo() {
            return drawInfo;
        }

        public int getEndIndex() {
            return endIndex;
        }

        public int getStartIndex() {
            return startIndex;
        }
    }

}